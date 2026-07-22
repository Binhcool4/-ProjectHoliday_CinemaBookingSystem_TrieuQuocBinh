package repository;

import model.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query("""
        SELECT COUNT(t) > 0 FROM Ticket t
        WHERE t.showtime.id = :showtimeId
          AND t.seat.id = :seatId
          AND t.booking.status <> model.entity.BookingStatus.CANCELLED
        """)
    boolean existsActiveTicket(@Param("showtimeId") Long showtimeId, @Param("seatId") Long seatId);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.showtime.id = :showtimeId
          AND t.booking.status <> model.entity.BookingStatus.CANCELLED
        """)
    List<Ticket> findActiveByShowtimeId(@Param("showtimeId") Long showtimeId);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.showtime.id = :showtimeId
          AND t.booking.status <> model.entity.BookingStatus.CANCELLED
        """)
    long countActiveByShowtimeId(@Param("showtimeId") Long showtimeId);
}

