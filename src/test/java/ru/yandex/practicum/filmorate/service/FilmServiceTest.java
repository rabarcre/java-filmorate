package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.service.FilmService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmServiceTest {

    FilmService filmService = new FilmService();

    @Test
    void shouldThrowExceptionIfNameIsBlank() {
        Film film = new Film();
        film.setName("");
        film.setDescription("descr");
        film.setReleaseDate(LocalDate.of(2000, 10, 10));
        film.setDuration(2000);

        assertThrows(ValidationException.class, () -> {
            filmService.addFilm(film);
        });
    }

    @Test
    void shouldThrowExceptionIfDateIsBeforeFixedDate() {
        Film film = new Film();
        film.setName("name");
        film.setDescription("descr");
        film.setReleaseDate(LocalDate.of(1800, 10, 10));
        film.setDuration(2000);

        assertThrows(ValidationException.class, () -> {
            filmService.addFilm(film);
        });
    }

    @Test
    void shouldThrowExceptionIfDurationLessThenZero() {
        Film film = new Film();
        film.setName("name");
        film.setDescription("descr");
        film.setReleaseDate(LocalDate.of(2000, 10, 10));
        film.setDuration(-2000);

        assertThrows(ValidationException.class, () -> {
            filmService.addFilm(film);
        });
    }

    @Test
    void shouldThrowExceptionIfCharInDescrMoreThenFixedChar() {
        int fixedChar = 200;
        String stringBuilder = "a".repeat(fixedChar + 1);

        Film film = new Film();
        film.setName("name");
        film.setDescription(stringBuilder);
        film.setReleaseDate(LocalDate.of(2000, 10, 10));
        film.setDuration(2000);

        assertThrows(ValidationException.class, () -> {
            filmService.addFilm(film);
        });
    }
}