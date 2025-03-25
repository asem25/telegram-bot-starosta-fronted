package ru.semavin.bot.service.users.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.EditStep;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;

@RequiredArgsConstructor
@Service
@Slf4j

public class ProfileEditingService {
    private final EditProfileStateService editProfileStateService;
    private final MessageSenderService messageSenderService;
    private final UserService userService;

    public void startEditingProfile(Long chatId, String telegramTag) {
        // Загрузим текущее состояние пользователя
        userService.getUserForTelegramTag(telegramTag).subscribe(
                userDTO -> {
                    editProfileStateService.initData(chatId, userDTO);
                    editProfileStateService.setStep(chatId, EditStep.ENTER_FIRSTNAME);
                    messageSenderService.sendTextMessage(chatId, "Введите новое имя (старое: " + userDTO.getFirstName() + ")");
                },
                error -> {
                    messageSenderService.sendTextMessage(chatId, "Не удалось загрузить профиль для редактирования.");
                    log.error("Ошибка при загрузке профиля: ", error);
                }
        );
    }

    public void processEditStep(Long chatId, String text) {
        EditStep step = editProfileStateService.getStep(chatId);
        // В зависимости от шага обрабатываем

        //Завершение изменения данных
        if (text.equalsIgnoreCase("назад")) {
            editProfileStateService.clear(chatId);
            messageSenderService.sendTextMessage(chatId, "Изменение данных отменено!");
            return;
        }

        switch (step) {
            case ENTER_FIRSTNAME -> {
                if (!text.equalsIgnoreCase("нет")) {
                    log.info("Имя пользователя {} , изменено {}", editProfileStateService.getData(chatId).getUsername(), text);
                    editProfileStateService.getData(chatId).setFirstName(text);
                }
                editProfileStateService.setStep(chatId, EditStep.ENTER_LASTNAME);
                messageSenderService.sendTextMessage(chatId, "Введите новую фамилию:");
            }
            case ENTER_LASTNAME -> {
                if (!text.equalsIgnoreCase("нет")) {
                    log.info("Фамилия пользователя {} , изменена {}", editProfileStateService.getData(chatId).getUsername(), text);
                    editProfileStateService.getData(chatId).setLastName(text);
                }
                editProfileStateService.setStep(chatId, EditStep.ENTER_GROUP);
                messageSenderService.sendTextMessage(chatId, "Введите новую группу:");
            }
            case ENTER_GROUP -> {
                if (!text.equalsIgnoreCase("нет")) {
                    log.info("Группа пользователя {} , изменена {}", editProfileStateService.getData(chatId).getUsername(), text);
                    editProfileStateService.getData(chatId).setGroupName(text);
                }

                finishEditing(chatId);
            }
            default -> messageSenderService.sendTextMessage(chatId, "Редактирование не запущено.");
        }
    }

    private void finishEditing(Long chatId) {
        UserDTO newData = editProfileStateService.getData(chatId);
        // Отправляем обновление в ваш API
        userService.updateUser(newData).subscribe(
                response -> {
                    messageSenderService.sendTextMessage(chatId, "Данные успешно обновлены!");
                    editProfileStateService.clear(chatId);
                },
                error -> {
                    messageSenderService.sendTextMessage(chatId, "Ошибка при обновлении данных.");
                    log.error("Ошибка updateUser: ", error);
                    editProfileStateService.clear(chatId);
                }
        );
    }

    public boolean isEditing(Long chatId) {
        EditStep step = editProfileStateService.getStep(chatId);
        return step != EditStep.NONE && step != EditStep.FINISHED;
    }
}
