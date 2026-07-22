package controller;

import model.entity.Movie;
import model.entity.Role;
import model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.MovieRepository;
import repository.UserRepository;
import service.BookingService;

import java.util.List;

@Controller
public class CustomerShowtimeController {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/movie/{id}/showtimes")
    public String viewShowtimes(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ma phim khong hop le"));

        model.addAttribute("movie", movie);
        model.addAttribute("showtimeList", bookingService.getAvailableShowtimes(id));
        return "customer/showtimes";
    }

    @GetMapping("/showtimes/{id}/seats")
    public String viewSeats(@PathVariable Long id, Model model) throws Exception {
        model.addAttribute("showtime", bookingService.getShowtime(id));
        model.addAttribute("seats", bookingService.getSeatsForShowtime(id));
        model.addAttribute("bookedSeatIds", bookingService.getBookedSeatIds(id));
        return "customer/seats";
    }

    @PostMapping("/booking/create")
    public String createBooking(@RequestParam Long showtimeId,
                                @RequestParam(required = false) List<Long> seatIds,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName());
        if (user == null) {
            redirectAttributes.addFlashAttribute("msgError", "Không tìm thấy user.");
            return "redirect:/login";
        }

        // Chỉ cho phép CUSTOMER đặt vé
        if (user.getRole() != null && user.getRole() != Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("msgError", "Chỉ khách hàng (customer) được phép đặt vé.");
            return "redirect:/";
        }

        try {
            bookingService.createBooking(user, showtimeId, seatIds);
            redirectAttributes.addFlashAttribute("msgSuccess", "Đặt vé thành công.");
            return "redirect:/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msgError", e.getMessage());
            return "redirect:/showtimes/" + showtimeId + "/seats";
        }
    }
}
