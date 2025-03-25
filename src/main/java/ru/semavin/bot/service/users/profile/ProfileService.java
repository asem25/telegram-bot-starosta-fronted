package ru.semavin.bot.service.users.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProfileService {
    private final UserService userService;
    private final MessageSenderService messageSenderService;

    /**
     * Получает профиль пользователя из внешнего API (через UserService)
     * и отправляет его содержимое в чат.
     *
     * @param chatId      идентификатор телеграм-чата
     * @param telegramTag уникальный username пользователя (без @)
     */
    public Mono<Void> viewProfile(Long chatId, String telegramTag) {
        return userService.getUserForTelegramTag(telegramTag)
                .flatMap(userDTO -> {
                    // Формируем сообщение
                    String messageText = buildProfileViewText(userDTO);
                    // Отправляем текст
                    return messageSenderService.sendMessage(chatId, messageText).then();
                })
                .doOnError(e -> {
                    // Если пользователь не найден или ошибка
                    messageSenderService.sendTextMessage(chatId, "Не удалось получить данные пользователя.");
                })
                .onErrorResume(e -> Mono.empty()); // чтобы не ронять обработку
    }


    /**
     * Формирует человекочитаемый текст профиля из UserDTO
     */
    private String buildProfileViewText(UserDTO userDTO) {
        // Пример простого формирования текстового сообщения
        return String.format("""
                Ваш профиль:
                Имя: %s
                Фамилия: %s
                Группа: %s
                Telegram: @%s
                """,
                userDTO.getFirstName(),
                userDTO.getLastName(),
                userDTO.getGroupName(),
                userDTO.getUsername()
        ) +(userDTO.getRole().equalsIgnoreCase("starosta") ? "Роль: Староста" : "");
    }
}
