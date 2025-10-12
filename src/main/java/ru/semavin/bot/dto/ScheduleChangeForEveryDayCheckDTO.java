package ru.semavin.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
@Data
public class ScheduleChangeForEveryDayCheckDTO {
    List<ScheduleChangeForFrontDTO> scheduleChangeEntityList;

    @Data
    @Builder
    public static class ScheduleChangeForFrontDTO {
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
        private boolean deleted;
    }
}

