package ru.semavin.bot.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.ScheduleDTO;
import ru.semavin.bot.dto.SkipNotificationDTO;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Сервис для взаимодействия с внешним API уведомлений.
 * Например, при добавлении/удалении пропуска.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationApiService {

    private final RestClient restClient;
    private final ExecutorService executorService;

    @Value("${api.url}")
    private String notificationApiBase;

    /**
     * Отправляем пропуск во внешний сервис (POST).
     * @param dto DTO пропуска
     */
    public CompletableFuture<String> sendSkipNotification(SkipNotificationDTO dto) {
        log.info("Body to post {}", dto);
        return CompletableFuture.supplyAsync(() -> {
            // Например, POST /add
            return restClient.post()
                    .uri(notificationApiBase + "/notification/add")
                    .body(dto)
                    .retrieve()
                    .body(String.class);
        }, executorService).whenComplete((resp, ex) -> {
            if (ex != null) {
                log.error("Ошибка при отправке пропуска во внешний API: {}", ex.getMessage());
            } else {
                log.info("Пропуск успешно отправлен во внешний API. Ответ: {}", resp);
            }
        });
    }

    /**
     * Удаляем пропуск во внешнем сервисе (DELETE).
     * Вариант, если у SkipNotificationDTO есть id,
     * или можно построить query param.
     */
    public CompletableFuture<Void> deleteSkipNotification(UUID id) {
        return CompletableFuture.runAsync(() -> {
            // Допустим, есть dto.getId() или из dto формируем query
            String url = notificationApiBase + "/notification/delete?id=" + id;

            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
        }, executorService).whenComplete((ok, ex) -> {
            if (ex != null) {
                log.error("Ошибка при удалении пропуска во внешнем API: {}", ex.getMessage());
            } else {
                log.info("Пропуск удалён во внешнем API: {}", id);
            }
        });
    }
    public CompletableFuture<List<SkipNotificationDTO>> getAllNotification(String groupName) {
        return CompletableFuture.supplyAsync(() ->
                    restClient.get()
                            .uri(notificationApiBase + "/notification?groupName=" + groupName)
                            .retrieve()
                            .body(new ParameterizedTypeReference<List<SkipNotificationDTO>>() {
                            })
                , executorService);
    }
}
