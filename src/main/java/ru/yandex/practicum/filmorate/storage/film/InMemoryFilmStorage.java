package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

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
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new HashMap<>();
    private final InMemoryUserStorage inMemoryUserStorage;

    private static final LocalDate DATE_TO_CHECK = LocalDate.of(1895, 12, 28);
    private static final Integer MAX_DESCR_LENGTH = 200;
    private int currentId = 0;

    @Override
    public Collection<Film> findAllFilms() {
        return films.values();
    }

    @Override
    public Film addFilm(Film film) {
        validateFilm(film);
        film.setId(++currentId);
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}, {}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film updFilm) {
        checkId(updFilm.getId());
        checkIdMap(updFilm.getId());
        validateFilm(updFilm);

        films.put(updFilm.getId(), updFilm);
        log.info("Обновлён фильм: {}, {}", updFilm.getId(), updFilm.getName());
        return updFilm;
    }

    @Override
    public Film getFilmById(int filmId) {
        checkId(filmId);
        checkIdMap(filmId);
        return films.get(filmId);
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        checkId(filmId);
        checkIdMap(filmId);
        validateUser(userId);

        Film film = films.get(filmId);
        film.addLike(userId);
        film.setLikesCount(film.getLikesCount() + 1);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    @Override
    public void deleteLike(Integer filmId, Integer userId) {
        checkId(filmId);
        checkIdMap(filmId);
        validateUser(userId);

        Film film = films.get(filmId);
        film.removeLike(userId);
        film.setLikesCount(Math.max(0, film.getLikesCount() - 1));
    }


    @Override
    public List<Film> getPopularFilms(int count) {
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

    private void validateUser(Integer userId) {
        if (!inMemoryUserStorage.userExists(userId)) {
            log.error("Пользователя с Id {} не существует", userId);
            throw new ConditionsNotMetException("Пользователя с таким Id не существует: " + userId);
        }
    }

    private void validateFilm(Film film) {
        checkName(film);
        checkDate(film);
        checkDuration(film);
        checkLength(film);
    }

    public void checkName(Film film) {
        if (film.getName().isEmpty()) {
            log.error("Название не указано");
            throw new ValidationException("Название должно быть указано");
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

}