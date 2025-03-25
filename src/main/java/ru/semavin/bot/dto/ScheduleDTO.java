package ru.semavin.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDTO {
    private String groupName;
    private String subjectName;
    private String lessonType; // LECTURE, PARCTIK, LABORATY
    private String teacherName;
    private String classroom;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
