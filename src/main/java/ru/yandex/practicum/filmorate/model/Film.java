package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Data
public class Film {
    private Integer id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private int duration;
    private int likesCount;

    @Getter
    private Set<Integer> likeScore = new HashSet<>();

    public void addLike(int userId) {
        likeScore.add(userId);
    }

    public void removeLike(int userId) {
        likeScore.remove(userId);
    }
}
