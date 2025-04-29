package ru.yandex.practicum.filmorate.DAO.rating;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Rating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RatingDao {
    private final JdbcTemplate jdbcTemplate;

    public List<Rating> getAllRatings() {
        String query = "SELECT * FROM RATINGS ORDER BY RATING_ID";
        log.info("Получение всех рейтингов");
        return jdbcTemplate.query(query, this::mapToRating);
    }

    public Optional<Rating> getRatingById(int id) {
        String query = "SELECT * FROM RATINGS WHERE RATING_ID = ?";
        log.info("Получение рейтинга с Id {}", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(query, this::mapToRating, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private Rating mapToRating(ResultSet resultSet, int rowNum) throws SQLException {
        Rating rating = new Rating();
        rating.setId(resultSet.getInt("RATING_ID"));
        rating.setName(resultSet.getString("NAME"));
        return rating;
    }
}
