package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FilmService {
    private final Map<Long, Film> films = new HashMap<>();
    private final LocalDate dateToCheck = LocalDate.of(1895, 12, 28);
    private final Integer maxDescrLength = 200;

    public Collection<Film> findAll() {
        return films.values();
    }

    public Film add(Film film) {
        if (film.getName().isEmpty()) {
            log.error("Id не указан");
            throw new ConditionsNotMetException("Название должно быть указано");
        }

        if (film.getReleaseDate().isBefore(dateToCheck)) {
            log.error("Дата релиза раньше фиксированной даты: {} .", film.getReleaseDate());
            throw new ConditionsNotMetException("Дата релиза не может быть раньше " + dateToCheck);
        }

        if (film.getDuration() < 0) {
            log.error("Продолжительность фильма отрицательное число: {} .", film.getDuration());
            throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
        }

        if (film.getDescription().length() > maxDescrLength) {
            log.error("Максимальная длинна описания превысила максимальное количество символов: {} .",
                    film.getDescription().length());
            throw new ConditionsNotMetException("Максимальная длинна описания не должна превышать 200 символов");
        }

        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}, {}", film.getId(), film.getName());
        return film;
    }

    public Film update(Film updFilm) {
        if (updFilm.getId() == null) {
            log.error("Id не указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }

        if (updFilm.getReleaseDate().isBefore(dateToCheck)) {
            log.error("Введена некоректная дата: {}", updFilm.getReleaseDate());
            throw new ConditionsNotMetException("Дата релиза не может быть раньше " + dateToCheck);
        }

        if (updFilm.getDuration() < 0) {
            log.error("Введено отрицательное число в поле Duration");
            throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
        }

        if (updFilm.getDescription().length() > maxDescrLength) {
            log.error("Превышено максимальное колличество символов в описании");
            throw new ConditionsNotMetException("Максимальная длинна описания не должна превышать 200 символов");
        }

        if (films.containsKey(updFilm.getId())) {
            Film oldFilm = films.get(updFilm.getId());
            if (updFilm.getName().isEmpty()) {
                updFilm.setName(oldFilm.getName());
            }
            if (updFilm.getReleaseDate() == null) {
                updFilm.setReleaseDate(oldFilm.getReleaseDate());
            }
            if (updFilm.getDescription() == null) {
                updFilm.setDescription(oldFilm.getDescription());
            }
            if (updFilm.getReleaseDate() == null) {
                updFilm.setReleaseDate(oldFilm.getReleaseDate());
            }

            films.put(updFilm.getId(), updFilm);
            log.info("Обновлён фильм: {}, {}", oldFilm.getId(), oldFilm.getName());
            return updFilm;
        } else {
            throw new ConditionsNotMetException("Фильма с таким Id не существует");
        }
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
