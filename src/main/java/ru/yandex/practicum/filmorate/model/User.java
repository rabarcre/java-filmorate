package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;


@Data
public class User {
    private Long id;
    private String email;
    private String login;
    private String name;
    private String birthday;

    @ToString.Exclude
    private List<User> friendList;
}
