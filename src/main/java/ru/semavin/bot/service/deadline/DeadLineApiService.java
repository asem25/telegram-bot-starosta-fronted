package ru.semavin.bot.service.deadline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.DeadlineDTO;
import ru.semavin.bot.util.exceptions.EntityNotFoundException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLineApiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    @Value("${api.url}")
    private String baseUrl;

    public CompletableFuture<List<DeadlineDTO>> getAllByGroup(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return List.of(Objects.requireNonNull(restClient.get()
                        .uri(baseUrl + "/deadlines/group/" + groupName)
                        .retrieve()
                        .body(DeadlineDTO[].class)));
            } catch (HttpClientErrorException e) {
                handleHttpClientError("получении дедлайнов", e);
                return List.of();
            }
        }, executorService);
    }

    public CompletableFuture<Void> save(DeadlineDTO dto) {
        return CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri(baseUrl + "/deadlines")
                        .body(dto)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException e) {
                handleHttpClientError("сохранении дедлайна", e);
            }
        }, executorService);
    }

    public CompletableFuture<Void> delete(UUID id) {
        return CompletableFuture.runAsync(() -> {
            try {
                restClient.delete()
                        .uri(baseUrl + "/deadlines/" + id)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException e) {
                handleHttpClientError("удалении дедлайна", e);
            }
        }, executorService);
    }
    public CompletableFuture<List<DeadlineDTO>> getDeadlinesBetween(LocalDate from, LocalDate to) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return List.of(Objects.requireNonNull(restClient.get()
                        .uri(baseUrl + "/deadlines/reminders?from=" + from + "&to=" + to)
                        .retrieve()
                        .body(DeadlineDTO[].class)));
            } catch (HttpClientErrorException e) {
                handleHttpClientError("получении дедлайнов по диапазону дат", e);
                return List.of();
            }
        }, executorService);
    }

    public CompletableFuture<Void> markNotified(UUID id, boolean notified3Days, boolean notified1Day) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("notified3Days", notified3Days);
                update.put("notified1Day", notified1Day);

                restClient.patch()
                        .uri(baseUrl + "/deadlines/" + id + "/notify")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(update)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException e) {
                handleHttpClientError("обновлении статуса уведомления дедлайна", e);
            }
        }, executorService);
    }
    private void handleHttpClientError(String action, HttpClientErrorException e) {
        log.error("Ошибка при {}: {}", action, e.getMessage());
        String responseBody = e.getResponseBodyAsString();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String errorDescription = root.path("error_description").asText();
            log.info("Error description: {}", errorDescription);
            throw new EntityNotFoundException(errorDescription);
        } catch (IOException ex) {
            log.error("Ошибка парсинга ответа: {}", ex.getMessage());
            throw new RuntimeException(e);
        }
    }

}
