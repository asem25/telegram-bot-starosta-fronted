package ru.semavin.bot.service.notification;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.dto.SkipNotificationDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        // Добавляем в список активных уведомлений
        notificationsByGroup.computeIfAbsent(group, g -> new ArrayList<>()).add(dto);
    }
    public boolean deleteSkip(String groupName, String username, LocalDate fromDate, LocalDate toDate) {
        //TODO Переделать для api

        // Получаем список для группы
        List<SkipNotificationDTO> list = notificationsByGroup.getOrDefault(groupName, new ArrayList<>());
        // Ищем элемент
        Optional<SkipNotificationDTO> maybe = list.stream()
                .filter(n ->
                        n.getUsername().equals(username) &&
                                n.getFromDate().equals(fromDate) &&
                                n.getToDate().equals(toDate)
                )
                .findFirst();
        if (maybe.isPresent()) {
            list.remove(maybe.get());
            return true;
        }
        return false;
    }

    private List<SkipNotificationDTO> getActiveNotifications(String group) {
        //TODO Переделать для api
        return notificationsByGroup.getOrDefault(group, List.of()).stream()
                .filter(n -> !n.isExpired())
                .toList();
    }
    public String formatGroupAbsences(String groupName, List<SkipNotificationDTO> absences) {
        if (absences == null || absences.isEmpty()) {
            return "✉️ В группе *" + groupName + "* нет активных уведомлений о пропусках.";
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

        return sb.toString();
    }
    @Scheduled(cron = "0 0 6 * * *")
    public void cleanupExpired() {
        notificationsByGroup.forEach((group, list) ->
                list.removeIf(SkipNotificationDTO::isExpired)
        );
    }
    @Cacheable(cacheNames = "missedAbsences", key = "#groupName")
    public MissedResponse formatGroupAbsencesWithKeyboard(String groupName) {
        MissedResponse toResponse = new MissedResponse();

        List<SkipNotificationDTO> absences = getActiveNotifications(groupName);
        String text = formatGroupAbsences(groupName, absences);
        // Клавиатура
        InlineKeyboardMarkup markup = KeyboardUtils.buildMissedInlineKeyboard(absences);

        toResponse.setMarkup(markup);
        toResponse.setText(text);
        return toResponse;
    }
}
