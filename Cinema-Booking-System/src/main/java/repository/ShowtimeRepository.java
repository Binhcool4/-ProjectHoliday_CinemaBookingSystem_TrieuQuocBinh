package repository;

import model.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    long countByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT s FROM Showtime s WHERE s.room.id = :roomId AND s.startTime >= :startOfDay AND s.startTime <= :endOfDay")
    List<Showtime> findByRoomIdAndDateRange(@Param("roomId") Long roomId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT s FROM Showtime s WHERE s.movie.id = :movieId AND s.startTime > :currentTime ORDER BY s.startTime ASC")
    List<Showtime> findAvailableShowtimes(@Param("movieId") Long movieId, @Param("currentTime") LocalDateTime currentTime);
}
