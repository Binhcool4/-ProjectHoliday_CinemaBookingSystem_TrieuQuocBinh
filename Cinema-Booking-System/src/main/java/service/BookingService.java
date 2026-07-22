package service;

import model.entity.Booking;
import model.entity.BookingStatus;
import model.entity.Seat;
import model.entity.Showtime;
import model.entity.Ticket;
import model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.BookingRepository;
import repository.SeatRepository;
import repository.ShowtimeRepository;
import repository.TicketRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

//    private static final BigDecimal TICKET_PRICE = new BigDecimal("75000");

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Transactional
    public void createBooking(User user, Long showtimeId, List<Long> seatIds) throws Exception {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new Exception("Vui lòng chọn ít nhất 1 ghế.");
        }

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new Exception("Khng tìm thấy suất chiếu."));

        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw new Exception("Suất chiếu đã bắt đầu hoặc đã kết thúc.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStatus(BookingStatus.CONFIRMED);
//        booking.setTotalPrice(TICKET_PRICE.multiply(BigDecimal.valueOf(seatIds.size())));
        BigDecimal showtimePrice = showtime.getPrice() != null ?
                showtime.getPrice() : new BigDecimal("75000"); // giá mặc định nếu null
        booking.setTotalPrice(showtimePrice.multiply(BigDecimal.valueOf(seatIds.size())));
        bookingRepository.save(booking);

        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new Exception("Không tìm thấy ghế"));

            if (!seat.getRoom().getId().equals(showtime.getRoom().getId())) {
                throw new Exception("Ghế không thuộc phòng chiếu của suất này.");
            }

            if (ticketRepository.existsActiveTicket(showtimeId, seatId)) {
                throw new Exception("Ghe da duoc dat boi nguoi khac. Vui long chon ghe khac.");
            }

            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setShowtime(showtime);
            ticket.setSeat(seat);
//            ticket.setPrice(TICKET_PRICE);
            ticket.setPrice(showtimePrice);
            ticketRepository.save(ticket);
        }
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryItem> getBookingHistory(Long userId) {
        return bookingRepository.findHistoryByUserId(userId).stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowtimeAvailability> getAvailableShowtimes(Long movieId) {
        return showtimeRepository.findAvailableShowtimes(movieId, LocalDateTime.now()).stream()
                .map(showtime -> {
                    long bookedSeats = ticketRepository.countActiveByShowtimeId(showtime.getId());
                    long capacity = showtime.getRoom().getCapacity();
                    long availableSeats = Math.max(0, capacity - bookedSeats);
                    return new ShowtimeAvailability(showtime, bookedSeats, availableSeats, availableSeats == 0);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Seat> getSeatsForShowtime(Long showtimeId) throws Exception {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new Exception("Khong tim thay suat chieu"));
        return seatRepository.findByRoomIdOrderBySeatNameAsc(showtime.getRoom().getId());
    }

    @Transactional(readOnly = true)
    public Showtime getShowtime(Long showtimeId) throws Exception {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new Exception("Khong tim thay suat chieu"));
    }

    @Transactional(readOnly = true)
    public List<Long> getBookedSeatIds(Long showtimeId) {
        return ticketRepository.findActiveByShowtimeId(showtimeId).stream()
                .map(ticket -> ticket.getSeat().getId())
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long expectedUserId) throws Exception {
        Booking booking = bookingRepository.findByIdWithTickets(bookingId);
        if (booking == null) {
            throw new Exception("Khong tim thay don hang");
        }

        if (!booking.getUser().getId().equals(expectedUserId)) {
            throw new Exception("Khong co quyen huy ve nay");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new Exception("Ve da duoc huy truoc do");
        }

        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            LocalDateTime startTime = booking.getTickets().get(0).getShowtime().getStartTime();
            if (!LocalDateTime.now().isBefore(startTime)) {
                throw new Exception("Khong the huy ve khi suat chieu da bat dau hoac da ket thuc");
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
    /*
    *
@Transactional
public void cancelBooking(Long bookingId, Long expectedUserId) throws Exception {
    Booking booking = bookingRepository.findByIdWithTickets(bookingId);
    if (booking == null) {
        throw new Exception("Khong tim thay don hang");
    }

    if (!booking.getUser().getId().equals(expectedUserId)) {
        throw new Exception("Khong co quyen huy ve nay");
    }
    if (booking.getStatus() == BookingStatus.CANCELLED) {
        throw new Exception("Ve da duoc huy truoc do");
    }

    if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
        LocalDateTime startTime = booking.getTickets().get(0).getShowtime().getStartTime();
        LocalDateTime twentyFourHoursBefore = startTime.minusHours(24);

        if (!LocalDateTime.now().isBefore(twentyFourHoursBefore)) {
            throw new Exception("Chi co the huy ve truoc 24 gio truoc gio chieu");
        }
    }

    booking.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(booking);
}
    * */

    private BookingHistoryItem toHistoryItem(Booking booking) {
        BookingHistoryItem item = new BookingHistoryItem();
        item.setBookingId(booking.getId());
        item.setStatus(booking.getStatus());
        item.setTotalPrice(booking.getTotalPrice());

        List<Ticket> tickets = booking.getTickets() == null ? new ArrayList<>() : booking.getTickets();
        item.setSeatNames(tickets.stream()
                .map(ticket -> ticket.getSeat().getSeatName())
                .collect(Collectors.toList()));

        if (!tickets.isEmpty()) {
            Showtime showtime = tickets.get(0).getShowtime();
            item.setMovieTitle(showtime.getMovie().getTitle());
            item.setRoomName(showtime.getRoom().getName());
            item.setShowtimeStart(showtime.getStartTime());
            item.setCancellable(booking.getStatus() != BookingStatus.CANCELLED
                    && LocalDateTime.now().isBefore(showtime.getStartTime()));
        } else {
            item.setMovieTitle("N/A");
            item.setRoomName("N/A");
            item.setCancellable(false);
        }

        return item;
    }
}
