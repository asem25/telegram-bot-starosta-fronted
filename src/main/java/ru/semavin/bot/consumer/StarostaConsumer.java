package ru.semavin.bot.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.botcommands.BotCommandHandler;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.*;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.UserApiService;
import ru.semavin.bot.service.users.profile.ProfileEditingService;
import ru.semavin.bot.service.users.profile.ProfileService;
import ru.semavin.bot.service.users.register.RegistrationStateService;
import ru.semavin.bot.service.users.register.UserRegistrationService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.CalendarUtils;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StarostaConsumer implements LongPollingUpdateConsumer {

    private final RegistrationStateService stateService;
    private final MessageSenderService messageSenderService;
    private final UserService userService;
    private final UserRegistrationService userRegistrationService;
    private final ProfileEditingService profileEditingService;
    private final ProfileService profileService;
    private final ScheduleService scheduleService;
    private final ExecutorService executorService;
    private final GroupService groupService;
    private final UserApiService userApiService;
    private final BotCommandHandler botCommand;
    @Override
    public void consume(List<Update> updates) {
        List<Update> stepUpdates = new ArrayList<>();
        List<Update> normalUpdates = new ArrayList<>();

        for (Update update : updates) {
            Long chatId = getChatId(update);
            if (inRegistrationOrEdit(chatId)) {
                stepUpdates.add(update);
            } else {
                normalUpdates.add(update);
            }
        }

        stepUpdates.forEach(this::processSingleUpdate);

        normalUpdates.forEach(update ->
                executorService.submit(() -> processSingleUpdate(update))
        );
    }

    private boolean inRegistrationOrEdit(Long chatId) {
        return stateService.getStep(chatId) != RegistrationStep.NONE || profileEditingService.isEditing(chatId);
    }

    private Long getChatId(Update update) {
        if (update.hasMessage() && update.getMessage() != null) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return -1L;
    }

    private void processSingleUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        org.telegram.telegrambots.meta.api.objects.User from = message.getFrom();

        log.info("Message received: {}", text);

        // 1) Если пользователь в процессе регистрации — сначала завершим регистрацию
        if (stateService.getStep(chatId) != RegistrationStep.NONE) {
            userRegistrationService.processStep(chatId, text);
            return;
        }

        // 2) Если пользователь редактирует профиль
        if (profileEditingService.isEditing(chatId)) {
            profileEditingService.processEditStep(chatId, text);
            return;
        }
        botCommand.handle(message);
    }
    private void handleCallback(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage maybeMsg = callbackQuery.getMessage();
        String data = callbackQuery.getData();
        log.info("Нажата кнопка с данными: {}", data);

        if (maybeMsg == null) {
            log.warn("Нет привязки к чату, обработка невозможна.");
            return;
        }

        Long chatId = maybeMsg.getChat().getId();
        Integer messageId = maybeMsg.getMessageId();
        String username = callbackQuery.getFrom().getUserName();

        if ("REG_START".equals(data)) {
            userRegistrationService.startRegistration(chatId, callbackQuery.getFrom().getId(), callbackQuery.getFrom());
            return;
        }

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
                        log.error("Ошибка обработки CALENDAR_DATE_: {}", e.getMessage());
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания.");
                        return null;
                    });

            return;
        }

        if (data.startsWith("CALENDAR_NAV_")) {
            String[] parts = data.split("_");
            int year = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[3]);


            InlineKeyboardMarkup calendar = CalendarUtils.buildCalendarKeyboard(year, month);
            messageSenderService.editCalendarMarkup(chatId, messageId, calendar)
                    .exceptionally(e -> {
                        log.error("Ошибка при навигации календаря: {}", e.getMessage());
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
                                            KeyboardUtils.createMarkupWithTomorrow(today)
                                    )
                            )
                    ).exceptionally(e -> {
                        log.error("Ошибка при возврате к сегодняшнему дню: {}", e.getMessage());
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на сегодня.");
                        return null;
                    });
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
        if (data.startsWith("MONTH_")) {
            YearMonth month = YearMonth.parse(data.replace("MONTH_", ""));
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите неделю месяца:",
                            CalendarUtils.buildWeeksMarkupForMonth(month)
                    )
            );
            return;
        }

        if (data.equals("BACK_MONTHS")) {
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            "📅 Выберите месяц:",
                            CalendarUtils.buildMonthsMarkup()
                    )
            );
            return;
        }
        if (data.startsWith("WEEK_")){
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
        }
    }




    private List<LocalDate> getWeekDates(LocalDate startDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate monday = startDate.with(DayOfWeek.MONDAY);
        for (int i = 0; i < 6; i++) {
            dates.add(monday.plusDays(i));
        }
        return dates;
    }
}
