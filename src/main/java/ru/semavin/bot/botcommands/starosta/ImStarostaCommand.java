package ru.semavin.bot.botcommands.starosta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.users.profile.ProfileService;

import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Component
public class ImStarostaCommand implements BotCommand {
    private final ProfileService profileService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Я староста".equalsIgnoreCase(message.getText().trim());
    }

    /**
     * Выполняет обработку команды.
     *
     * @param message входящее сообщение
     * @return CompletableFuture, позволяющий работать асинхронно
     */
    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();

        profileService.tryBecomeStarostaIfNoOneElse(chatId, message.getFrom().getUserName());

        return CompletableFuture.completedFuture(null);
    }
}
