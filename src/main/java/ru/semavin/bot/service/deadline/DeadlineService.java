package ru.semavin.bot.service.deadline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.semavin.bot.dto.DeadlineDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.exceptions.BadRequestException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final UserService userService;
    private final MessageSenderService messageSenderService;
    private final DeadLineApiService deadlineApiService;

    @CacheEvict(value = "deadlineMessages", allEntries = true)
    public CompletableFuture<Void> addDeadline(String senderUsername, DeadlineDTO dto, Map<String, Long> recipientChats) {
        return userService.getUserForTelegramTag(senderUsername)
                .thenCompose(user -> {
                    if (!"starosta".equalsIgnoreCase(user.getRole())) {
                        log.warn("Пользователь {} не является старостой", senderUsername);
                        return CompletableFuture.failedFuture(new BadRequestException("Недостаточно прав для создания дедлайна"));
                    }

                    dto.setUuid(UUID.randomUUID());
                    log.info("dto перед отправкой: {}", dto);

                    return deadlineApiService.save(dto)
                            .thenRun(() -> notifyOnCreation(dto, recipientChats));
                })
                .exceptionally(e -> {
                    log.error("Ошибка при добавлении дедлайна: {}", e.getMessage());
                    throw new BadRequestException("Ошибка при создании дедлайна");
                });
    }

    public CompletableFuture<List<DeadlineDTO>> getDeadlinesForGroupName(String groupName) {
        return deadlineApiService.getAllByGroup(groupName);
    }

    @Cacheable(value = "deadlineMessages", key = "#groupName + ':' + #isStarosta")
    public List<SendMessage> buildDeadlineMessages(String groupName, Long chatId, boolean isStarosta) {
        try {
            List<DeadlineDTO> deadlines = deadlineApiService.getAllByGroup(groupName).join();
            return DeadLineMessageUtil.buildDeadlineMessagesWithButtons(groupName, chatId, deadlines, isStarosta);
        } catch (Exception ex) {
            log.error("Ошибка при построении сообщений дедлайнов: {}", ex.getMessage());
            return List.of(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("\u26A0\uFE0F Не удалось получить список дедлайнов. Попробуйте позже.")
                    .build());
        }
    }

    public Optional<DeadlineDTO> getDeadlineById(UUID id) {
        return Optional.empty();
    }

    @CacheEvict(value = "deadlineMessages", allEntries = true)
    public boolean deleteDeadline(UUID id) {
        try {
            deadlineApiService.delete(id).join();
            return true;
        } catch (Exception ex) {
            log.error("Ошибка при удалении дедлайна через API: {}", ex.getMessage());
            return false;
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @CacheEvict(value = "deadlineMessages", allEntries = true)
    public void checkAndSendReminders() {
        LocalDate today = LocalDate.now();
        LocalDate from = today;
        LocalDate to = today.plusDays(3);

        deadlineApiService.getDeadlinesBetween(from, to).thenAccept(deadlines -> {
            for (DeadlineDTO deadline : deadlines) {
                long daysLeft = today.until(deadline.getDueDate()).getDays();

                if (daysLeft == 3 && !deadline.isNotified3Days()) {
                    notifyReminder(deadline, 3);
                    deadlineApiService.markNotified(deadline.getUuid(), true, deadline.isNotified1Day());
                }
                if (daysLeft == 1 && !deadline.isNotified1Day()) {
                    notifyReminder(deadline, 1);
                    deadlineApiService.markNotified(deadline.getUuid(), deadline.isNotified3Days(), true);
                }
            }
        }).exceptionally(ex -> {
            log.error("Ошибка при проверке дедлайнов для напоминаний: {}", ex.getMessage());
            return null;
        });
    }

    @Async
    public void notifyOnCreation(DeadlineDTO dto, Map<String, Long> recipientChats) {
        for (String username : dto.getReceivers()) {
            Long chatId = recipientChats.get(username);
            if (chatId != null) {
                String message = String.format("""
                    📌 *Новый дедлайн назначен!*

                    *%s*

                    📝 _%s_
                    📅 До: *%s*
                    """,
                        dto.getTitle(),
                        dto.getDescription(),
                        dto.getDueDate()
                );

                messageSenderService.sendTextWithMarkdown(chatId, message);
            } else {
                log.warn("Не удалось отправить уведомление: chatId не найден для {}", username);
            }
        }
    }



    private void notifyReminder(DeadlineDTO dto, int daysLeft) {
        for (String username : dto.getReceivers()) {
            userService.getUserForTelegramTag(username)
                    .thenAccept(user -> {
                        Long chatId = user.getTelegramId();
                        if (chatId != null) {
                            String message = String.format("""
                                \uD83D\uDD14 *Напоминание о дедлайне!*

                                *%s* — осталось *%d %s*!

                                \uD83D\uDCDD _%s_
                                \uD83D\uDCC5 До: *%s*
                                """,
                                    dto.getTitle(),
                                    daysLeft, daysLeft == 1 ? "день" : (daysLeft <= 4 ? "дня" : "дней"),
                                    dto.getDescription(),
                                    dto.getDueDate()
                            );

                            sendReminderAsync(chatId, message);
                        } else {
                            log.warn("Не удалось отправить напоминание: chatId не найден для {}", username);
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Ошибка при отправке напоминания для {}: {}", username, ex.getMessage());
                        return null;
                    });
        }
    }

    @Async
    public void sendReminderAsync(Long chatId, String message) {
        messageSenderService.sendTextWithMarkdown(chatId, message);
    }
}