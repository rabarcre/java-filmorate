package ru.yandex.practicum.filmorate.DAO.genre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GenreDao {
    private final JdbcTemplate jdbcTemplate;

    public List<Genre> getAllGenres() {
        String query = "SELECT * FROM GENRES ORDER BY GENRE_ID";
        log.info("Получение всех жанров");
        return jdbcTemplate.query(query, this::mapToGenre);
    }

    public Optional<Genre> getGenreById(Integer id) {
        String query = "SELECT * FROM GENRES WHERE GENRE_ID = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(query, this::mapToGenre, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Genre mapToGenre(ResultSet resultSet, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(resultSet.getInt("GENRE_ID"));
        genre.setName(resultSet.getString("NAME"));
        return genre;
    }
}
