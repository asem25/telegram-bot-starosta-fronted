package ru.semavin.bot.service.users.register;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.RegistrationStep;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.requests.RequestService;
import ru.semavin.bot.service.users.UserApiService;
import ru.semavin.bot.util.KeyboardUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {
    private final RegistrationStateService stateService;
    private final MessageSenderService messageSenderService;
    private final UserApiService userApiService;
    private final RequestService requestService;
    /**
     * Инициирует процесс регистрации пользователя.
     * Если пользователь уже зарегистрирован, отправляется соответствующее сообщение.
     *
     * @param chatId     идентификатор чата
     * @param telegramId идентификатор пользователя (числовой)
     * @param user       объект User, полученный из CallbackQuery или Message.getFrom()
     */
    public void startRegistration(Long chatId, Long telegramId, org.telegram.telegrambots.meta.api.objects.User user) {
        // Инициализируем данные регистрации
        UserDTO data = stateService.getData(chatId);
        data.setTelegramId(telegramId);
        data.setUsername(user.getUserName());
        stateService.setStep(chatId, RegistrationStep.ENTER_FIRSTNAME);
        messageSenderService.sendTextMessage(chatId, "Введите ваше имя:");
        log.info("Начата регистрация для чата {}. Запрошено имя пользователя.", chatId);
    }

    /**
     * Обрабатывает текущий шаг регистрации для пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @param text   текст, полученный от пользователя
     */
    public void processStep(Long chatId, String text) {
        //TODO Создание профиля старосты
        RegistrationStep step = stateService.getStep(chatId);
        switch (step) {
            case ENTER_FIRSTNAME:
                saveFirstName(chatId, text);
                break;
            case ENTER_LASTNAME:
                saveLastName(chatId, text);
                break;
            case ENTER_GROUP:
                saveGroup(chatId, text);
                break;
            default:
                messageSenderService.sendTextMessage(chatId, "Пожалуйста, продолжите регистрацию.");
                break;
        }
    }

    /**
     * Сохраняет имя пользователя и переводит регистрацию на шаг ввода фамилии.
     *
     * @param chatId    идентификатор чата
     * @param firstName введённое имя
     */
    private void saveFirstName(Long chatId, String firstName) {
        UserDTO data = stateService.getData(chatId);
        data.setFirstName(firstName);
        stateService.setStep(chatId, RegistrationStep.ENTER_LASTNAME);
        messageSenderService.sendTextMessage(chatId, "Введите вашу фамилию:");
        log.info("Сохранено имя {} для чата {}. Переход к шагу ENTER_LASTNAME", firstName, chatId);
    }

    /**
     * Сохраняет фамилию пользователя и переводит регистрацию на шаг ввода группы.
     *
     * @param chatId   идентификатор чата
     * @param lastName введенная фамилия
     */
    private void saveLastName(Long chatId, String lastName) {
        UserDTO data = stateService.getData(chatId);
        data.setLastName(lastName);
        stateService.setStep(chatId, RegistrationStep.ENTER_GROUP);
        messageSenderService.sendTextMessage(chatId, "Введите название вашей группы (например, М3О-303С-22(Буквы русские!)):");
        log.info("Сохранена фамилия {} для чата {}. Переход к шагу ENTER_GROUP", lastName, chatId);
    }

    /**
     * Сохраняет название группы пользователя и завершает регистрацию.
     *
     * @param chatId    идентификатор чата
     * @param groupName введенное название группы
     */
    private void saveGroup(Long chatId, String groupName) {
        UserDTO data = stateService.getData(chatId);
        data.setGroupName(groupName);
        stateService.setStep(chatId, RegistrationStep.FINISHED);
        messageSenderService.sendTextMessage(chatId, "Регистрация завершена! Сохраняем ваши данные...");
        log.info("Сохранена группа {} для чата {}. Завершаем регистрацию.", groupName, chatId);
        // Запрос на добавление в группу
        //TODO requestService.sendRequest(chatId, data.getUsername(), groupName);

        // Завершаем регистрацию: отправляем данные на внешний API и очищаем состояние
        userApiService.registerUser(data).subscribe(
                response -> {
                    log.info("Ответ от API: {}", response);
                    messageSenderService.sendTextMessage(chatId, "Вы успешно зарегистрированы!");
                    stateService.clear(chatId);
                    messageSenderService.sendButtonMessage(KeyboardUtils.createMessageAfterRegistration(chatId))
                            .subscribe();
                },
                error -> {
                    log.error("Ошибка регистрации в API", error);
                    messageSenderService.sendTextMessage(chatId, "Произошла ошибка при регистрации. Попробуйте позже.");
                    stateService.clear(chatId);
                }
        );
    }
}
