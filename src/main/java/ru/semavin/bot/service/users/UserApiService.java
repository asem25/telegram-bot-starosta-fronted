package ru.semavin.bot.service.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.util.exceptions.UserNotFoundException;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiService {

    private final RestClient restClient;
    private final ExecutorService executorService;

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.url}")
    private String url;

    public CompletableFuture<String> registerUser(UserDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Запрос регистрации пользователя с id: {}", dto.getTelegramId());
                return restClient.post()
                        .uri(url + "/users")
                        .header("API-KEY", apiKey)
                        .body(dto)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                log.error("Ошибка при регистрации пользователя в API", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    @Cacheable(value = "users", key = "#id")
    public CompletableFuture<UserDTO> getUser(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Запрос пользователя с tag: {}", id);
                return restClient.get()
                        .uri(url + "/users?telegramTag={id}", id)
                        .header("API-KEY", apiKey)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                            if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                                throw new UserNotFoundException("Пользователь не найден");
                            }
                        })
                        .body(UserDTO.class);
            } catch (HttpClientErrorException.NotFound e) {
                log.info("Пользователь с id {} не найден (404)", id);
                throw new UserNotFoundException("Пользователь не найден");
            } catch (Exception e) {
                log.error("Ошибка при получении пользователя с id {}: ", id, e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<String> updateUser(UserDTO userDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return restClient.patch()
                        .uri(url + "/users")
                        .header("API-KEY", apiKey)
                        .body(userDTO)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                log.error("Ошибка при обновлении пользователя: ", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
}