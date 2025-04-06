package ru.semavin.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkipNotificationDTO {
    private UUID uuid;
    private String username;
    private String groupName;
    private String description;
    private LocalDate fromDate;
    private LocalDate toDate;


}
