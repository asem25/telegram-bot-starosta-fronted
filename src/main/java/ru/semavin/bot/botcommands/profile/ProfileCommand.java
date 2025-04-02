package ru.semavin.bot.botcommands.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.util.KeyboardUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileCommand implements BotCommand {
    private final MessageSenderService messageSenderService;



    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        // Отправляем сообщение с главным меню (клавиатурой), связанным с профилем
        return messageSenderService.sendButtonMessage(KeyboardUtils.createMessageMainMenu(chatId))
                .thenAccept(msgId -> {
                    log.info("Команда 'Профиль' успешно выполнена для чата {}", chatId);
                });
    }

    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Профиль".equalsIgnoreCase(message.getText().trim());
    }
}
