package ru.semavin.bot.service.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.service.MessageSenderService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestApiService requestApiService;
    private final MessageSenderService messageSenderService;

    public void sendRequest(Long chatID, String telegramTagUser, String groupName) {
        messageSenderService.sendTextMessage(chatID, "Отправили заявку старосте группы!");
        log.info("Отправлена заявка из чата <{}>, пользователя {}, группы {}", chatID, telegramTagUser, groupName);

        requestApiService.sendRequest(groupName, telegramTagUser).thenAccept(response -> {
            log.info("Ответ от API: {}", response);
            messageSenderService.sendTextMessage(chatID, "Попросите старосту побыстрее принять!");
        }).exceptionally(error -> {
            log.error("Ошибка регистрации в API", error);
            messageSenderService.sendTextMessage(chatID, "Заявка не дошла, какая-то ошибка((");
            return null;
        });
    }
}