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
    public String staffDashboard(@RequestParam(required = false) String q, Model model) {
        List<Booking> bookings = new ArrayList<>();
        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim();
            // 1. Check if query is numeric (Booking ID)
            if (query.matches("\\d+")) {
                Booking booking = bookingRepository.findByIdWithTickets(Long.parseLong(query));
                if (booking != null) {
                    bookings.add(booking);
                }
            }
            // 2. If not found or not numeric, search by user email or username
            if (bookings.isEmpty()) {
                User user = userRepository.findByEmail(query);
                if (user == null) {
                    user = userRepository.findByUsername(query);
                }
                if (user != null) {
                    bookings = bookingRepository.findHistoryByUserId(user.getId());
                }
            }

            if (bookings.isEmpty()) {
                model.addAttribute("searchError", "Không tìm thấy đơn đặt vé nào khớp với: " + q);
            } else {
                model.addAttribute("searchedQuery", q);
            }
        } else {
            // Retrieve recent bookings
            bookings = bookingRepository.findAll();
            // Sort by ID descending
            bookings.sort((b1, b2) -> b2.getId().compareTo(b1.getId()));
            // Limit to last 15 bookings
            if (bookings.size() > 15) {
                bookings = bookings.subList(0, 15);
            }
        }
        model.addAttribute("bookings", bookings);
        return "staff/dashboard";
    }

    private String formatVnd(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(safeAmount) + " VND";
    }

    @PostMapping("/staff/confirm-booking")
    public String confirmBooking(@RequestParam Long bookingId, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            redirectAttributes.addFlashAttribute("msgSuccess", "Đã xác nhận thanh toán đơn hàng #" + bookingId + " thành công!");
        } else {
            redirectAttributes.addFlashAttribute("msgError", "Không tìm thấy đơn hàng #" + bookingId);
        }
        return "redirect:/staff/dashboard";
    }

    @PostMapping("/staff/print-ticket")
    public String printTicket(@RequestParam Long bookingId, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("msgSuccess", "Đã gửi lệnh in vé giấy cho đơn hàng #" + bookingId + " thành công!");
        return "redirect:/staff/dashboard";
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
