package ru.semavin.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupDTO {
    private String groupName;
    private String starosta_username;
    private List<String> names_students;
}
