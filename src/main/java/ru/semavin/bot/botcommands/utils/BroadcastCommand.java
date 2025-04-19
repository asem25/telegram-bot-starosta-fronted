package ru.semavin.bot.botcommands.utils;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;      // существует  :contentReference[oaicite:0]{index=0}&#8203;:contentReference[oaicite:1]{index=1}
import ru.semavin.bot.service.broadcast.BroadCastService;
import ru.semavin.bot.service.users.UserService;


import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Команда «Рассылка», доступная только старосте.
 * Формат сообщения:  "Рассылка: <текст объявления>"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BroadcastCommand implements BotCommand {

    private static final Pattern CMD = Pattern.compile("^\\s*Рассылка\\s*:\\s*(.+)", Pattern.DOTALL);

    private final UserService userService;
    private final BroadCastService broadcastService;

    @Override
    public boolean canHandle(Message message) {
        return CMD.matcher(message.getText()).matches();
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Matcher m = CMD.matcher(message.getText());
        if (!m.matches()) {
            return CompletableFuture.completedFuture(null);
        }
        String payload = m.group(1).trim();
        Long chatId   = message.getChatId();
        String author = message.getFrom().getUserName();

        return userService.getUserForTelegramTag(author).thenCompose(userDTO -> {
            if (!userService.isStarosta(userDTO)) {
                return broadcastService.replyForbidden(chatId);
            }
            return broadcastService.broadcastToGroup(userDTO.getGroupName(), payload, author);
        });
    }
}

