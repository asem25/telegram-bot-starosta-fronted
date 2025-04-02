package ru.semavin.bot.botcommands;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.concurrent.CompletableFuture;
/**
 * Интерфейс для обработки команд бота.
 */
public interface BotCommand {
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    boolean canHandle(Message message);

    /**
     * Выполняет обработку команды.
     *
     * @param message входящее сообщение
     * @return CompletableFuture, позволяющий работать асинхронно
     */
    CompletableFuture<Void> execute(Message message);
}
