package repository;

import model.entity.Booking;
import model.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status = :status")
    BigDecimal sumTotalPriceByStatus(@Param("status") BookingStatus status);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.tickets t
        LEFT JOIN FETCH t.showtime st
        LEFT JOIN FETCH st.movie
        LEFT JOIN FETCH st.room
        LEFT JOIN FETCH t.seat
        WHERE b.id = :bookingId
        """)
    Booking findByIdWithTickets(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.tickets t
        LEFT JOIN FETCH t.showtime st
        LEFT JOIN FETCH st.movie
        LEFT JOIN FETCH st.room
        LEFT JOIN FETCH t.seat
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findHistoryByUserId(@Param("userId") Long userId);
}

