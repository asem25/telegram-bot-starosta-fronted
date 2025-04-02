package ru.semavin.bot.botcommands.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.service.users.register.UserRegistrationService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.exceptions.EntityNotFoundException;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class StartCommand implements BotCommand {
    private final UserService userService;
    private final UserRegistrationService userRegistrationService;
    private final MessageSenderService messageSenderService;

    @Override
    public boolean canHandle(Message message) {
        return "/start".equalsIgnoreCase(message.getText().trim());
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        String username = message.getFrom().getUserName();

        // 1) Получаем пользователя по его Telegram-username
        return userService.getUserForTelegramTag(username)
                // 2) thenCompose, чтобы при успехе отправить меню
                .thenCompose(userDTO -> {
                    if (userDTO != null && "STAROSTA".equalsIgnoreCase(userDTO.getRole())) {
                        // Если пользователь староста, отправляем меню старосты
                        return messageSenderService.sendButtonMessage(
                                KeyboardUtils.createHelloMessageAndStarostaMainMenu(chatId)
                        );
                    } else {
                        // Иначе — меню обычного пользователя
                        return messageSenderService.sendButtonMessage(
                                KeyboardUtils.createHelloMessageAndMainMenu(chatId)
                        );
                    }
                })
                // 3) exceptionally обрабатывает любую ошибку в цепочке
                .exceptionally(ex -> {
                    // Можно получить корневую причину, проверяя ex.getCause()
                    // Если пользователь не найден — начинаем регистрацию
                    if (ex.getCause() != null && ex.getCause().getCause() instanceof EntityNotFoundException) {
                        log.info("Пользователь не найден, начинаем регистрацию: {}", username);
                        messageSenderService.sendTextMessage(chatId, "В первый раз? Регистрируемся!");
                        userRegistrationService.startRegistration(chatId, message.getFrom().getId(), message.getFrom());
                    } else {
                        // Логируем прочие ошибки
                        log.error("Ошибка при обработке команды /start: {}", ex.getMessage(), ex);
                        messageSenderService.sendTextMessage(chatId, "Произошла ошибка, попробуйте позже.");
                    }
                    return null;
                })
                .thenApply(messageId -> {
                    return null;
                });

    }
}
