package ru.semavin.bot.service.schedules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.ScheduleChangeDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ScheduleChangeEditingContextService {

    private final ConcurrentHashMap<Long, ScheduleChangeDTO> contextByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> editingFieldByUser = new ConcurrentHashMap<>();
    // Хранилище для последнего messageId (как строку, если так удобнее)
    private final ConcurrentHashMap<Long, Integer> lastMessageIdByUser = new ConcurrentHashMap<>();

    public void initScheduleChange(Long userId, ScheduleChangeDTO dto) {
        contextByUser.put(userId, dto);
    }

    public Optional<ScheduleChangeDTO> getScheduleChange(Long userId) {
        return Optional.ofNullable(contextByUser.get(userId));
    }

    public void updateScheduleChange(Long userId, ScheduleChangeDTO dto) {
        contextByUser.put(userId, dto);
    }

    public void setEditingField(Long userId, String fieldName) {
        editingFieldByUser.put(userId, fieldName);
    }

    public Optional<String> getEditingField(Long userId) {
        return Optional.ofNullable(editingFieldByUser.get(userId));
    }

    public void clearEditingState(Long userId) {
        editingFieldByUser.remove(userId);
    }

    public void clearScheduleChange(Long userId) {
        contextByUser.remove(userId);
        clearEditingState(userId);
        lastMessageIdByUser.remove(userId);
    }


    public Optional<ScheduleChangeDTO> processEditingInput(Long userId, String userInput) {
        // Получаем, какое поле сейчас редактируется
        Optional<String> maybeField = getEditingField(userId);
        if (maybeField.isEmpty()) {
            return Optional.empty();
        }
        String field = maybeField.get();
        Optional<ScheduleChangeDTO> maybeDto = getScheduleChange(userId);
        if (maybeDto.isEmpty()) {
            return Optional.empty();
        }
        ScheduleChangeDTO dto = maybeDto.get();
        try {
            // Обрабатываем ввод пользователя в зависимости от редактируемого поля
            switch (field) {
                case "description" -> dto.setDescription(userInput);
                case "newLessonDate" -> dto.setNewLessonDate(LocalDate.parse(userInput)); // ожидается формат yyyy-MM-dd
                case "newStartTime" -> dto.setNewStartTime(LocalTime.parse(userInput));   // ожидается формат HH:mm
                case "newEndTime" -> dto.setNewEndTime(LocalTime.parse(userInput));
                case "classroom" -> dto.setClassroom(userInput);
                default -> log.warn("Неизвестное поле для редактирования: {}", field);
            }
            // Обновляем сохранённый объект в контексте и сбрасываем редактирование
            updateScheduleChange(userId, dto);
            clearEditingState(userId);
            return Optional.of(dto);
        } catch (Exception e) {
            log.error("Ошибка при обработке ввода для поля {}: {}", field, e.getMessage());
            return Optional.empty();
        }
    }
    public String buildScheduleChangeText(ScheduleChangeDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("Изменения для занятия:\n");

        // Даты
        sb.append("Дата занятия: ")
                .append(dto.getOldLessonDate());
        if (dto.getNewLessonDate() != null) {
            sb.append(" → ").append(dto.getNewLessonDate());
        }
        sb.append("\n");

        // Время: oldStartTime - oldEndTime --> newStartTime - newEndTime (если заданы)
        sb.append("Время занятия: ")
                .append(dto.getOldStartTime()).append(" – ").append(dto.getOldEndTime());

        // Логика показа новых значений: даже если изменили только start или только end, всё равно покажем
        sb.append(" → ");
        if (dto.getNewStartTime() != null) {
            sb.append(dto.getNewStartTime());
        } else {
            // Если пользователь изменил только новое конечное время, а начало не тронул
            sb.append(dto.getOldStartTime());
        }
        sb.append(" – ");
        if (dto.getNewEndTime() != null) {
            sb.append(dto.getNewEndTime());
        } else {
            sb.append(dto.getOldEndTime());
        }
        sb.append("\n");

        sb.append("Предмет: ").append(dto.getSubjectName()).append("\n");

        sb.append("Аудитория: ").append(dto.getClassroom()).append("\n");

        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            sb.append("Комментарий: ").append(dto.getDescription());
        }

        return sb.toString();
    }
}
