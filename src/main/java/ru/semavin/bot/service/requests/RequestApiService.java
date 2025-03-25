package ru.semavin.bot.service.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.RequestDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestApiService {
    private final WebClient webClient;

    @Value("${api.key}")
    private String apiKey;
    //TODO
    private final String url = "http://localhost:8081/api/v1/";

    /**
     * Получает список заявок для старосты по его идентификатору в Telegram.
     *
     * @param telegramIdStarosta идентификатор старосты в Telegram
     * @return список заявок на принятие в группу
     */
    public Mono<List<RequestDTO>> getRequests(String telegramIdStarosta) {
        return webClient.get()
                .uri(url + "/requests?telegramId=" + telegramIdStarosta)
                .header("API-KEY", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(RequestDTO.class)
                .collectList()
                .doOnNext(requests -> log.info("Получены заявки: {}", requests))
                .doOnError(error -> log.error("Ошибка получения заявок", error));
    }

    /**
     * Отправляет новую заявку от пользователя на вступление в группу.
     *
     * @param groupName название группы
     * @param telegramTagUser Telegram-тег пользователя
     * @return ответ от внешнего API в виде строки
     */
    public Mono<String> sendRequest(String groupName, String telegramTagUser) {
        RequestDTO requestDTO = RequestDTO.builder()
                .groupName(groupName)
                .telegramTagUser(telegramTagUser)
                .build();
        log.info("Построенный dto {}", requestDTO);
        return webClient.post()
                .uri(url + "/requests")
                .header("API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDTO)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.info("Заявка отправлена, ответ от API: {}", response))
                .doOnError(error -> log.error("Ошибка отправки заявки", error));
    }
}
