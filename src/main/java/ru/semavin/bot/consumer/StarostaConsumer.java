package ru.semavin.bot.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.*;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.profile.ProfileEditingService;
import ru.semavin.bot.service.users.profile.ProfileService;
import ru.semavin.bot.service.users.register.RegistrationStateService;
import ru.semavin.bot.service.users.register.UserRegistrationService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.CalendarUtils;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.exceptions.UserNotFoundException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс-потребитель обновлений Telegram.
 * Отвечает за маршрутизацию входящих обновлений:
 * - При получении команды "/start" проверяет регистрацию пользователя и при необходимости инициирует процесс регистрации.
 * - При нажатии inline-кнопки "Регистрация" делегирует запуск регистрации в UserRegistrationService.
 * - Если пользователь находится в процессе регистрации, все введённые данные передаются в UserRegistrationService.
 */
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
    @Override
    public void consume(List<Update> updates) {
        // Два списка: шаговые и обычные
        List<Update> stepUpdates = new ArrayList<>();
        List<Update> normalUpdates = new ArrayList<>();

        // Разделяем апдейты
        for (Update update : updates) {
            Long chatId = getChatId(update);
            // Проверяем, участвует ли чат в пошаговом процессе (регистрация / редактирование)
            if (inRegistrationOrEdit(chatId)) {
                stepUpdates.add(update);
            } else {
                normalUpdates.add(update);
            }
        }

        // 1) «Шаговые» апдейты обрабатываем ПОСЛЕДОВАТЕЛЬНО (гарантирует порядок)
        for (Update u : stepUpdates) {
            processSingleUpdate(u);
        }

        // 2) «Обычные» апдейты обрабатываем ПАРАЛЛЕЛЬНО
        Flux.fromIterable(normalUpdates)
                .parallel()                          // Параллельный поток
                .runOn(Schedulers.parallel())
                .doOnNext(this::processSingleUpdate) // Вызов основного метода обработки
                .sequential()                        // Возвращаемся в единый поток
                .subscribe();
    }
    /**
     * Проверяем, не находится ли чат в процессе пошаговой логики:
     * - Если RegistrationStep != NONE, значит идёт регистрация
     * - Или если profileEditingService.isEditing(...) == true, значит редактирование
     */
    private boolean inRegistrationOrEdit(Long chatId) {
        // пример: используем existing сервисы
        if (stateService.getStep(chatId) != RegistrationStep.NONE) {
            return true;
        }
        return profileEditingService.isEditing(chatId);
    }

    /**
     * Извлекаем chatId (для разделения логики)
     */
    private Long getChatId(Update update) {
        if (update.hasMessage() && update.getMessage() != null) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        // Если вдруг нет ни Message, ни CallbackQuery, возвращаем что-то по умолчанию
        return -1L;
    }
    private void processSingleUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }
    /**
     * Обрабатывает входящее текстовое сообщение от пользователя.
     *
     * Если получена команда "/start", проверяется наличие пользователя в системе.
     * Если пользователь найден – отправляется приветственное сообщение,
     * если нет – инициируется регистрация.
     *
     * Если пользователь находится в процессе регистрации, текст передается в UserRegistrationService.
     *
     * @param message объект Message, содержащий информацию о сообщении.
     */

    private void handleMessage(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        org.telegram.telegrambots.meta.api.objects.User from = message.getFrom();
        Mono<UserDTO> userDTOMono = userService.getUserForTelegramTag(from.getUserName());
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

        switch (text.trim()) {
            case "/start" ->{
                getStartedAndCheck(userDTOMono, chatId, from);
            }
            case "Профиль" -> {
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageMainMenu(chatId)).subscribe();
                }
                case "Настройки" -> {
                    messageSenderService.sendTextMessage(chatId, "Пока раздел не реализован...");
                }
                case "Помощь" -> {
                    messageSenderService.sendTextMessage(chatId, "Справочная информация...");
                }
                case "Посмотреть данные" -> {
                    profileService.viewProfile(chatId, from.getUserName()).subscribe();
                }
                case "Изменить данные" -> {
                    messageSenderService.sendTextMessage(chatId, "Если поле не нужно менять напишите 'нет'");
                    profileEditingService.startEditingProfile(chatId, from.getUserName());
                }
                case "Назад" -> {
                    //TODO Сделать список user старост и проверять в списке старост, а не чекать в бд
                    userDTOMono.subscribe(
                            user -> {
                                if (isStarosta(user)){
                                    messageSenderService.sendButtonMessage(KeyboardUtils.createMessageStarostaMainMenu(chatId)).subscribe();
                                }else {
                                    messageSenderService.sendButtonMessage(KeyboardUtils.createMessageBackMenu(chatId)).subscribe();
                                }

                            }
                    );
                }
                case "Староста" ->{
                    userDTOMono.subscribe(
                            user -> {
                                if (isStarosta(user)){
                                    messageSenderService.sendButtonMessage(KeyboardUtils.createMessageWithStarostaMenu(chatId)).subscribe();
                                }else {
                                    messageSenderService.sendTextMessage(chatId, "Похоже, что Вы не староста!");
                                }
                            }
                    );
                }
                case "Расписание" ->{
                    messageSenderService.sendButtonMessage(KeyboardUtils.createMessageScheduleMenu(chatId)).subscribe();
                }
            case "Сегодня" -> {
                LocalDate today = LocalDate.now();
                userService.getUserForTelegramTag(from.getUserName())
                        .flatMap(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), today))
                        .flatMap(schedule -> messageSenderService.sendButtonMessage(
                                SendMessage.builder()
                                        .chatId(chatId)
                                        .text(schedule)
                                        .replyMarkup(KeyboardUtils.createMarkupWithTomorrow(today))
                                        .build()
                        ))
                        .subscribe();
            }
            case "На неделю" -> {
                LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);
                List<LocalDate> weekDates = getWeekDates(currentMonday);

                messageSenderService.sendButtonMessage(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text("📅 Выберите день недели:")
                                .replyMarkup(KeyboardUtils.createScheduleWeekMarkup(weekDates, null))
                                .build()
                ).subscribe();
            }

            case "Неделя по номеру" -> {
                //TODO нужен календарь неделю
                messageSenderService.sendTextMessage(chatId, "Введите номер недели:");
            }
            case "День по дате" -> {
                LocalDate now = LocalDate.now();
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageWithInlineCalendar(chatId, now.getYear(), now.getMonthValue())).subscribe();
            }
                default -> {
                    messageSenderService.sendTextMessage(chatId, "Неизвестная команда. Нажмите кнопку меню или напишите /start.");
                }

        }

    }

    private void getStartedAndCheck(Mono<UserDTO> userDTOMono, Long chatId, User from) {
        userDTOMono.subscribe(
                user -> {
                    if (isStarosta(user)){

                        messageSenderService.sendButtonMessage(KeyboardUtils.createHelloMessageAndStarostaMainMenu(chatId)).subscribe();
                    }else {
                        messageSenderService.sendButtonMessage(KeyboardUtils.createHelloMessageAndMainMenu(chatId)).subscribe();
                    }

                },
                error -> {
                    if (error instanceof UserNotFoundException) {
                        messageSenderService.sendTextMessage(chatId, "В первый раз? Регистрируемся!");
                        userRegistrationService.startRegistration(chatId, from.getId(), from);
                    } else {
                        log.error("Ошибка при получении пользователя: ", error);
                        messageSenderService.sendTextMessage(chatId, "Произошла ошибка, попробуйте позже.");
                    }
                }
        );
    }

    /**
     * Обрабатывает CallbackQuery, возникающий при нажатии inline‑кнопки.
     *
     * При получении данных "REG_START" инициируется процесс регистрации.
     *
     * @param callbackQuery объект CallbackQuery, содержащий данные о нажатой кнопке.
     */
    private void handleCallback(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage maybeMsg = callbackQuery.getMessage();
        String data = callbackQuery.getData();
        log.info("Нажата кнопка с данными: {}", data);

        if ("REG_START".equals(data)) {
            if (maybeMsg != null) {
                Long chatId = maybeMsg.getChat().getId();
                userRegistrationService.startRegistration(chatId,
                        callbackQuery.getFrom().getId(), callbackQuery.getFrom());
            } else {
                log.warn("Нет привязки к чату — невозможно начать регистрацию");
            }
        }
        if (data.startsWith("CALENDAR_DATE_")) {
            LocalDate selectedDate = CalendarUtils.parseDateFromCallback(data);
            if (selectedDate == null) {
                messageSenderService.sendTextMessage(maybeMsg.getChatId(), "Не удалось разобрать дату.");
                return;
            }
            userService.getUserForTelegramTag(callbackQuery.getFrom().getUserName())
                    .flatMap(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), selectedDate))
                    .flatMap(text -> {
                        return messageSenderService.editMessageText(KeyboardUtils.createEditMessage(
                                maybeMsg.getChatId().toString(),
                                maybeMsg.getMessageId(),
                                text,
                                KeyboardUtils.createMarkupWithBackToCalendarButton()));
                    })
                    .subscribe();
            return;
        }
        if (data.startsWith("CALENDAR_NAV_")) {
            String[] parts = data.split("_");
            int year = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[3]);

            InlineKeyboardMarkup calendar = CalendarUtils.buildCalendarKeyboard(year, month);
            messageSenderService.editCalendarMarkup(maybeMsg.getChatId(), maybeMsg.getMessageId(), calendar).subscribe();
            return;
        }
        if ("CALENDAR_BACK".equals(data)) {
            LocalDate now = LocalDate.now();
            SendMessage message = KeyboardUtils.createMessageWithInlineCalendar(
                    maybeMsg.getChatId(),
                    now.getYear(),
                    now.getMonthValue()
            );
            messageSenderService.editMessageText(KeyboardUtils.createEditMessage(
                    maybeMsg.getChatId().toString(),
                    maybeMsg.getMessageId(),
                    message.getText(),
                    (InlineKeyboardMarkup) message.getReplyMarkup()
            )).subscribe();
        }
        if (data.startsWith("SHOW_DAY_")) {
            LocalDate date = LocalDate.parse(data.replace("SHOW_DAY_", ""));

            userService.getUserForTelegramTag(callbackQuery.getFrom().getUserName())
                    .flatMap(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), date))
                    .flatMap(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            maybeMsg.getChatId().toString(),
                                            maybeMsg.getMessageId(),
                                            scheduleText,
                                            KeyboardUtils.createBackToWeekMarkup(date.with(DayOfWeek.MONDAY))
                                    )
                            )
                    )
                    .subscribe();

            return;
        }

        if (data.startsWith("BACK_WEEK_")) {
            LocalDate weekStartDate = LocalDate.parse(data.replace("BACK_WEEK_", ""));
            List<LocalDate> weekDates = getWeekDates(weekStartDate);

            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            maybeMsg.getChatId().toString(),
                            maybeMsg.getMessageId(),
                            "📅 Выберите день недели:",
                            KeyboardUtils.createScheduleWeekMarkup(weekDates, null)
                    )
            ).subscribe();

            return;
        }
        if (data.startsWith("TOMORROW_")) {
            LocalDate tomorrow = LocalDate.parse(data.replace("TOMORROW_", ""));

            userService.getUserForTelegramTag(callbackQuery.getFrom().getUserName())
                    .flatMap(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), tomorrow))
                    .flatMap(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            maybeMsg.getChatId().toString(),
                                            maybeMsg.getMessageId(),
                                            scheduleText,
                                            KeyboardUtils.createMarkupWithBackToToday(tomorrow.minusDays(1))
                                    )
                            )
                    )
                    .subscribe();

            return;
        }

        if (data.startsWith("BACK_TO_TODAY_")) {
            LocalDate today = LocalDate.parse(data.replace("BACK_TO_TODAY_", ""));

            userService.getUserForTelegramTag(callbackQuery.getFrom().getUserName())
                    .flatMap(user -> scheduleService.getScheduleSomeDate(user.getGroupName(), today))
                    .flatMap(scheduleText ->
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(
                                            maybeMsg.getChatId().toString(),
                                            maybeMsg.getMessageId(),
                                            scheduleText,
                                            KeyboardUtils.createMarkupWithTomorrow(today)
                                    )
                            )
                    )
                    .subscribe();

            return;
        }
    }
    private boolean isStarosta(UserDTO user) {
        return user != null && "STAROSTA".equalsIgnoreCase(user.getRole());
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
