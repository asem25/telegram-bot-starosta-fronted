package ru.semavin.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeadlineDTO {
    private UUID id;                  // Уникальный ID дедлайна
    private String title;             // Название задания
    private String description;       // Подробности
    private LocalDate dueDate;        // Срок
    private String groupName;         // Назначено для группы
    private List<String> recipients;  // Telegram username'ы получателей
    private boolean notified3Days;    // Было ли уведомление за 3 дня
    private boolean notified1Day;
}
