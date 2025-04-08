package ru.semavin.bot.service.schedules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.ScheduleChangeDTO;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ScheduleChangeEditingContextService {

    private final ConcurrentHashMap<Long, ScheduleChangeDTO> contextByUser = new ConcurrentHashMap<>();

    /**
     * Инициализирует контекст для пользователя новым объектом ScheduleChangeDTO.
     *
     * @param userId Идентификатор пользователя
     * @param dto Объект ScheduleChangeDTO
     */
    public void initScheduleChange(Long userId, ScheduleChangeDTO dto) {
        contextByUser.put(userId, dto);
    }

    /**
     * Возвращает объект ScheduleChangeDTO для указанного пользователя.
     *
     * @param userId Идентификатор пользователя
     * @return Optional с объектом ScheduleChangeDTO, если он существует
     */
    public Optional<ScheduleChangeDTO> getScheduleChange(Long userId) {
        return Optional.ofNullable(contextByUser.get(userId));
    }

    /**
     * Обновляет данные объекта ScheduleChangeDTO для указанного пользователя.
     *
     * @param userId Идентификатор пользователя
     * @param dto Обновлённый объект ScheduleChangeDTO
     */
    public void updateScheduleChange(Long userId, ScheduleChangeDTO dto) {
        contextByUser.put(userId, dto);
    }

    /**
     * Очищает контекст редактирования для указанного пользователя.
     *
     * @param userId Идентификатор пользователя
     */
    public void clearScheduleChange(Long userId) {
        contextByUser.remove(userId);
    }
}
