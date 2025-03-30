package ru.semavin.bot.service.users;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserApiService userApiService;

    @Cacheable(value = "users", key = "#telegramTag")
    public CompletableFuture<UserDTO> getUserForTelegramTag(String telegramTag) {
        return userApiService.getUser(telegramTag);
    }

    @CacheEvict(value = "users", key = "#dto.username")
    public CompletableFuture<String> updateUser(UserDTO dto) {
        return userApiService.updateUser(dto);
    }
}
