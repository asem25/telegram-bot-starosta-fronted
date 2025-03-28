package ru.semavin.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.CorePublisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSenderService {

    private final WebClient webClient;
    @Value("${bot.token}")
    private String botToken;

    private  final  String URL = "https://api.telegram.org/bot" ;
    /**
     * Отправляет сообщение в указанный чат через Telegram API.
     *
     * @param chatId идентификатор чата, куда нужно отправить сообщение.
     * @param text   текст сообщения.
     * @return Mono с ответом от Telegram API.
     */
    public Mono<String> sendMessage(Long chatId, String text) {

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();
        return webClient.post()
                .uri(URL + botToken + "/sendMessage")
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Ошибка при отправке сообщения", error));
    }
    public Mono<String> sendButtonMessage(SendMessage message) {
        return webClient.post()
                .uri(URL + botToken + "/sendMessage")
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Ошибка при отправке сообщения", error));
    }
    /**
     * Отправляет текстовое сообщение в указанный чат.
     * <p>
     * В данном примере отправка сообщения реализована через логирование.
     * В реальной реализации здесь следует использовать метод отправки сообщений (например, execute(SendMessage)).
     * </p>
     *
     * @param chatId идентификатор чата.
     * @param text   текст сообщения.
     */
    public void sendTextMessage(Long chatId, String text) {
        sendMessage(chatId, text).subscribe(
                response -> log.info("Сообщение отправлено: {}", text),
                error -> log.error("Ошибка при отправке сообщения", error)
        );
    }


    public Mono<String> editMessageMarkup(EditMessageReplyMarkup markup) {
        return webClient.post()
                .uri(URL + botToken + "/editMessageReplyMarkup")
                .bodyValue(markup)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Ошибка при редактировании клавиатуры", error));
    }
    public Mono<Void> editMessageText(EditMessageText editMessageText) {
        log.info("Попытка редактирования сообщения {}", editMessageText.getReplyMarkup().getKeyboard());

        return webClient.post()
                .uri(URL + botToken + "/editMessageText")
                .bodyValue(editMessageText)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(resp -> log.info("Сообщение {} успешно отредактировано", editMessageText.getMessageId()))
                .doOnError(error -> log.error("Ошибка при редактировании текста сообщения", error))
                .then();
    }
    public Mono<String> editCalendarMarkup(Long chatId, Integer messageId, InlineKeyboardMarkup markup) {
        //TODO Вынести логику создания кнопок в ${link KeyBoardUtils}


        List<InlineKeyboardRow> rows = new ArrayList<>(markup.getKeyboard());

        EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build())
                .build();
        return editMessageMarkup(edit);
    }
}
