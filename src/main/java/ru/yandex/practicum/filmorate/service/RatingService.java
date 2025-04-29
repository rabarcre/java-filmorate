package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.DAO.rating.RatingDao;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.Rating;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingDao ratingDao;

    public List<Rating> getAllRatings() {
        return ratingDao.getAllRatings();
    }

    public Rating getRatingById(int ratingId) {
        return ratingDao.getRatingById(ratingId)
                .orElseThrow(() -> new ConditionsNotMetException("Рейтинга с Id " + ratingId + " не существует"));
    }
}
