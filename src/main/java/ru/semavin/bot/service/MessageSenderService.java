package ru.semavin.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSenderService {

    private final RestClient restClient;
    private final ExecutorService executorService;

    @Value("${bot.token}")
    private String botToken;

    @Value("${admin.contact}")
    private Long chatIdAdmin;

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    public CompletableFuture<String> sendMessage(Long chatId, String text) {
        return CompletableFuture.supplyAsync(() -> {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build();

            return restClient.post()
                    .uri(TELEGRAM_API_BASE + botToken + "/sendMessage")
                    .body(message)
                    .retrieve()
                    .body(String.class);
        }, executorService).exceptionally(ex -> {
            log.error("Ошибка при отправке сообщения: {}", ex.getMessage());
            return null;
        });
    }

    public CompletableFuture<String> sendMessageWithMarkDown(Long chatId, String text) {
        return CompletableFuture.supplyAsync(() -> {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("Markdown")
                    .build();

            return restClient.post()
                    .uri(TELEGRAM_API_BASE + botToken + "/sendMessage")
                    .body(message)
                    .retrieve()
                    .body(String.class);
        }, executorService).exceptionally(ex -> {
            log.error("Ошибка при отправке сообщения: {}", ex.getMessage());
            return null;
        });
    }

    public CompletableFuture<String> sendButtonMessage(SendMessage message) {
        return CompletableFuture.supplyAsync(() -> restClient.post()
                        .uri(TELEGRAM_API_BASE + botToken + "/sendMessage")
                        .body(message)
                        .retrieve()
                        .body(String.class), executorService)
                .exceptionally(ex -> {
                    log.error("Ошибка при отправке сообщения с кнопками: {}", ex.getMessage());
                    return null;
                });
    }

    public void sendTextMessage(Long chatId, String text) {
        sendMessage(chatId, text).thenAccept(response -> log.info("Сообщение отправлено: {}", text))
                .exceptionally(ex -> {
                    log.error("Ошибка при отправке сообщения: {}", ex.getMessage());
                    return null;
                });
    }

    public void sendTextErrorMessage(Long chatId, String text, Throwable throwable) {
        String errorMsg = Optional.ofNullable(throwable)
                .map(Throwable::toString)
                .orElse("Неизвестная ошибка");

        var userFuture = sendMessage(chatId, text);
        var adminFuture = sendMessage(chatIdAdmin,
                "Ошибка при отправке пользователю " + chatId + ":\n" + errorMsg);

        CompletableFuture.allOf(adminFuture, userFuture)
                .whenComplete((v, ex) -> {
                    if (ex != null) log.error("Ошибка при отправке ошибки пользователю: {}", ex.getMessage());
                });
    }

    public void sendTextErrorMessage(Throwable throwable) {
        String errorMsg = Optional.ofNullable(throwable)
                .map(Throwable::toString)
                .orElse("Неизвестная ошибка");

        var adminFuture = sendMessage(chatIdAdmin,
                "Ошибка при выполнении" + ":\n" + errorMsg);

        CompletableFuture.allOf(adminFuture)
                .whenComplete((v, ex) -> {
                    if (ex != null) log.error("Ошибка при отправке ошибки пользователю: {}", ex.getMessage());
                });
    }

    public void sendTextWithMarkdown(Long chaId, String text) {
        sendMessageWithMarkDown(chaId, text).thenAccept(response -> log.info("Сообщение отправлено: {}", text))
                .exceptionally(ex -> {
                    log.error("Ошибка при отправке сообщения: {}", ex.getMessage());
                    return null;
                });
    }

    public CompletableFuture<String> editMessageMarkup(EditMessageReplyMarkup markup) {
        return CompletableFuture.supplyAsync(() -> restClient.post()
                        .uri(TELEGRAM_API_BASE + botToken + "/editMessageReplyMarkup")
                        .body(markup)
                        .retrieve()
                        .body(String.class), executorService)
                .exceptionally(ex -> {
                    log.error("Ошибка при редактировании клавиатуры: {}", ex.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> editMessageText(EditMessageText editMessageText) {
        return CompletableFuture.runAsync(() -> restClient.post()
                        .uri(TELEGRAM_API_BASE + botToken + "/editMessageText")
                        .body(editMessageText)
                        .retrieve()
                        .toBodilessEntity(), executorService)
                .thenAccept(resp -> log.info("Сообщение {} успешно отредактировано", editMessageText.getMessageId()))
                .exceptionally(ex -> {
                    log.error("Ошибка при редактировании текста сообщения: {}", ex.getMessage());
                    return null;
                });
    }

    public CompletableFuture<String> editCalendarMarkup(Long chatId, Integer messageId, InlineKeyboardMarkup markup) {
        EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                .chatId(chatId.toString())
                .messageId(messageId)
                .replyMarkup(markup)
                .build();

        return editMessageMarkup(edit);
    }
}
