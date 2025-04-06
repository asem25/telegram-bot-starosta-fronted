package ru.semavin.bot.service.notification;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.dto.SkipNotificationDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
@Slf4j
public class SkipNotificationService {
    private final MessageSenderService messageSenderService;
    private final GroupService groupService;
    private final NotificationApiService notificationApiService;

    private final Map<String, List<SkipNotificationDTO>> notificationsByGroup = new ConcurrentHashMap<>();
    @Data

    public static class MissedResponse {
        private String text;
        private InlineKeyboardMarkup markup;
    }
    public void notifyStarosta(SkipNotificationDTO dto) {
        String group = dto.getGroupName();
        String link = "https://t.me/" + dto.getUsername();
        String decription = dto.getDescription();


        String period = dto.getFromDate().equals(dto.getToDate())
                ? dto.getFromDate().toString()
                : dto.getFromDate() + " — " + dto.getToDate();

        String message = String.format("""
                ⚠️ Студент [%s](%s) предупредил об отсутствии:
                Что именно пропущу?: %s
                📅 %s
                """, dto.getUsername(), link, decription,period);

        groupService.getStarosta(group).thenAccept(optStarosta -> {
            if (optStarosta.isPresent()) {
                UserDTO starosta = optStarosta.get();
                Long chatId = starosta.getTelegramId();
                if (chatId != null) {
                    messageSenderService.sendMessageWithMarkDown(chatId, message);
                } else {
                    log.warn("Не удалось отправить уведомление: chatId старосты не найден");
                }
            } else {
                log.warn("В группе {} нет старосты, уведомление не отправлено", group);
            }
        });
        //Отправляем на сервис
        dto.setUuid(UUID.randomUUID());
        notificationApiService.sendSkipNotification(dto);
    }
    @Async
    public CompletableFuture<Void> deleteSkip(String uuid) {
        return
                notificationApiService.deleteSkipNotification(UUID.fromString(uuid));
    }

    @Async
    public CompletableFuture<String> formatGroupAbsences(String groupName, List<SkipNotificationDTO> absences) {
        if (absences == null || absences.isEmpty()) {
            return CompletableFuture.completedFuture("✉️ В группе *" + groupName + "* нет активных уведомлений о пропусках.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCC5 Актуальные пропуски в группе *").append(groupName).append("*:\n\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (SkipNotificationDTO dto : absences) {
            String from = dto.getFromDate().format(formatter);
            String to = dto.getToDate().format(formatter);
            String range = from.equals(to) ? from : from + " - " + to;
            String link = dto.getUsername() != null ? "https://t.me/" + dto.getUsername() : "[неизвестен]";

            sb.append("\u2022 [@")
                    .append(dto.getUsername())
                    .append("](").append(link).append(") — ")
                    .append(range);

            if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
                sb.append(" — _").append(dto.getDescription().strip()).append("_");
            }

            sb.append("\n");
        }

        return CompletableFuture.completedFuture(sb.toString());
    }

    public CompletableFuture<MissedResponse> formatGroupAbsencesWithKeyboard(String groupName) {
        // 1) Сначала получаем список пропусков во внешнем API
        return notificationApiService.getAllNotification(groupName) // CF<List<SkipNotificationDTO>>
                // 2) Когда список готов – вызываем formatGroupAbsences
                .thenCompose(skipList -> {
                    // formatGroupAbsences(...) вернёт CF<String>
                    CompletableFuture<String> textFuture = formatGroupAbsences(groupName, skipList);

                    // объединяем:
                    return textFuture.thenApply(text -> {
                        // формируем MissedResponse
                        MissedResponse response = new MissedResponse();
                        response.setText(text);

                        // Инлайн-кнопки строим из skipList
                        InlineKeyboardMarkup markup = KeyboardUtils.buildMissedInlineKeyboard(skipList);
                        response.setMarkup(markup);
                        return response;
                    });
                })
                // обработка ошибок
                .exceptionally(ex -> {
                    log.error("Ошибка в цепочке при получении пропусков или формировании текста: {}", ex.getMessage());
                    // Возвращаем MissedResponse с текстом об ошибке
                    MissedResponse fallback = new MissedResponse();
                    fallback.setText("Ошибка при получении пропусков");
                    fallback.setMarkup(null);
                    return fallback;
                });
    }
}
