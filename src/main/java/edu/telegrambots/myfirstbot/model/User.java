package edu.telegrambots.myfirstbot.model;

import edu.telegrambots.myfirstbot.enums.UserState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

// TODO: починить таблицу(некоторые аттрибуты не добавились)
@Getter
@Setter
@Entity(name = "usersDataTable")
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    @CreationTimestamp
    private Timestamp registeredAt;
    private int course;
    private String faculty;
    private String group;
    @Enumerated(EnumType.STRING)
    private UserState state;

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registeredAt=" + registeredAt +
                ", course=" + course +
                ", faculty='" + faculty + '\'' +
                ", group='" + group + '\'' +
                ", state=" + state +
                '}';
    }
}
