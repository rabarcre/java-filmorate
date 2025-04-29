package ru.yandex.practicum.filmorate.DAO.film;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.Collection;
import java.util.List;

@Component("FilmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final FilmDAO filmDAO;

    @Override
    public Collection<Film> findAllFilms() {
        return filmDAO.findAllFilms();
    }

    @Override
    public Film addFilm(Film film) {
        return filmDAO.addFilm(film);
    }

    @Override
    public Film updateFilm(Film updFilm) {
        return filmDAO.updateFilm(updFilm);
    }

    @Override
    public Film getFilmById(int filmId) {
        return filmDAO.getFilmById(filmId);
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        filmDAO.addLike(filmId, userId);
    }

    @Override
    public void deleteLike(Integer filmId, Integer userId) {
        filmDAO.deleteLike(filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return filmDAO.getPopularFilms(count);
    }
}
