package ru.yandex.practicum.filmorate.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FilmService {
    private final Map<Long, Film> films = new HashMap<>();
    private final UserService userService;

    private static final LocalDate DATE_TO_CHECK = LocalDate.of(1895, 12, 28);
    private static final Integer MAX_DESCR_LENGTH = 200;
    private static final Integer maxFilmCount = 10;

    public FilmService(UserService userService) {
        this.userService = userService;
    }

    public Collection<Film> findAll() {
        return films.values();
    }

    public Film addFilm(Film film) {
        checkName(film);
        checkDate(film);
        checkDuration(film);
        checkLength(film);

        film.setId(getNextId());
        if (film.getUserIdLikes() == null) {
            List<Long> userIdLikes = new ArrayList<>();
            film.setUserIdLikes(userIdLikes);
        }
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

    //добавление и удаление лайка, вывод 10 наиболее популярных фильмов по количеству лайков.
    // Пока пусть каждый пользователь может поставить лайк фильму только один раз.

    public void addLike(Long filmId, Long userId) {
        checkId(filmId);
        checkIdMap(filmId);
        checkDoubleLike(filmId, userId);
        if (!userService.userExists(filmId)) {
            log.error("Пользователя с Id {} не существует", userId);
            throw new ConditionsNotMetException("Пользователя с таким Id не существует: " + userId);
        }

        Film film = films.get(filmId);
        List<Long> userLikes = film.getUserIdLikes();
        userLikes.add(userId);
        film.setUserIdLikes(userLikes);
        film.setLikesCount(film.getLikesCount() + 1);
        films.put(filmId, film);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        checkId(filmId);
        checkIdMap(filmId);
        checkLikeExists(filmId, userId);

        Film film = films.get(filmId);
        List<Long> userLikes = film.getUserIdLikes();
        userLikes.remove(userId);
        film.setUserIdLikes(userLikes);
        film.setLikesCount(film.getLikesCount() - 1);
        films.put(filmId, film);
    }

    public List<Film> findFilmsWithTopLikesCount(Integer count) {
        if (count == null) {
            count = maxFilmCount;
        }
        Integer finalCount = count;
        return films.values().stream()
                .sorted(Comparator.comparing(Film::getLikesCount).reversed())
                .filter(film -> film.getLikesCount() == (finalCount))
                .toList();
    }

    public void checkName(Film film) {
        if (film.getName().isEmpty()) {
            log.error("Название не указано");
            throw new ValidationException("Название должно быть указано");
        }
    }

    public void checkId(Long filmId) {
        if (filmId == null) {
            log.error("Id не указан");
            throw new ValidationException("Id должен быть указан");
        }
    }

    public void checkIdMap(Long filmId) {
        if (!films.containsKey(filmId)) {
            log.error("Фильма с этим Id не существует: {}", filmId);
            throw new ConditionsNotMetException("Фильма с таким Id не существует: " + filmId);
        }
    }

    public void checkDate(Film film) {
        if (film.getReleaseDate().isBefore(DATE_TO_CHECK)) {
            log.error("Дата релиза раньше фиксированной даты: {} .", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше " + DATE_TO_CHECK);
        }
    }

    public void checkDuration(Film film) {
        if (film.getDuration() < 0) {
            log.error("Продолжительность фильма отрицательное число: {} .", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    public void checkLength(Film film) {
        if (film.getDescription().length() > MAX_DESCR_LENGTH) {
            log.error("Максимальная длинна описания превысила максимальное количество символов: {} .",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длинна описания не должна превышать 200 символов");
        }
    }

    public void checkDoubleLike(Long filmId, Long userId) {
        if (films.get(filmId).getUserIdLikes().contains(userId)) {
            log.error("Пользователь {} пытается повторно поставить лайк", userId);
            throw new ValidationException("Пользователь " + userId + " уже поставил лайк");
        }
    }

    public void checkLikeExists(Long filmId, Long userId) {
        if (!films.get(filmId).getUserIdLikes().contains(userId)) {
            log.error("Пользователь {} не ставил лайков фильму {}", userId, filmId);
            throw new ValidationException("Пользователь " + userId + " не ставил лайк фильму: " + filmId);
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
