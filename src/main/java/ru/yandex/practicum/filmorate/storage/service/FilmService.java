package ru.yandex.practicum.filmorate.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.InternalException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final Map<Integer, Film> films = new HashMap<>();
    private final UserService userService;

    private static final LocalDate DATE_TO_CHECK = LocalDate.of(1895, 12, 28);
    private static final Integer MAX_DESCR_LENGTH = 200;
    private int currentId = 0;


    public Collection<Film> findAll() {
        return films.values();
    }

    public Film addFilm(Film film) {
        checkName(film);
        checkDate(film);
        checkDuration(film);
        checkLength(film);
        film.setId(++currentId);


        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}, {}", film.getId(), film.getName());
        return film;
    }

    public Film updateFilm(Film updFilm) {
        checkId(updFilm.getId());
        checkIdMap(updFilm.getId());
        checkDate(updFilm);
        checkDuration(updFilm);
        checkLength(updFilm);

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
    }

    public void addLike(Integer filmId, Integer userId) {
        checkId(filmId);
        checkIdMap(filmId);

        if (!userService.userExists(userId)) {
            log.error("Пользователя с Id {} не существует", userId);
            throw new ConditionsNotMetException("Пользователя с таким Id не существует: " + userId);
        }
        Film film = films.get(filmId);

        film.addLike(userId);
        film.setLikesCount(film.getLikesCount() + 1);
        films.put(filmId, film);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void deleteLike(Integer filmId, Integer userId) {
        checkId(filmId);
        checkIdMap(filmId);

        if (!userService.userExists(userId)) {
            log.error("Пользователя с Id {} не существует", userId);
            throw new ConditionsNotMetException("Пользователя с таким Id не существует: " + userId);
        }

        Film film = films.get(filmId);
        film.removeLike(filmId);
        film.setLikesCount(film.getLikesCount() - 1);
        films.put(filmId, film);
    }

    public List<Film> getPopularFilms(Integer count) {
        if (count <= 0) {
            log.error("Неверное значение count: {}", count);
            throw new ValidationException("Count должен быть больше 0");
        }
        log.info("Получение {} популярных фильмов", count);
        return films.values().stream()
                .sorted(Comparator.comparingInt(Film::getLikesCount).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public void checkName(Film film) {
        if (film.getName().isEmpty()) {
            log.error("Название не указано");
            throw new InternalException("Название должно быть указано");
        }
    }

    public void checkId(Integer filmId) {
        if (filmId == null) {
            log.error("Id не указан");
            throw new ValidationException("Id должен быть указан");
        }
    }

    public void checkIdMap(Integer filmId) {
        if (!films.containsKey(filmId)) {
            log.error("Фильма с этим Id не существует: {}", filmId);
            throw new ConditionsNotMetException("Фильма с таким Id не существует: " + filmId);
        }
    }

    public void checkDate(Film film) {
        if (film.getReleaseDate().isBefore(DATE_TO_CHECK)) {
            log.error("Дата релиза раньше фиксированной даты: {} .", film.getReleaseDate());
            throw new InternalException("Дата релиза не может быть раньше " + DATE_TO_CHECK);
        }
    }

    public void checkDuration(Film film) {
        if (film.getDuration() < 0) {
            log.error("Продолжительность фильма отрицательное число: {} .", film.getDuration());
            throw new InternalException("Продолжительность фильма должна быть положительным числом");
        }
    }

    public void checkLength(Film film) {
        if (film.getDescription().length() > MAX_DESCR_LENGTH) {
            log.error("Максимальная длинна описания превысила максимальное количество символов: {} .",
                    film.getDescription().length());
            throw new InternalException("Максимальная длинна описания не должна превышать 200 символов");
        }
    }

}
