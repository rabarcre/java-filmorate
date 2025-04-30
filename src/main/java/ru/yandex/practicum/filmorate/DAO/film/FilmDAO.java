package ru.yandex.practicum.filmorate.DAO.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.IdNotMetException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            throw new IdNotMetException("Сгенерированный Id отсутствует");
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

        String query = """
                SELECT\s
                    f.FILM_ID AS FILM_ID,
                    f.NAME AS NAME,
                    f.DESCRIPTION AS DESCRIPTION,
                    f.RELEASE_DATE AS RELEASE_DATE,
                    f.DURATION AS DURATION,
                    r.RATING_ID AS RATING_ID,
                    r.NAME AS RATING,
                    COALESCE(GROUP_CONCAT(DISTINCT g.GENRE_ID ORDER BY g.GENRE_ID), '') AS GENRES_ID,
                    COALESCE(GROUP_CONCAT(DISTINCT g.NAME ORDER BY g.GENRE_ID), '') AS GENRES
                FROM\s
                    FILMS f
                LEFT JOIN\s
                    RATINGS r ON f.RATING_ID = r.RATING_ID
                LEFT JOIN\s
                    FILM_GENRE fg ON f.FILM_ID = fg.FILM_ID
                LEFT JOIN\s
                    GENRES g ON fg.GENRE_ID = g.GENRE_ID
                WHERE f.FILM_ID = ?\s
                GROUP BY\s
                    f.FILM_ID, r.RATING_ID;""";
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
        String query = """
                SELECT\s
                    f.FILM_ID AS FILM_ID,
                    f.NAME AS NAME,
                    f.DESCRIPTION AS DESCRIPTION,
                    f.RELEASE_DATE AS RELEASE_DATE,
                    f.DURATION AS DURATION,
                    r.RATING_ID AS RATING_ID,
                    r.NAME AS RATING,
                    COALESCE(GROUP_CONCAT(DISTINCT g.GENRE_ID ORDER BY g.GENRE_ID), '') AS GENRES_ID,
                    COALESCE(GROUP_CONCAT(DISTINCT g.NAME ORDER BY g.NAME), '') AS GENRES
                FROM\s
                    FILMS f
                LEFT JOIN\s
                    RATINGS r ON f.RATING_ID = r.RATING_ID
                LEFT JOIN\s
                    FILM_GENRE fg ON f.FILM_ID = fg.FILM_ID
                LEFT JOIN\s
                    GENRES g ON fg.GENRE_ID = g.GENRE_ID
                GROUP BY\s
                    f.FILM_ID, r.RATING_ID;""";
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
        String query = """
                SELECT\s
                    f.FILM_ID AS FILM_ID,
                    f.NAME AS NAME,
                    f.DESCRIPTION AS DESCRIPTION,
                    f.RELEASE_DATE AS RELEASE_DATE,
                    f.DURATION AS DURATION,
                    r.RATING_ID AS RATING_ID,
                    r.NAME AS RATING,
                    COALESCE(GROUP_CONCAT(DISTINCT g.GENRE_ID ORDER BY g.GENRE_ID), '') AS GENRES_ID,
                    COALESCE(GROUP_CONCAT(DISTINCT g.NAME ORDER BY g.NAME), '') AS GENRES
                FROM\s
                    FILMS f
                LEFT JOIN\s
                    RATINGS r ON f.RATING_ID = r.RATING_ID
                LEFT JOIN\s
                    FILM_GENRE fg ON f.FILM_ID = fg.FILM_ID
                LEFT JOIN\s
                    GENRES g ON fg.GENRE_ID = g.GENRE_ID
                LEFT JOIN\s
                    LIKES l ON f.FILM_ID = l.FILM_ID
                GROUP BY\s
                    f.FILM_ID, r.RATING_ID
                ORDER BY\s
                   COUNT(DISTINCT l.USER_ID) DESC
                LIMIT ?;""";

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
        String existingGenresQuery = "SELECT GENRE_ID FROM FILM_GENRE WHERE FILM_ID = ?";
        List<Integer> existingGenres = jdbcTemplate.queryForList(existingGenresQuery, Integer.class, filmId);

        List<Integer> genresToAdd = genresId.stream()
                .filter(genreId -> !existingGenres.contains(genreId))
                .collect(Collectors.toList());

        if (genresToAdd.isEmpty()) {
            return;
        }

        String addQuery = "INSERT INTO FILM_GENRE (FILM_ID, GENRE_ID) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(addQuery, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, filmId);
                ps.setInt(2, genresToAdd.get(i));
            }

            @Override
            public int getBatchSize() {
                return genresToAdd.size();
            }
        });
        for (Integer genreId : genresToAdd) {
            log.info("Фильму {} добавлен жанр {}", filmId, genreId);
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


    private void checkName(Film film) {
        if (film.getName().isEmpty() || film.getName() == null) {
            log.error("Название не указано");
            throw new ValidationException("Название должно быть указано");
        }
    }

    private void checkId(Integer filmId) {
        if (filmId == null) {
            log.error("Id не указан");
            throw new ValidationException("Id должен быть указан");
        }
    }


    private void checkDate(Film film) {
        if (film.getReleaseDate() == null) {
            log.error("Дата релиза null");
            throw new ValidationException("Дата релиза не может быть null");
        }

        if (film.getReleaseDate().isBefore(DATE_TO_CHECK)) {
            log.error("Дата релиза раньше фиксированной даты: {} .", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше " + DATE_TO_CHECK);
        }
    }

    private void checkDuration(Film film) {
        if (film.getDuration() < 0) {
            log.error("Продолжительность фильма отрицательное число: {} .", film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    private void checkDescription(Film film) {
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

    private void checkMpa(Film film) {
        if (film.getMpa() != null && !mpaExists(film.getMpa().getId())) {
            log.error("MPA с Id {} не существует", film.getMpa().getId());
            throw new ConditionsNotMetException("MPA с ID " + film.getMpa().getId() + " не существует.");
        }
    }

    private boolean mpaExists(int ratingId) {
        String query = "SELECT COUNT(RATING_ID) FROM RATINGS WHERE RATING_ID = ? AND NAME IS NOT NULL";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, ratingId);
        return count != null && count > 0;
    }

    private void checkGenre(List<Integer> genresId) {
        if (genresId != null && !genresId.isEmpty()) {
            Set<Integer> existingGenres = new HashSet<>(genreExists(genresId));

            for (int id : genresId) {
                if (!existingGenres.contains(id)) {
                    log.error("Жанр с Id {} не существует", id);
                    throw new ConditionsNotMetException("Жанр с ID " + id + " не существует.");
                }
            }
        }
    }

    private List<Integer> genreExists(List<Integer> genresId) {
        String query = "SELECT GENRE_ID FROM GENRES WHERE GENRE_ID IN (" +
                String.join(",", Collections.nCopies(genresId.size(), "?")) + ")";

        Object[] params = genresId.toArray();

        List<Integer> existingGenres = jdbcTemplate.queryForList(query, params, Integer.class);
        log.debug("Существующие жанры: {}", existingGenres);
        return existingGenres;
    }

    private boolean filmExistsDb(int filmId) {
        String query = "SELECT COUNT(FILM_ID) FROM FILMS WHERE FILM_ID = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, filmId);
        return (count != null && count > 0);
    }

    private void filmExists(int filmId) {
        if (!filmExistsDb(filmId)) {
            log.error("Фильм с Id {} не существует", filmId);
            throw new ConditionsNotMetException("Фильм с ID " + filmId + " не существует.");
        }
    }

    private boolean userExistsDb(int userId) {
        String query = "SELECT COUNT(USER_ID) FROM USERS WHERE USER_ID = ?";
        Integer count = jdbcTemplate.queryForObject(query, Integer.class, userId);
        return count != null && count > 0;
    }

    private void userExist(int userId) {
        if (!userExistsDb(userId)) {
            log.error("Пользователь с Id {} не существует", userId);
            throw new ValidationException("Пользователь с ID " + userId + " не существует.");
        }
    }


}
