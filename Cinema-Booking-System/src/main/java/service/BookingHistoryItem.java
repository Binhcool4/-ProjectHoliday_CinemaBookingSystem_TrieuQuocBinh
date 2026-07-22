package service;

import model.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingHistoryItem {
    private Long bookingId;
    private String movieTitle;
    private String roomName;
    private LocalDateTime showtimeStart;
    private List<String> seatNames;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private boolean cancellable;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public LocalDateTime getShowtimeStart() { return showtimeStart; }
    public void setShowtimeStart(LocalDateTime showtimeStart) { this.showtimeStart = showtimeStart; }
    public List<String> getSeatNames() { return seatNames; }
    public void setSeatNames(List<String> seatNames) { this.seatNames = seatNames; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public boolean isCancellable() { return cancellable; }
    public void setCancellable(boolean cancellable) { this.cancellable = cancellable; }
}
