package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;


@Data
public class User {
    private Integer id;
    private String email;
    private String login;
    private String name;
    private String birthday;

    private Set<Integer> friendsList = new HashSet<>();

    public void addFriend(int friendId) {
        friendsList.add(friendId);
    }

    public void removeFriend(int friendId) {
        friendsList.remove(friendId);
    }
}
