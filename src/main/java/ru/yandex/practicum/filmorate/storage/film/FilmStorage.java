package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Collection<Film> findAllFilms();

    Film addFilm(@RequestBody Film user);

    Film updateFilm(@RequestBody Film updUser);

    void addLike(Integer filmId, Integer userId);

    void deleteLike(Integer filmId, Integer userId);

    @GetMapping(value = "/popular")
    List<Film> getPopularFilms(@RequestParam(defaultValue = "10") int count);
}
