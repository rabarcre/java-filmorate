package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Collection<Film> findAllFilms();

    Film addFilm(Film film);

    Film updateFilm(Film updFilm);

    Film getFilmById(int filmId);

    void addLike(Integer filmId, Integer userId);

    void deleteLike(Integer filmId, Integer userId);

    List<Film> getPopularFilms(int count);
}
