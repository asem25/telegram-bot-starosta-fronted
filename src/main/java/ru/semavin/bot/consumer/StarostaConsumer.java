package ru.semavin.bot.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.botcommands.handlers.*;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.deadline.DeadLineCreationService;
import ru.semavin.bot.service.deadline.DeadlineService;
import ru.semavin.bot.service.schedules.ScheduleChangeEditingContextService;
import ru.semavin.bot.service.users.profile.ProfileEditingService;
import ru.semavin.bot.service.users.register.RegistrationStateService;
import ru.semavin.bot.service.users.register.UserRegistrationService;
import ru.semavin.bot.service.users.starosta.StarostaChangeServiceState;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.AbsenceCalendarContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private final ScheduleChangeCallBackHandler scheduleChangeCallBackHandler;
    private final ScheduleChangeEditingContextService scheduleChangeEditingContextService;
    private final StarostaChangeServiceState state;

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
        if (scheduleChangeEditingContextService.getEditingField(chatId).isPresent()) {
            scheduleChangeEditingContextService.processEditingInput(chatId, text).ifPresent(dto -> {
                String updatedText = scheduleChangeEditingContextService.buildScheduleChangeText(dto);
                InlineKeyboardMarkup editMarkup = KeyboardUtils.createScheduleChangeEditMarkup();
                // Вместо редактирования или удаления старого сообщения – отправляем новое
                messageSenderService.sendButtonMessage(
                        SendMessage.builder()
                                .chatId(chatId.toString())
                                .text(updatedText)
                                .replyMarkup(editMarkup)
                                .build()).thenAccept(response -> {
                    // Можно логировать, что обновление отправлено,
                    // либо сохранить идентификатор нового сообщения, если потребуется дальнейшая логика.
                    log.info("Отправлено новое сообщение с обновлёнными данными");
                }).exceptionally(ex -> {
                    log.error("Ошибка при отправке нового сообщения: {}", ex.getMessage());
                    return null;
                });
            });
            state.clearState(chatId);
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
        if (data.startsWith("LESSON_SELECT_")
                || data.startsWith("CALENDAR_CHANGE_")
                || data.startsWith("SCHEDULE_CHANGE_EDIT")
                || data.startsWith("SCHEDULE_CHANGE_DELETE")
                || data.startsWith("UPDATE_SCHEDULE_CHANGE_FIELD_")
                || data.startsWith("SCHEDULE_CHANGE_CONFIRM")
                || data.startsWith("SCHEDULE_CHANGE_CANCEL_LESSON")){
            scheduleChangeCallBackHandler.handleScheduleChangeCallback(callbackQuery);
            return;
        }
        if (data.startsWith("DELETE_DEADLINE_")) {
            UUID id = UUID.fromString(data.replace("DELETE_DEADLINE_", ""));
            boolean deleted = deadlineService.deleteDeadline(id);
            String text = deleted ? "✅ Дедлайн удалён." : "⚠️ Не удалось найти дедлайн.";
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(chatId.toString(), messageId, text, null)
            );
        }


    }
}
