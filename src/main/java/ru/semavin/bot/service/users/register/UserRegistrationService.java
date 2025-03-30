package ru.semavin.bot.service.users.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.requests.RequestService;
import ru.semavin.bot.service.users.UserApiService;
import ru.semavin.bot.util.KeyboardUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final RegistrationStateService stateService;
    private final MessageSenderService messageSenderService;
    private final UserApiService userApiService;
    private final RequestService requestService;
    //TODO Если не найдено группы
    public void startRegistration(Long chatId, Long telegramId, org.telegram.telegrambots.meta.api.objects.User user) {
        UserDTO data = stateService.getData(chatId);
        data.setTelegramId(telegramId);
        data.setUsername(user.getUserName());
        stateService.setStep(chatId, RegistrationStep.ENTER_FIRSTNAME);
        messageSenderService.sendTextMessage(chatId, "Введите ваше имя:");
        log.info("Начата регистрация для чата {}. Запрошено имя пользователя.", chatId);
    }

    public void processStep(Long chatId, String text) {
        RegistrationStep step = stateService.getStep(chatId);
        switch (step) {
            case ENTER_FIRSTNAME -> saveFirstName(chatId, text);
            case ENTER_LASTNAME -> saveLastName(chatId, text);
            case ENTER_GROUP -> saveGroup(chatId, text);
            default -> messageSenderService.sendTextMessage(chatId, "Пожалуйста, продолжите регистрацию.");
        }
    }

    private void saveFirstName(Long chatId, String firstName) {
        UserDTO data = stateService.getData(chatId);
        data.setFirstName(firstName);
        stateService.setStep(chatId, RegistrationStep.ENTER_LASTNAME);
        messageSenderService.sendTextMessage(chatId, "Введите вашу фамилию:");
        log.info("Сохранено имя {} для чата {}. Переход к шагу ENTER_LASTNAME", firstName, chatId);
    }

    private void saveLastName(Long chatId, String lastName) {
        UserDTO data = stateService.getData(chatId);
        data.setLastName(lastName);
        stateService.setStep(chatId, RegistrationStep.ENTER_GROUP);
        messageSenderService.sendTextMessage(chatId, "Введите название вашей группы (например, М3О-303С-22(Буквы русские!)):");
        log.info("Сохранена фамилия {} для чата {}. Переход к шагу ENTER_GROUP", lastName, chatId);
    }

    private void saveGroup(Long chatId, String groupName) {
        UserDTO data = stateService.getData(chatId);
        data.setGroupName(groupName);
        stateService.setStep(chatId, RegistrationStep.FINISHED);
        messageSenderService.sendTextMessage(chatId, "Регистрация завершена! Сохраняем ваши данные...");
        log.info("Сохранена группа {} для чата {}. Завершаем регистрацию.", groupName, chatId);

        userApiService.registerUser(data).thenAccept(response -> {
            log.info("Ответ от API: {}", response);
            messageSenderService.sendTextMessage(chatId, "Вы успешно зарегистрированы!");
            stateService.clear(chatId);
            messageSenderService.sendButtonMessage(KeyboardUtils.createMessageAfterRegistration(chatId));
        }).exceptionally(error -> {
            log.error("Ошибка регистрации в API", error);
            messageSenderService.sendTextMessage(chatId, "Произошла ошибка при регистрации. Попробуйте позже.");
            stateService.clear(chatId);
            return null;
        });
    }
}