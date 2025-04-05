package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarCallBackHandler {

    private final MessageSenderService messageSenderService;
    private final UserService userService;
    private final ScheduleService scheduleService;

    public void handleCalendarCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String username = callbackQuery.getFrom().getUserName();

        // Навигация по календарю
        if (data.startsWith("NAV_CALENDAR_")) {
            String[] parts = data.split("_"); // NAV_CALENDAR_YYYY_MM
            int year = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[3]);

            InlineKeyboardMarkup calendar = CalendarUtils.buildCalendarKeyboard(year, month);
            messageSenderService.editCalendarMarkup(chatId, messageId, calendar)
                    .exceptionally(e -> {
                        log.error("Ошибка при навигации calendar: {}", e.getMessage());
                        return null;
                    });
            return;
        }

        // Выбор даты для расписания
        if (data.startsWith("CALENDAR_DATE_")) {
            LocalDate selectedDate = CalendarUtils.parseDateFromCallback(data);
            if (selectedDate == null) {
                messageSenderService.sendTextMessage(chatId, "Не удалось разобрать дату.");
                return;
            }
            CalendarUtils.rememberMonthForUser(callbackQuery.getFrom().getId(), YearMonth.from(selectedDate));

            userService.getUserForTelegramTag(username)
                    .thenCompose(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), selectedDate))
                    .thenCompose(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            chatId.toString(),
                                            messageId,
                                            scheduleText,
                                            KeyboardUtils.createMarkupWithBackToCalendarButton()
                                    )
                            )
                    )
                    .exceptionally(e -> {
                        log.error("Ошибка при получении расписания: {}", e.getMessage());
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания.");
                        return null;
                    });

            return;
        }
        if ("CALENDAR_BACK".equals(data)) {
            LocalDate now = LocalDate.now();
            SendMessage calendarMessage = KeyboardUtils.createMessageWithInlineCalendar(chatId, now.getYear(),
                    CalendarUtils.getUserMonthOrDefault(callbackQuery.getFrom().getId()).getMonthValue());


            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            calendarMessage.getText(),
                            (InlineKeyboardMarkup) calendarMessage.getReplyMarkup()
                    )
            ).exceptionally(e -> {
                log.error("Ошибка при возврате к календарю: {}", e.getMessage());
                return null;
            });

            return;
        }

    }
}