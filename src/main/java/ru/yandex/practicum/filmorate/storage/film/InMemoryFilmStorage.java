package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.service.FilmService;

import java.util.Collection;
import java.util.List;

@Component
@RestController
@RequestMapping("/films")
public class InMemoryFilmStorage implements FilmStorage {
    private final FilmService filmService;

    public InMemoryFilmStorage(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    @Override
    public Collection<Film> findAllFilms() {
        return filmService.findAll();
    }

    @PostMapping
    @Override
    public Film addFilm(@RequestBody Film film) {
        return filmService.addFilm(film);
    }

    @PutMapping
    @Override
    public Film updateFilm(@RequestBody Film updFilm) {
        return filmService.updateFilm(updFilm);
    }

    @PutMapping(value = "/{filmId}/like/{userId}")
    @Override
    public void addLike(@PathVariable("filmId") Long filmId, @PathVariable("userId") Long userId) {
        filmService.addLike(filmId, userId);
    }

    @DeleteMapping(value = "/{filmId}/like/{userId}")
    @Override
    public void deleteLike(@PathVariable("filmId") Long filmId, @PathVariable("userId") Long userId) {
        filmService.deleteLike(filmId, userId);
    }

    @GetMapping(value = "/popular?count={count}")
    @Override
    public List<Film> topLikesCount(@PathVariable("count") Integer count) {
        return filmService.filmsWithTopLikesCount(count);
    }
}
