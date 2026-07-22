package controller;
import model.entity.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import repository.UserRepository;
import model.entity.User;
import model.entity.Role;
import service.BookingHistoryItem;
import service.BookingService;
import model.entity.BookingStatus;
import repository.BookingRepository;
import repository.MovieRepository;
import repository.ShowtimeRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        return "index";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {
        try {
            // Kiểm tra mật khẩu xác nhận
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
                return "register";
            }

            // Kiểm tra email đã tồn tại
            if (userRepository.findByEmail(email) != null) {
                model.addAttribute("error", "Email đã tồn tại!");
                return "register";
            }

            // Kiểm tra username đã tồn tại (nếu muốn)
            if (userRepository.findByUsername(username) != null) {
                model.addAttribute("error", "Tên đăng nhập đã tồn tại!");
                return "register";
            }

            // Tạo user mới
            User user = new User();
            user.setEmail(email);
            user.setPhone(phone);
            user.setUsername(username);              // dùng username form
            user.setFullName(username);              // đặt full_name = username (không null)
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(Role.CUSTOMER);

            userRepository.save(user);

            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi đăng ký: " + e.getMessage());
            e.printStackTrace();
            return "register";
        }
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        long totalUsers = userRepository.count();
        long totalMovies = movieRepository.count();
        long totalBookings = bookingRepository.count();

        // totalRevenue: sum of booking.totalPrice (handle null)
        BigDecimal totalRevenue = bookingRepository.findAll().stream()
                .map(b -> b.getTotalPrice() == null ? BigDecimal.ZERO : b.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // bookings today
        LocalDate today = LocalDate.now();
        long bookingsToday = bookingRepository.findAll().stream()
                .filter(b -> {
                    if (b.getCreatedAt() == null) return false;
                    LocalDate d = b.getCreatedAt().toLocalDate();
                    return d.equals(today);
                }).count();

        // upcoming showtimes count
        long upcomingShowtimes = showtimeRepository.findAll().stream()
                .filter(s -> s.getStartTime().isAfter(LocalDateTime.now()))
                .count();

        // new users last 7 days
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        long newUsersLast7Days = userRepository.findAll().stream()
                .filter(u -> {
                    try {
                        // assuming User has createdAt or similar; if not, count by id or ignore
                        // If there's no createdAt, set newUsersLast7Days = 0
                        return false;
                    } catch (Exception ex) {
                        return false;
                    }
                }).count();

        // top movies by tickets sold (basic calculation)
        List<Map<String, Object>> topMovies = new ArrayList<>();
        // Build a simple map movieTitle -> ticketsSold
        Map<String, Long> counts = new HashMap<>();
        bookingRepository.findAll().forEach(b -> {
            if (b.getTickets() != null && !b.getTickets().isEmpty()) {
                String title = b.getTickets().get(0).getShowtime().getMovie().getTitle();
                counts.put(title, counts.getOrDefault(title, 0L) + b.getTickets().size());
            }
        });
        counts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("movieTitle", e.getKey());
                    m.put("ticketsSold", e.getValue());
                    topMovies.add(m);
                });

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalMovies", totalMovies);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalRevenue", formatVnd(totalRevenue));
        model.addAttribute("bookingsToday", bookingsToday);
        model.addAttribute("upcomingShowtimes", upcomingShowtimes);
        model.addAttribute("newUsersLast7Days", newUsersLast7Days);
        model.addAttribute("topMovies", topMovies);
        model.addAttribute("now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        return "admin/dashboard";
    }

    @GetMapping("/staff/dashboard")
    public String staffDashboard() {
        return "staff/dashboard";
    }

    private String formatVnd(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(safeAmount) + " VND";
    }

    // Thêm vào MainController
    @PostMapping("/staff/search-bookings-by-email")
    public String searchBookingsByEmail(@RequestParam String email, Model model) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            model.addAttribute("searchError", "Không tìm thấy khách hàng với email: " + email);
            return "staff/dashboard";
        }
        List<Booking> bookings = bookingRepository.findHistoryByUserId(user.getId());
        if (bookings.isEmpty()) {
            model.addAttribute("searchError", "Khách hàng " + email + " chưa có đơn đặt vé nào.");
        } else {
            model.addAttribute("searchedBookings", bookings);
            model.addAttribute("searchedUser", user);
        }
        return "staff/dashboard";
    }
    @GetMapping("/staff/user-invoices")
    public String staffUserInvoicesForm() {
        return "staff/user-invoices";
    }

    @PostMapping("/staff/user-invoices")
    public String staffUserInvoicesSearch(@RequestParam String q, Model model) {
        // q có thể là email hoặc username
        User user = userRepository.findByEmail(q);
        if (user == null) user = userRepository.findByUsername(q);
        if (user == null) {
            model.addAttribute("error", "Không tìm thấy người dùng với: " + q);
            return "staff/user-invoices";
        }
        List<BookingHistoryItem> history = bookingService.getBookingHistory(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        return "staff/user-invoices";
    }

    @GetMapping("/profile")
    public String customerProfile(Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName());
        model.addAttribute("bookings", bookingService.getBookingHistory(user.getId()));
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/booking/cancel")
    public String cancelBooking(@RequestParam Long bookingId, Authentication authentication, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName());
        try {
            bookingService.cancelBooking(bookingId, user.getId());
            redirectAttributes.addFlashAttribute("msgSuccess", "Đã hủy vé thành công và hoàn trả ghế!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msgError", e.getMessage());
        }
        return "redirect:/profile";
    }
}
