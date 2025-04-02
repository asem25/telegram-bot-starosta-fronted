package ru.semavin.bot.service.users.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.GroupService;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserService userService;
    private final MessageSenderService messageSenderService;
    private final GroupService groupService;
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
    public CompletableFuture<Object> tryBecomeStarostaIfNoOneElse(Long chatId, String telegramUsername) {
        return userService.getUserForTelegramTag(telegramUsername)
                .thenCompose(currentUser ->
                        groupService.getStarosta(currentUser.getGroupName())
                                .thenCompose(optionalStarosta -> {
                                    if (optionalStarosta.isPresent()) {
                                        UserDTO starosta = optionalStarosta.get();
                                        String msg = String.format("В вашей группе уже есть староста: %s %s.",
                                                starosta.getFirstName(), starosta.getLastName());
                                        return messageSenderService.sendMessage(chatId, msg).thenApply(r -> null);
                                    } else {
                                        return groupService.setStarosta(currentUser.getGroupName(), currentUser.getUsername())
                                                .thenCompose(res -> messageSenderService
                                                        .sendMessage(chatId, "Вы теперь староста группы!"))
                                                .thenCompose(r -> messageSenderService.sendButtonMessage(
                                                        KeyboardUtils.createMessageStarostaMainMenu(chatId)))
                                                .thenApply(resp -> (Object) resp);
                                    }
                                })
                ).exceptionally(e -> {
                    log.error("Ошибка при попытке стать старостой", e);
                    messageSenderService.sendTextMessage(chatId, "Произошла ошибка при обработке запроса.");
                    return null;
                });
    }
    public CompletableFuture<Object> refuseToBeStarosta(Long chatId, String telegramUsername) {
        return userService.getUserForTelegramTag(telegramUsername)
                .thenCompose(currentUser -> {
                    if (!"STAROSTA".equalsIgnoreCase(currentUser.getRole())) {
                        return messageSenderService
                                .sendMessage(chatId, "Вы и так не являетесь старостой.")
                                .thenApply(resp -> (Object) resp);
                    }

                    return groupService.deleteStarosta(currentUser.getGroupName(), telegramUsername)
                            .thenCompose(result ->
                                    messageSenderService.sendMessage(chatId, "Вы больше не староста.")
                            )
                            .thenApply(resp -> (Object) resp);
                })
                .exceptionally(e -> {
                    log.error("Ошибка при снятии роли старосты", e);
                    messageSenderService.sendTextMessage(chatId, "Ошибка при снятии роли старосты.");
                    return null;
                });
    }
    public CompletableFuture<Object> showCurrentStarosta(Long chatId, String telegramUsername) {
        return userService.getUserForTelegramTag(telegramUsername)
                .thenCompose(currentUser ->
                        groupService.getStarosta(currentUser.getGroupName())
                                .thenCompose(optionalStarosta -> {
                                    if (optionalStarosta.isPresent()) {
                                        UserDTO starosta = optionalStarosta.get();
                                        String msg = String.format("Староста вашей группы — %s %s.",
                                                starosta.getFirstName(), starosta.getLastName());
                                        return messageSenderService.sendMessage(chatId, msg)
                                                .thenApply(resp -> (Object) resp);
                                    } else {
                                        return messageSenderService.sendMessage(chatId, "В вашей группе пока нет старосты.")
                                                .thenApply(resp -> (Object) resp);
                                    }
                                })
                )
                .exceptionally(e -> {
                    log.error("Ошибка при получении старосты", e);
                    messageSenderService.sendTextMessage(chatId, "Ошибка при получении информации о старосте.");
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
