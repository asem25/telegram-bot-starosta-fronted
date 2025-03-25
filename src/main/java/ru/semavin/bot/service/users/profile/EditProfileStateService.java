package ru.semavin.bot.service.users.profile;

import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.enums.EditStep;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит промежуточные данные о редактировании профиля пользователя:
 * - текущий шаг
 * - временный UserDTO, куда складываем новые данные
 */
@Service
public class EditProfileStateService {
    // Храним для каждого chatId текущий шаг редактирования
    private final Map<Long, EditStep> editSteps = new ConcurrentHashMap<>();
    // Храним для каждого chatId временный UserDTO (новые вводимые данные)
    private final Map<Long, UserDTO> editData = new ConcurrentHashMap<>();

    /**
     * Получаем текущий шаг редактирования для чата
     */
    public EditStep getStep(Long chatId) {
        return editSteps.getOrDefault(chatId, EditStep.NONE);
    }

    /**
     * Устанавливаем шаг
     */
    public void setStep(Long chatId, EditStep step) {
        editSteps.put(chatId, step);
    }

    /**
     * Получаем временный UserDTO (создаём, если нет)
     */
    public UserDTO getData(Long chatId) {
        return editData.computeIfAbsent(chatId, k -> new UserDTO());
    }

    /**
     * Очищаем состояние (когда редактирование закончено или прервано)
     */
    public void clear(Long chatId) {
        editSteps.remove(chatId);
        editData.remove(chatId);
    }
    public void initData(Long chatId, UserDTO userDTO) {
        // Сбрасываем предыдущий шаг, если нужен "чистый" старт
        editSteps.put(chatId, EditStep.NONE);
        // Записываем начальный UserDTO, полученный из API
        editData.put(chatId, userDTO);
    }

}
