package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleCallbackHandler {

    private final MessageSenderService messageSenderService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    public void handleScheduleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String username = callbackQuery.getFrom().getUserName();

        // --- 1) WEEK_ ---
        if (data.startsWith("WEEK_")) {
            int neededWeek = Integer.parseInt(data.substring("WEEK_".length()));

            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите день недели:",
                            CalendarUtils.buildWeekMessageWithBack(neededWeek)
                    )
            ).exceptionally(e -> {
                log.error("Ошибка при возврате к неделе: {}", e.getMessage());
                return null;
            });
            return;
        }

        // --- 2) BACK_WEEK_ ---
        if (data.startsWith("BACK_WEEK_")) {
            LocalDate weekStart = LocalDate.parse(data.replace("BACK_WEEK_", ""));
            int neededWeek = CalendarUtils.getRelativeWeekNumber(weekStart);

            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите день недели:",
                            CalendarUtils.buildWeekMessage(neededWeek)
                    )
            ).exceptionally(e -> {
                log.error("Ошибка при возврате к неделе: {}", e.getMessage());
                return null;
            });
            return;
        }

        // --- 3) SHOW_DAY_ ---
        if (data.startsWith("SHOW_DAY_")) {
            LocalDate date = LocalDate.parse(data.replace("SHOW_DAY_", ""));
            userService.getUserForTelegramTag(username)
                    .thenCompose(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), date))
                    .thenCompose(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            chatId.toString(),
                                            messageId,
                                            scheduleText,
                                            KeyboardUtils.createBackToWeekMarkup(date.with(DayOfWeek.MONDAY))
                                    )
                            )
                    ).exceptionally(e -> {
                        log.error("Ошибка при показе дня: {}", e.getMessage());
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на день.");
                        return null;
                    });
            return;
        }

        // --- 4) BACK_MONTHS ---
        if ("BACK_MONTHS".equals(data)) {
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите месяц:",
                            CalendarUtils.buildMonthsMarkup()
                    )
            ).exceptionally(e -> {
                log.error("Ошибка при BACK_MONTHS: {}", e.getMessage());
                return null;
            });
            return;
        }

        // --- 5) MONTH_yyyy-mm ---
        if (data.startsWith("MONTH_")) {
            YearMonth month = YearMonth.parse(data.replace("MONTH_", ""));
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите неделю месяца:",
                            CalendarUtils.buildWeeksMarkupForMonth(month)
                    )
            ).exceptionally(e -> {
                log.error("Ошибка при MONTH_: {}", e.getMessage());
                return null;
            });
            return;
        }

        // --- 6) TOMORROW_ ---
        if (data.startsWith("TOMORROW_")) {
            LocalDate tomorrow = LocalDate.parse(data.replace("TOMORROW_", ""));
            userService.getUserForTelegramTag(username)
                    .thenCompose(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), tomorrow))
                    .thenCompose(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            chatId.toString(),
                                            messageId,
                                            scheduleText,
                                            // кнопка "Назад к сегодняшнему дню"
                                            KeyboardUtils.createMarkupWithBackToToday(tomorrow.minusDays(1))
                                    )
                            )
                    ).exceptionally(e -> {
                        log.error("Ошибка при показе завтрашнего дня: {}", e.getMessage());
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на завтра.");
                        return null;
                    });
            return;
        }

        // --- 7) BACK_TO_TODAY_ ---
        if (data.startsWith("BACK_TO_TODAY_")) {
            LocalDate today = LocalDate.parse(data.replace("BACK_TO_TODAY_", ""));
            userService.getUserForTelegramTag(username)
                    .thenCompose(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), today))
                    .thenCompose(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            chatId.toString(),
                                            messageId,
                                            scheduleText,
                                            // кнопка "Завтра"
                                            KeyboardUtils.createMarkupWithTomorrow(today)
                                    )
                            )
                    ).exceptionally(e -> {
                        log.error("Ошибка при возврате к сегодняшнему дню: {}", e.getMessage());
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на сегодня.");
                        return null;
                    });
            return;
        }
        if (data.equals("BACK_WEEKS")) {
            YearMonth currentMonth = YearMonth.now();
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите неделю месяца:",
                            CalendarUtils.buildWeeksMarkupForMonth(currentMonth)
                    )
            );
            return;
        }
        // Если мы дошли сюда, значит коллбэк неизвестен
        log.warn("Неизвестный callback для schedule: {}", data);
    }
}
