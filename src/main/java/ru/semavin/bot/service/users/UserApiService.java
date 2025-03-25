package ru.semavin.bot.service.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.util.exceptions.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiService {
    private final WebClient webClient;
    @Value("${api.key}")
    private String apiKey;
    @Value("${api.url}")
    private String url;
    /**
     * Регистрирует пользователя через ваш API.
     * @param dto объект пользователя
     * @return Mono с ответом от API в виде строки
     */
    public Mono<String> registerUser(UserDTO dto) {

        log.info("Запрос пользователя с id: {}", dto.getTelegramId().toString());
        return webClient.post()
                .uri(url + "/users")
                .bodyValue(dto)
                .header("API-KEY", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .retry(3)
                .doOnError(error -> log.error("Ошибка при регистрации пользователя в API", error));
    }
    @Cacheable(value = "users", key = "#id")
    public Mono<UserDTO> getUser(String id) {
        log.info("Запрос пользователя с tag: {}", id);
        return webClient.get()
                .uri(url + "/users?telegramTag="+ id)
                .header("API-KEY", apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        log.info("Пользователь с id {} не найден (404)", id);
                        return Mono.error(new UserNotFoundException("Пользователь не найден"));
                    } else {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Ошибка получения пользователя с id {}: статус: {}, тело: {}",
                                            id, clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("Ошибка: "
                                            + clientResponse.statusCode() + ", тело: " + errorBody));
                                });
                    }
                })
                .bodyToMono(UserDTO.class)
                .retry(3)
                .doOnNext(user -> log.info("Пользователь получен: {}", user))
                .doOnError(e -> log.error("Ошибка при получении пользователя с id {}: ", id, e));
    }

    public Mono<String> updateUser(UserDTO userDTO) {
        return webClient.patch()
                .uri(url + "/users")
                .header("API-KEY", apiKey)
                .bodyValue(userDTO)
                .retrieve()
                .bodyToMono(String.class);
    }
}
