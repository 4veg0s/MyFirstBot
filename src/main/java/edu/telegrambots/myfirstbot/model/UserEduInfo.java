package edu.telegrambots.myfirstbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Entity(name = "userEduInfo")
public class UserEduInfo {
    @Id
    private Long chatId;
    private String faculty;
    private String degree;
    private Integer course;
    private String groupName;
}
