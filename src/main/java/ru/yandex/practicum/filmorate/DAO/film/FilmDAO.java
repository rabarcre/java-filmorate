package ru.yandex.practicum.filmorate.DAO.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Rating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FilmDAO {
    private final JdbcTemplate jdbcTemplate;

    private static final LocalDate DATE_TO_CHECK = LocalDate.of(1895, 12, 28);
    private static final Integer MAX_DESCR_LENGTH = 200;

    public Film addFilm(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        checkName(film);
        checkDate(film);
        checkDuration(film);
        checkDescription(film);
        checkMpa(film);

        List<Integer> genresId;
        if (film.getGenres() != null) {
            genresId = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toList());
        } else {
            genresId = Collections.emptyList();
        }
        checkGenre(genresId);

        String query = "INSERT INTO FILMS (NAME, DESCRIPTION, RELEASE_DATE, DURATION, RATING_ID) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, film.getName());
            preparedStatement.setString(2, film.getDescription());
            preparedStatement.setDate(3, Date.valueOf(film.getReleaseDate()));
            preparedStatement.setInt(4, film.getDuration());
            preparedStatement.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null, Types.INTEGER);
            return preparedStatement;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new RuntimeException("Сгенерированный Id отсутствует");
        }
        Integer filmId = keyHolder.getKey().intValue();

        if (!genresId.isEmpty()) {
            addGenres(filmId, genresId);
        }

        film.setId(filmId);
        log.info("Добавлен фильм: {}, {}", film.getId(), film.getName());
        return film;
    }

    public Film updateFilm(Film film) {
        checkId(film.getId());
        filmExists(film.getId());

        String query = "UPDATE FILMS SET NAME = ?, DESCRIPTION = ?, RELEASE_DATE = ?, DURATION = ?, RATING_ID = ? " +
                "WHERE FILM_ID = ?";
        jdbcTemplate.update(query, film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        List<Genre> genres = film.getGenres();
        if (genres != null && !genres.isEmpty()) {
            deleteGenres(film.getId());
            addGenres(film.getId(), genres.stream()
                    .map(Genre::getId)
                    .toList());
        }
        log.info("Обновлён фильм: {}, {}", film.getId(), film.getName());
        return getFilmById(film.getId());
    }

    public Film getFilmById(Integer id) {
        checkId(id);

        String query = "SELECT \n" +
                "    f.FILM_ID AS FILM_ID,\n" +
                "    f.NAME AS NAME,\n" +
                "    f.DESCRIPTION AS DESCRIPTION,\n" +
                "    f.RELEASE_DATE AS RELEASE_DATE,\n" +
                "    f.DURATION AS DURATION,\n" +
                "    r.RATING_ID AS RATING_ID,\n" +
                "    r.NAME AS RATING,\n" +
                "    COALESCE(GROUP_CONCAT(DISTINCT g.GENRE_ID ORDER BY g.GENRE_ID), '') AS GENRES_ID,\n" +
                "    COALESCE(GROUP_CONCAT(DISTINCT g.NAME ORDER BY g.GENRE_ID), '') AS GENRES\n" +
                "FROM \n" +
                "    FILMS f\n" +
                "LEFT JOIN \n" +
                "    RATINGS r ON f.RATING_ID = r.RATING_ID\n" +
                "LEFT JOIN \n" +
                "    FILM_GENRE fg ON f.FILM_ID = fg.FILM_ID\n" +
                "LEFT JOIN \n" +
                "    GENRES g ON fg.GENRE_ID = g.GENRE_ID\n" +
                "WHERE f.FILM_ID = ? \n" +
                "GROUP BY \n" +
                "    f.FILM_ID, r.RATING_ID;";
        try {
            return jdbcTemplate.queryForObject(query, (resultSet, rowNum) -> {
                Film film = new Film();
                film.setId(resultSet.getInt("FILM_ID"));
                film.setName(resultSet.getString("NAME"));
                film.setDescription(resultSet.getString("DESCRIPTION"));
                film.setReleaseDate(resultSet.getDate("RELEASE_DATE").toLocalDate());
                film.setDuration(resultSet.getInt("DURATION"));

                Integer ratingId = resultSet.getObject("RATING_ID") != null ?
                        resultSet.getInt("RATING_ID") : null;
                if (ratingId != null) {
                    Rating rating = new Rating();
                    rating.setId(ratingId);
                    rating.setName(resultSet.getString("RATING"));
                    film.setMpa(rating);
                } else {
                    film.setMpa(null);
                }

                String genesId = resultSet.getString("GENRES_ID");
                String genresName = resultSet.getString("GENRES");
                List<Genre> genres = new ArrayList<>();

                if (!genesId.isEmpty() && !genresName.isEmpty()) {
                    String[] genreId = genesId.split(",");
                    String[] genreName = genresName.split(",");

                    for (int i = 0; i < genreId.length; i++) {
                        Genre genre = new Genre();
                        genre.setId(Integer.parseInt(genreId[i]));
                        genre.setName(genreName[i]);
                        genres.add(genre);
                    }
                }
                film.setGenres(genres);

                return film;
            }, id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Фильм с Id {} не существует", id);
            throw new ConditionsNotMetException("Фильм с ID " + id + " не существует.");
        }
    }

    public List<Film> findAllFilms() {
        String query = "SELECT \n" +
                "    f.FILM_ID AS FILM_ID,\n" +
                "    f.NAME AS NAME,\n" +
                "    f.DESCRIPTION AS DESCRIPTION,\n" +
                "    f.RELEASE_DATE AS RELEASE_DATE,\n" +
                "    f.DURATION AS DURATION,\n" +
                "    r.RATING_ID AS RATING_ID,\n" +
                "    r.NAME AS RATING,\n" +
                "    COALESCE(GROUP_CONCAT(DISTINCT g.GENRE_ID ORDER BY g.GENRE_ID), '') AS GENRES_ID,\n" +
                "    COALESCE(GROUP_CONCAT(DISTINCT g.NAME ORDER BY g.NAME), '') AS GENRES\n" +
                "FROM \n" +
                "    FILMS f\n" +
                "LEFT JOIN \n" +
                "    RATINGS r ON f.RATING_ID = r.RATING_ID\n" +
                "LEFT JOIN \n" +
                "    FILM_GENRE fg ON f.FILM_ID = fg.FILM_ID\n" +
                "LEFT JOIN \n" +
                "    GENRES g ON fg.GENRE_ID = g.GENRE_ID\n" +
                "GROUP BY \n" +
                "    f.FILM_ID, r.RATING_ID;";
        return jdbcTemplate.query(query, (resultSet, rowNum) -> {
            Film film = new Film();
            film.setId(resultSet.getInt("FILM_ID"));
            film.setName(resultSet.getString("NAME"));
            film.setDescription(resultSet.getString("DESCRIPTION"));
            film.setReleaseDate(resultSet.getDate("RELEASE_DATE").toLocalDate());
            film.setDuration(resultSet.getInt("DURATION"));

            Integer ratingId = resultSet.getObject("RATING_ID") != null ?
                    resultSet.getInt("RATING_ID") : null;
            if (ratingId != null) {
                Rating rating = new Rating();
                rating.setId(ratingId);
                film.setMpa(rating);
            } else {
                film.setMpa(null);
            }

            String genesId = resultSet.getString("GENRES_ID");
            String genresName = resultSet.getString("GENRES");
            List<Genre> genres = new ArrayList<>();

            if (!genesId.isEmpty() && !genresName.isEmpty()) {
                String[] genreId = genesId.split(",");
                String[] genreName = genresName.split(",");

                for (int i = 0; i < genreId.length; i++) {
                    Genre genre = new Genre();
                    genre.setId(Integer.parseInt(genreId[i]));
                    genre.setName(genreName[i]);
                    genres.add(genre);
                }
            }
            film.setGenres(genres);

            return film;
        });
    }

    public List<Film> getPopularFilms(int count) {
        String query = "SELECT \n" +
                "    f.FILM_ID AS FILM_ID,\n" +
                "    f.NAME AS NAME,\n" +
                "    f.DESCRIPTION AS DESCRIPTION,\n" +
                "    f.RELEASE_DATE AS RELEASE_DATE,\n" +
                "    f.DURATION AS DURATION,\n" +
                "    r.RATING_ID AS RATING_ID,\n" +
                "    r.NAME AS RATING,\n" +
                "    COALESCE(GROUP_CONCAT(DISTINCT g.GENRE_ID ORDER BY g.GENRE_ID), '') AS GENRES_ID,\n" +
                "    COALESCE(GROUP_CONCAT(DISTINCT g.NAME ORDER BY g.NAME), '') AS GENRES\n" +
                "FROM \n" +
                "    FILMS f\n" +
                "LEFT JOIN \n" +
                "    RATINGS r ON f.RATING_ID = r.RATING_ID\n" +
                "LEFT JOIN \n" +
                "    FILM_GENRE fg ON f.FILM_ID = fg.FILM_ID\n" +
                "LEFT JOIN \n" +
                "    GENRES g ON fg.GENRE_ID = g.GENRE_ID\n" +
                "LEFT JOIN \n" +
                "    LIKES l ON f.FILM_ID = l.FILM_ID\n" +
                "GROUP BY \n" +
                "    f.FILM_ID, r.RATING_ID\n" +
                "ORDER BY \n" +
                "   COUNT(DISTINCT l.USER_ID) DESC\n" +
                "LIMIT ?;";

        return jdbcTemplate.query(query, (resultSet, rowNum) -> {
            Film film = new Film();
            film.setId(resultSet.getInt("FILM_ID"));
            film.setName(resultSet.getString("NAME"));
            film.setDescription(resultSet.getString("DESCRIPTION"));
            film.setReleaseDate(resultSet.getDate("RELEASE_DATE").toLocalDate());
            film.setDuration(resultSet.getInt("DURATION"));

            Integer ratingId = resultSet.getObject("RATING_ID") != null ?
                    resultSet.getInt("RATING_ID") : null;
            if (ratingId != null) {
                Rating rating = new Rating();
                rating.setId(ratingId);
                film.setMpa(rating);
            } else {
                film.setMpa(null);
            }

            String genesId = resultSet.getString("GENRES_ID");
            String genresName = resultSet.getString("GENRES");
            List<Genre> genres = new ArrayList<>();

            if (!genesId.isEmpty() && !genresName.isEmpty()) {
                String[] genreId = genesId.split(",");
                String[] genreName = genresName.split(",");

                for (int i = 0; i < genreId.length; i++) {
                    Genre genre = new Genre();
                    genre.setId(Integer.parseInt(genreId[i]));
                    genre.setName(genreName[i]);
                    genres.add(genre);
                }
            }
            film.setGenres(genres);

            return film;
        }, count);
    }

    public void removeFilm(Integer filmId) {
        filmExists(filmId);

        String query = "DELETE FROM FILMS WHERE FILM_ID = ?";
        jdbcTemplate.update(query, filmId);
        log.info("Фильм {}, удалён", filmId);
    }

    public void addLike(Integer filmId, Integer userId) {
        filmExists(filmId);
        userExist(userId);
        String query = "INSERT INTO LIKES (FILM_ID, USER_ID) VALUES (?, ?)";
        jdbcTemplate.update(query, filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void deleteLike(Integer filmId, Integer userId) {
        filmExists(filmId);
        userExist(userId);
        String query = "DELETE FROM LIKES WHERE FILM_ID = ? AND USER_ID = ?";
        jdbcTemplate.update(query, filmId, userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public void addGenres(Integer filmId, List<Integer> genresId) {
        String checkQuery = "SELECT COUNT(*) FROM FILM_GENRE WHERE FILM_ID = ? AND GENRE_ID = ?";
        String addQuery = "INSERT INTO FILM_GENRE (FILM_ID, GENRE_ID) VALUES (?, ?)";

        for (Integer genreId : genresId) {
            Integer count = jdbcTemplate.queryForObject(checkQuery, Integer.class, filmId, genreId);
            if (count == null || count == 0) {
                jdbcTemplate.update(addQuery, filmId, genreId);
                log.info("Фильму {} добавлен жанр {}", filmId, genreId);
            }
        }
    }

    public void deleteGenres(Integer filmId) {
        String query = "DELETE FROM FILM_GENRES WHERE FILM_ID = ?";
        jdbcTemplate.update(query, filmId);
    }

    public List<Genre> getGenresByFilmId(Integer filmId) {
        filmExists(filmId);
        String query = "SELECT g.GENRES_ID, g.NAME" +
                "FROM GENRES g" +
                "JOIN FILM_GENRE fg ON g.GENRE_ID = fg.GENRE_ID" +
                "WHERE fg.FILM_ID = ?";

        try {
            return jdbcTemplate.query(query, this::mapToGenre, filmId);
        } catch (EmptyResultDataAccessException e) {
            return List.of();
        }
    }

    public Optional<Object> getRatingByFilmId(Integer filmId) {
        String query = "SELECT r.RATING_ID, r.NAME" +
                "FROM RATINGS r" +
                "WHERE r.RATING_ID = (SELECT RATING_ID FROM FILMS WHERE FILM_ID = ?)";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(query, this::mapToRating, filmId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Rating mapToRating(ResultSet resultSet, Integer rowNum) throws SQLException {
        Rating rating = new Rating();
        rating.setId(resultSet.getInt("RATING_ID"));
        rating.setName(resultSet.getString("NAME"));
        return rating;
    }

    private Genre mapToGenre(ResultSet resultSet, Integer rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(resultSet.getInt("GENRE_ID"));
        genre.setName(resultSet.getString("NANE"));
        return genre;
    }


    public void checkName(Film film) {
        if (film.getName().isEmpty() || film.getName() == null) {
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


    public void checkDate(Film film) {
        if (film.getReleaseDate() == null) {
            log.error("Дата релиза null");
            throw new ValidationException("Дата релиза не может быть null");
        }

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

    public void checkDescription(Film film) {
        if (film.getDescription() == null) {
            log.error("Описание фильма null");
            throw new ValidationException("Описание фильма не может быть null");
        }
        if (film.getDescription().length() > MAX_DESCR_LENGTH) {
            log.error("Максимальная длинна описания превысила максимальное количество символов: {} .",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длинна описания не должна превышать 200 символов");
        }
    }

    public void checkMpa(Film film) {
        if (film.getMpa() != null && !mpaExists(film.getMpa().getId())) {
            log.error("MPA с Id {} не существует", film.getMpa().getId());
            throw new ConditionsNotMetException("MPA с ID " + film.getMpa().getId() + " не существует.");
        }
    }

    public boolean mpaExists(int ratingId) {
        String query = "SELECT COUNT(*) FROM RATINGS WHERE RATING_ID = ? AND name IS NOT NULL";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, ratingId);
        return count != null && count > 0;
    }

    public void checkGenre(List<Integer> genresId) {
        if (!genresId.isEmpty()) {
            for (int id : genresId) {
                if (!genreExists(id)) {
                    log.error("Жанр с Id {} не существует", id);
                    throw new ConditionsNotMetException("Жанр с ID " + id + " не существует.");
                }
            }
        }
    }

    public boolean genreExists(int genreId) {
        String query = "SELECT COUNT(*) FROM GENRES WHERE GENRE_ID = ?";
        Long count = jdbcTemplate.queryForObject(query, Long.class, genreId);
        log.debug("Проверка существования жанра с ID {}: count = {}", genreId, count);
        return count != null && count > 0;
    }

    public boolean filmExistsDb(int filmId) {
        String query = "SELECT COUNT(*) FROM FILMS WHERE FILM_ID = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, filmId);
        return (count != null && count > 0);
    }

    public void filmExists(int filmId) {
        if (!filmExistsDb(filmId)) {
            log.error("Фильм с Id {} не существует", filmId);
            throw new ConditionsNotMetException("Фильм с ID " + filmId + " не существует.");
        }
    }

    private boolean userExistsDb(int userId) {
        String query = "SELECT COUNT(*) FROM USERS WHERE USER_ID = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, userId);
        return count != null && count > 0;
    }

    public void userExist(int userId) {
        if (!userExistsDb(userId)) {
            log.error("Пользователь с Id {} не существует", userId);
            throw new ValidationException("Пользователь с ID " + userId + " не существует.");
        }
    }


}
