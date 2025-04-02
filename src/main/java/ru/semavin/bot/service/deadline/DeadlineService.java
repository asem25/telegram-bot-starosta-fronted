package ru.semavin.bot.service.deadline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.DeadlineDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineService {

    private final UserService userService;
    private final MessageSenderService messageSenderService;

    // TODO: заменить на репозиторий при интеграции с БД
    private final Map<String, List<DeadlineDTO>> deadlinesByGroup = new ConcurrentHashMap<>();

    /**
     * Добавляет новый дедлайн от имени пользователя. Проверяет, что отправитель — староста.
     * Передаётся карта username → chatId для рассылки уведомлений.
     */
    public void addDeadline(String senderUsername, DeadlineDTO dto, Map<String, Long> recipientChats) {
        userService.getUserForTelegramTag(senderUsername)
                .thenAccept(user -> {
                    if (!"starosta".equalsIgnoreCase(user.getRole())) {
                        log.warn("Пользователь {} не является старостой", senderUsername);
                        return;
                    }
                    dto.setId(UUID.randomUUID());

                    deadlinesByGroup.computeIfAbsent(dto.getGroupName(), k -> new ArrayList<>()).add(dto);
                    notifyOnCreation(dto, recipientChats);
                })
                .exceptionally(e -> {
                    log.error("Ошибка при добавлении дедлайна", e);
                    return null;
                });
    }

    /**
     * Получает все дедлайны, где пользователь является получателем.
     */
    public List<DeadlineDTO> getDeadlinesForUser(String username) {
        return deadlinesByGroup.values().stream()
                .flatMap(Collection::stream)
                .filter(deadline -> deadline.getRecipients().contains(username))
                .collect(Collectors.toList());
    }

    /**
     * Проверяет дедлайны и отправляет напоминания за 3 дня и за 1 день.
     * Запуск планируется раз в сутки.
     */
    @Scheduled(cron = "0 0 9 * * *") // каждый день в 9:00 утра
    public void checkAndSendReminders() {
        LocalDate today = LocalDate.now();

        for (List<DeadlineDTO> groupDeadlines : deadlinesByGroup.values()) {
            for (DeadlineDTO deadline : groupDeadlines) {
                long daysLeft = today.until(deadline.getDueDate()).getDays();

                if (daysLeft == 3 && !deadline.isNotified3Days()) {
                    notifyReminder(deadline, 3);
                    deadline.setNotified3Days(true);
                } else if (daysLeft == 1 && !deadline.isNotified1Day()) {
                    notifyReminder(deadline, 1);
                    deadline.setNotified1Day(true);
                }
            }
        }
    }

    private void notifyOnCreation(DeadlineDTO dto, Map<String, Long> recipientChats) {
        for (String username : dto.getRecipients()) {
            Long chatId = recipientChats.get(username);
            if (chatId != null) {
                messageSenderService.sendTextMessage(
                        chatId,
                        "\uD83D\uDCCC Назначен новый дедлайн: " + dto.getTitle() + "\n"
                                + dto.getDescription() + "\nСрок: " + dto.getDueDate()
                );
            } else {
                log.warn("Не удалось отправить уведомление: chatId не найден для {}", username);
            }
        }
    }

    private void notifyReminder(DeadlineDTO dto, int daysLeft) {
        for (String username : dto.getRecipients()) {
            // Здесь можно использовать кеш, если будет доступен
            log.info("(Напоминание) {}: {} дн. до дедлайна", username, daysLeft);
            // TODO: подключить chatId при интеграции с хранением/сессиями
        }
    }

    // TODO: методы для сохранения/загрузки дедлайнов из базы (будущая интеграция)
}
