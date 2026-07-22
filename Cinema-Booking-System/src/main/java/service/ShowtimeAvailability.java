package service;

import model.entity.Showtime;

public class ShowtimeAvailability {
    private Showtime showtime;
    private long bookedSeats;
    private long availableSeats;
    private boolean soldOut;

    public ShowtimeAvailability(Showtime showtime, long bookedSeats, long availableSeats, boolean soldOut) {
        this.showtime = showtime;
        this.bookedSeats = bookedSeats;
        this.availableSeats = availableSeats;
        this.soldOut = soldOut;
    }

    public Showtime getShowtime() { return showtime; }
    public long getBookedSeats() { return bookedSeats; }
    public long getAvailableSeats() { return availableSeats; }
    public boolean isSoldOut() { return soldOut; }
}
