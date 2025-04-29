package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
public class Film {
    private Integer id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private int duration;
    private int likesCount;
    private List<Genre> genres;
    private Rating mpa;

    @Getter
    private Set<Integer> likeScore = new HashSet<>();

    public void addLike(int userId) {
        likeScore.add(userId);
    }

    public void removeLike(int userId) {
        likeScore.remove(userId);
    }
}
