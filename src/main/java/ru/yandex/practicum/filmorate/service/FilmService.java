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
    private final LocalDate DATE_TO_CHECK = LocalDate.of(1895, 12, 28);
    private final Integer MAX_DESCR_LENGTH = 200;

    public Collection<Film> findAll() {
        return films.values();
    }

    public Film add(Film film) {
        idCheck(film);
        dateCheck(film);
        durationCheck(film);
        lengthCheck(film);

        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}, {}", film.getId(), film.getName());
        return film;
    }

    public Film update(Film updFilm) {
        idCheck(updFilm);
        dateCheck(updFilm);
        durationCheck(updFilm);
        lengthCheck(updFilm);

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
            throw new ConditionsNotMetException("Фильма с таким Id не существует: " + updFilm.getId());
        }
    }

    public void idCheck(Film film) {
        if (film.getName().isEmpty()) {
            log.error("Id не указан");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
    }

    public void dateCheck(Film film) {
        if (film.getReleaseDate().isBefore(DATE_TO_CHECK)) {
            log.error("Дата релиза раньше фиксированной даты: {} .", film.getReleaseDate());
            throw new ConditionsNotMetException("Дата релиза не может быть раньше " + DATE_TO_CHECK);
        }
    }

    public void durationCheck(Film film) {
        if (film.getDuration() < 0) {
            log.error("Продолжительность фильма отрицательное число: {} .", film.getDuration());
            throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
        }
    }

    public void lengthCheck(Film film) {
        if (film.getDescription().length() > MAX_DESCR_LENGTH) {
            log.error("Максимальная длинна описания превысила максимальное количество символов: {} .",
                    film.getDescription().length());
            throw new ConditionsNotMetException("Максимальная длинна описания не должна превышать 200 символов");
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
