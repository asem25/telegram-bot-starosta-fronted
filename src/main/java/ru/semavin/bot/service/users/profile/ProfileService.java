package ru.semavin.bot.service.users.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserService userService;
    private final MessageSenderService messageSenderService;

    public void viewProfile(Long chatId, String telegramTag) {
        userService.getUserForTelegramTag(telegramTag).thenAccept(userDTO -> {
            String messageText = buildProfileViewText(userDTO);
            messageSenderService.sendMessage(chatId, messageText);
        }).exceptionally(e -> {
            log.error("Ошибка при получении данных пользователя: ", e);
            messageSenderService.sendTextMessage(chatId, "Не удалось получить данные пользователя.");
            return null;
        });
    }

    private String buildProfileViewText(UserDTO userDTO) {
        return String.format("""
                Ваш профиль:
                Имя: %s
                Фамилия: %s
                Группа: %s
                Telegram: @%s
                %s
                """,
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getGroupName(),
                userDTO.getUsername(),
                userDTO.getRole().equalsIgnoreCase("starosta") ? "Роль: Староста" : ""
        );
    }
}
