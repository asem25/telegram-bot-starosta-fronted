package ru.semavin.bot.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.handlers.*;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.*;
import ru.semavin.bot.service.deadline.DeadLineCreationService;
import ru.semavin.bot.service.deadline.DeadlineService;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.profile.ProfileEditingService;
import ru.semavin.bot.service.users.register.RegistrationStateService;
import ru.semavin.bot.service.users.register.UserRegistrationService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.calendar.AbsenceCalendarContextService;
import ru.semavin.bot.util.calendar.CalendarUtils;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StarostaConsumer implements LongPollingUpdateConsumer {

    private final RegistrationStateService stateService;
    private final MessageSenderService messageSenderService;
    private final UserRegistrationService userRegistrationService;
    private final ProfileEditingService profileEditingService;
    private final ExecutorService executorService;
    private final BotCommandHandler botCommand;
    private final DeadLineCreationService deadLineCreationService;
    private final DeadlineCreationStepHandler deadlineCreationStepHandler;
    private final DeadlineService deadlineService;
    private final AbsenceCalendarContextService absenceCalendarContextService;
    private final AbsenceCallbackHandler absenceCallbackHandler;
    private final CalendarCallBackHandler calendarCallBackHandler;
    private final ScheduleCallbackHandler scheduleCallbackHandler;
    private final SkipCallBackHandler skipCallBackHandler;

    @Override
    public void consume(List<Update> updates) {
        List<Update> stepUpdates = new ArrayList<>();
        List<Update> normalUpdates = new ArrayList<>();

        for (Update update : updates) {
            Long chatId = getChatId(update);
            if (isStep(chatId) ) {
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

    private boolean isStep(Long chatId) {
        return stateService.getStep(chatId) != RegistrationStep.NONE
                || profileEditingService.isEditing(chatId)
                || deadLineCreationService.getStep(chatId) != DeadLineCreationService.Step.COMPLETE
                || absenceCalendarContextService.isAwaitingDescription(chatId);
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
        if (absenceCalendarContextService.isAwaitingDescription(chatId)){
            absenceCalendarContextService.saveDescription(chatId, text);
            return;
        }
        // 2) Если пользователь редактирует профиль
        if (profileEditingService.isEditing(chatId)) {
            profileEditingService.processEditStep(chatId, text);
            return;
        }
        if (deadLineCreationService.getStep(chatId) != DeadLineCreationService.Step.COMPLETE) {
            deadlineCreationStepHandler.handleStep(message);
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

        if ("REG_START".equals(data)) {
            userRegistrationService.startRegistration(chatId, callbackQuery.getFrom().getId(), callbackQuery.getFrom());
            return;
        }


        if (data.startsWith("ABSENCE_DATE_")
                || data.startsWith("NAV_ABSENCE_")
                || data.equals("ABSENCE_CONFIRM")
                || data.equals("ABSENCE_CANCEL")) {
            absenceCallbackHandler.handleAbsenceCallback(callbackQuery);
            return;
        }
        if (data.startsWith("CALENDAR_DATE_")
                || data.startsWith("NAV_CALENDAR_")
                || data.startsWith("CALENDAR_BACK")) {
            calendarCallBackHandler.handleCalendarCallback(callbackQuery);
            return;
        }
        if (data.startsWith("WEEK_")
                || data.startsWith("BACK_WEEK_")
                || data.startsWith("SHOW_DAY_")
                || data.startsWith("MONTH_")
                || data.equals("BACK_MONTHS")
                || data.startsWith("TOMORROW_")
                || data.startsWith("BACK_TO_TODAY_")
                || data.startsWith("BACK_WEEKS")) {
            scheduleCallbackHandler.handleScheduleCallback(callbackQuery);
            return;
        }
        if (data.startsWith("DELETE_MISSED_")){
            skipCallBackHandler.handleSkipCallback(callbackQuery);
            return;
        }
        //TODO ScheduleChangeHandler
        if (data.startsWith("DELETE_DEADLINE_")) {
            UUID id = UUID.fromString(data.replace("DELETE_DEADLINE_", ""));
            boolean deleted = deadlineService.deleteDeadline(id);
            String text = deleted ? "✅ Дедлайн удалён." : "⚠️ Не удалось найти дедлайн.";
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(chatId.toString(), messageId, text, null)
            );
            return;
        }


    }
}
