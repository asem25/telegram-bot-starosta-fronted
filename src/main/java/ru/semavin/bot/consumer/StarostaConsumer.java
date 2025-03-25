package ru.semavin.bot.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.*;
import ru.semavin.bot.service.users.profile.ProfileEditingService;
import ru.semavin.bot.service.users.profile.ProfileService;
import ru.semavin.bot.service.users.register.RegistrationStateService;
import ru.semavin.bot.service.users.register.UserRegistrationService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.exceptions.UserNotFoundException;

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
    //TODO реализация меню для старосты
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
                default -> {
                    // Любой текст, который не совпал с кнопками
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
    }
    private boolean isStarosta(UserDTO user) {
        return user != null && "STAROSTA".equalsIgnoreCase(user.getRole());
    }
}
