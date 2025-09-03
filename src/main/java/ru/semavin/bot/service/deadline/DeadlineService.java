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
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final UserService userService;
    private final MessageSenderService messageSenderService;
    private final DeadLineApiService deadlineApiService;

    /**
     * Храним «следы» уже отправленных уведомлений, чтобы не спамить.
     * Ключи:
     * uuid:username                       — уведомление о создании
     * uuid:reminder:3|1:username         — напоминания за 3 дня и 1 день
     */
    private final Set<String> notifiedUsers = new ConcurrentSkipListSet<>();

    /* --------------------------------------------------------------------------
     *  Создание дедлайна
     * ----------------------------------------------------------------------- */
    @CacheEvict(value = "deadlineMessages", allEntries = true)
    public CompletableFuture<Void> addDeadline(String senderUsername,
                                               DeadlineDTO dto,
                                               Map<String, Long> recipientChats) {
        return userService.getUserForTelegramTag(senderUsername)
                .thenCompose(user -> {
                    if (!"starosta".equalsIgnoreCase(user.getRole())) {
                        log.warn("{} не староста — отказ в создании дедлайна", senderUsername);
                        return CompletableFuture.failedFuture(
                                new BadRequestException("Недостаточно прав для создания дедлайна"));
                    }

                    dto.setUuid(UUID.randomUUID());
                    dto.setUsername(senderUsername);
                    log.info("Создаём дедлайн {}", dto);

                    return deadlineApiService.save(dto)
                            .thenRun(() -> notifyOnCreation(dto, recipientChats));
                });
    }

    /* --------------------------------------------------------------------------
     *  Сообщения списка дедлайнов
     * ----------------------------------------------------------------------- */
    @Cacheable(value = "deadlineMessages", key = "#groupName + ':' + #isStarosta")
    public List<SendMessage> buildDeadlineMessages(String groupName, Long chatId, boolean isStarosta) {
        try {
            List<DeadlineDTO> deadlines = deadlineApiService.getAllByGroup(groupName).join();
            return DeadLineMessageUtil.buildDeadlineMessagesWithButtons(groupName, chatId, deadlines, isStarosta);
        } catch (Exception ex) {
            log.error("Не смог построить сообщения дедлайнов: {}", ex.getMessage());
            return List.of(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("⚠ Не удалось получить список дедлайнов. Попробуйте позже.")
                    .build());
        }
    }

    /* --------------------------------------------------------------------------
     *  Удаление
     * ----------------------------------------------------------------------- */
    @CacheEvict(value = "deadlineMessages", allEntries = true)
    public boolean deleteDeadline(UUID id) {
        try {
            deadlineApiService.delete(id).join();
            // очищаем следы об этом дедлайне
            notifiedUsers.removeIf(k -> k.startsWith(id.toString()));
            return true;
        } catch (Exception ex) {
            log.error("Ошибка удаления дедлайна {}: {}", id, ex.getMessage());
            return false;
        }
    }

    /* --------------------------------------------------------------------------
     *  CRON: напоминания
     * ----------------------------------------------------------------------- */
    @Scheduled(cron = "${deadline.cron}")   // 09:00 каждый день
    @CacheEvict(value = "deadlineMessages", allEntries = true)
    public void checkAndSendReminders() {
        LocalDate today = LocalDate.now();
        LocalDate from = today;
        LocalDate to = today.plusDays(3);

        deadlineApiService.getDeadlinesBetween(from, to).thenAccept(deadlines -> {
            for (DeadlineDTO d : deadlines) {
                long daysLeft = today.until(d.getDueDate()).getDays();

                if (daysLeft == 3 && !d.isNotified3Days()) {
                    notifyReminder(d, 3)
                            .thenCompose(v -> deadlineApiService.markNotified(d.getUuid(), true, d.isNotified1Day()))
                            .exceptionally(ex -> {
                                log.error("Не смог проставить флаг 3 дней", ex);
                                return null;
                            })
                            .join();
                }
                if (daysLeft == 1 && !d.isNotified1Day()) {
                    notifyReminder(d, 1)
                            .thenCompose(v -> deadlineApiService.markNotified(d.getUuid(), d.isNotified3Days(), true))
                            .exceptionally(ex -> {
                                log.error("Не смог проставить флаг 1 дня", ex);
                                return null;
                            })
                            .join();
                }
            }

            // Очищаем «старые» напоминания (дедлайн уже прошёл)
            notifiedUsers.removeIf(key -> {
                String[] p = key.split(":");
                if (p.length == 0) return false;
                try {
                    UUID uuid = UUID.fromString(p[0]);
                    return deadlines.stream()
                            .filter(dl -> dl.getUuid().equals(uuid))
                            .anyMatch(dl -> dl.getDueDate().isBefore(today.minusDays(1)));
                } catch (IllegalArgumentException e) {
                    return false;
                }
            });
        });
    }

    /* --------------------------------------------------------------------------
     *  Отправка уведомлений о создании
     * ----------------------------------------------------------------------- */
    @Async
    public void notifyOnCreation(DeadlineDTO dto, Map<String, Long> recipientChats) {
        dto.getReceivers().stream().distinct().forEach(username -> {
            String key = dto.getUuid() + ":" + username;
            if (!notifiedUsers.add(key)) return;   // уже отправляли

            Long chatId = recipientChats.get(username);
            if (chatId == null) {
                log.warn("chatId не найден для {}", username);
                return;
            }

            String msg = String.format("""
                            \uD83D\uDCCC *Новый дедлайн!*\n\n*%s*\n\n\uD83D\uDCDD _%s_\n\uD83D\uDCC5 До: *%s*""",
                    dto.getTitle(), dto.getDescription(), dto.getDueDate());
            messageSenderService.sendTextWithMarkdown(chatId, msg);
        });
    }

    /* --------------------------------------------------------------------------
     *  Напоминания
     * ----------------------------------------------------------------------- */
    private CompletableFuture<Void> notifyReminder(DeadlineDTO dto, int daysLeft) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (String username : dto.getReceivers()) {
            String key = dto.getUuid() + ":reminder:" + daysLeft + ":" + username;
            if (!notifiedUsers.add(key)) continue;   // уже отправляли

            tasks.add(userService.getUserForTelegramTag(username)
                    .thenAccept(user -> {
                        Long chatId = user.getTelegramId();
                        if (chatId == null) {
                            log.warn("chatId не найден для {}", username);
                            return;
                        }

                        String message = String.format("""
                                        \uD83D\uDD14 *Напоминание о дедлайне!*\n\n*%s* — осталось *%d %s*\n\n\uD83D\uDCDD _%s_\n\uD83D\uDCC5 До: *%s*""",
                                dto.getTitle(), daysLeft,
                                daysLeft == 1 ? "день" : (daysLeft <= 4 ? "дня" : "дней"),
                                dto.getDescription(), dto.getDueDate());
                        sendReminderAsync(chatId, message);
                    }));
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    @Async
    public void sendReminderAsync(Long chatId, String message) {
        messageSenderService.sendTextWithMarkdown(chatId, message);
    }
}
