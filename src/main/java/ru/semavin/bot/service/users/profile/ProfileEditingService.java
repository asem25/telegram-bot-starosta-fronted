package ru.semavin.bot.service.users.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.EditStep;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileEditingService {

    private final EditProfileStateService editProfileStateService;
    private final MessageSenderService messageSenderService;
    private final UserService userService;

    public void startEditingProfile(Long chatId, String telegramTag) {
        userService.getUserForTelegramTag(telegramTag).thenAccept(userDTO -> {
            editProfileStateService.initData(chatId, userDTO);
            editProfileStateService.setStep(chatId, EditStep.ENTER_FIRSTNAME);
            messageSenderService.sendTextMessage(chatId, "Введите новое имя (старое: " + userDTO.getFirstName() + ")");
        }).exceptionally(error -> {
            messageSenderService.sendTextMessage(chatId, "Не удалось загрузить профиль для редактирования.");
            log.error("Ошибка при загрузке профиля: ", error);
            return null;
        });
    }

    public void processEditStep(Long chatId, String text) {
        EditStep step = editProfileStateService.getStep(chatId);
        //TODO Могут тыкнуть посмотреть профиль или изменить профиль
        if ("посмотреть данные".equals(text)) {
            messageSenderService.sendTextMessage(chatId, "Вы изменяете данные. Для выхода нажмите 'Назад'");
            return;
        }
        if ("изменить данные".equals(text)) {
            messageSenderService.sendTextMessage(chatId, "Вы уже меняете данные!");
            return;
        }
        if ("назад".equalsIgnoreCase(text)) {
            editProfileStateService.clear(chatId);
            messageSenderService.sendTextMessage(chatId, "Изменение данных отменено!");
            return;
        }

        switch (step) {
            case ENTER_FIRSTNAME -> {
                if (!"нет".equalsIgnoreCase(text)) {
                    log.info("Имя пользователя {} изменено на {}", editProfileStateService.getData(chatId).getUsername(), text);
                    editProfileStateService.getData(chatId).setFirstName(text);
                }
                editProfileStateService.setStep(chatId, EditStep.ENTER_LASTNAME);
                messageSenderService.sendTextMessage(chatId, "Введите новую фамилию:");
            }
            case ENTER_LASTNAME -> {
                if (!"нет".equalsIgnoreCase(text)) {
                    log.info("Фамилия пользователя {} изменена на {}", editProfileStateService.getData(chatId).getUsername(), text);
                    editProfileStateService.getData(chatId).setLastName(text);
                }
                editProfileStateService.setStep(chatId, EditStep.ENTER_GROUP);
                messageSenderService.sendTextMessage(chatId, "Введите новую группу:");
            }
            case ENTER_GROUP -> {
                if (!"нет".equalsIgnoreCase(text)) {
                    log.info("Группа пользователя {} изменена на {}", editProfileStateService.getData(chatId).getUsername(), text);
                    editProfileStateService.getData(chatId).setGroupName(text);
                }
                finishEditing(chatId);
            }
            default -> messageSenderService.sendTextMessage(chatId, "Редактирование не запущено.");
        }
    }

    private void finishEditing(Long chatId) {
        UserDTO newData = editProfileStateService.getData(chatId);

        userService.updateUser(newData).thenAccept(response -> {
            messageSenderService.sendTextMessage(chatId, "Данные успешно обновлены!");
            editProfileStateService.clear(chatId);
        }).exceptionally(error -> {
            messageSenderService.sendTextMessage(chatId, "Ошибка при обновлении данных.");
            log.error("Ошибка updateUser: ", error);
            editProfileStateService.clear(chatId);
            return null;
        });
    }

    public boolean isEditing(Long chatId) {
        EditStep step = editProfileStateService.getStep(chatId);
        return step != EditStep.NONE && step != EditStep.FINISHED;
    }
}