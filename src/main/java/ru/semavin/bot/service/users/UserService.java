package ru.semavin.bot.service.users;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.UserDTO;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserApiService userApiService;
    /**
     * Получаем пользователя по Telegram‑тегу (username).
     * Кэшируем результат на 1 час (это задаётся в конфигурации кэша, а не здесь).
     */
    @Cacheable(value = "users", key = "#telegramTag")
    public Mono<UserDTO> getUserForTelegramTag(String telegramTag) {
        return userApiService.getUser(telegramTag);
    }

    /**
     * Обновляем пользователя и сбрасываем кэш,
     * чтобы при следующем запросе получить актуальные данные.
     */
    @CacheEvict(value = "users", key = "#dto.username")
    public Mono<String> updateUser(UserDTO dto) {
        return userApiService.updateUser(dto);
    }
}
