package ru.semavin.bot.service.users.register;

import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.RegistrationStep;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistrationStateService {
    // Хранит текущий шаг для каждого пользователя (по chatId)
    private final Map<Long, RegistrationStep> userSteps = new ConcurrentHashMap<>();

    // Хранит вводимые данные
    private final Map<Long, UserDTO> userData = new ConcurrentHashMap<>();

    public RegistrationStep getStep(Long chatId) {
        return userSteps.getOrDefault(chatId, RegistrationStep.NONE);
    }

    public void setStep(Long chatId, RegistrationStep step) {
        userSteps.put(chatId, step);
    }

    public UserDTO getData(Long chatId) {
        return userData.computeIfAbsent(chatId, id -> UserDTO.builder().build());
    }

    public void clear(Long chatId) {
        userSteps.put(chatId, RegistrationStep.NONE);
        userData.remove(chatId);
    }
}
