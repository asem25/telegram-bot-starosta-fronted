package ru.semavin.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@ Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleChangeDTO {
    private String subjectName;
    private String lessonType;
    private String teacherName;
    private String classroom;
    private LocalDate oldLessonDate;
    private LocalTime oldStartTime;
    private LocalTime oldEndTime;

    private LocalDate newLessonDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;

    private String description;
}
