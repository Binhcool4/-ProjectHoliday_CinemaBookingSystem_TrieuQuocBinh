package controller;

import model.entity.Movie;
import model.entity.Room;
import model.entity.Showtime;
import repository.MovieRepository;
import repository.RoomRepository;
import repository.ShowtimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/showtimes")
public class AdminShowtimeController {

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping
    public String listShowtimes(Model model) {
        model.addAttribute("showtimes", showtimeRepository.findAll());
        return "admin/showtimes/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        Showtime showtime = new Showtime();
        showtime.setMovie(new model.entity.Movie());
        showtime.setRoom(new model.entity.Room());
        model.addAttribute("showtime", showtime);
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "admin/showtimes/form";
    }

    @PostMapping("/save")
    public String saveShowtime(@Valid @ModelAttribute("showtime") Showtime showtime, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movies", movieRepository.findAll());
            model.addAttribute("rooms", roomRepository.findAll());
            return "admin/showtimes/form";
        }

        Movie movie = movieRepository.findById(showtime.getMovie().getId()).orElseThrow();
        LocalDateTime newStart = showtime.getStartTime();
        LocalDateTime newEnd = newStart.plusMinutes(movie.getDuration()).plusMinutes(15); // 15 mins for cleanup

        LocalDateTime startOfDay = newStart.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        List<Showtime> existingShowtimes = showtimeRepository.findByRoomIdAndDateRange(showtime.getRoom().getId(), startOfDay, endOfDay);

        for (Showtime existing : existingShowtimes) {
            if (existing.getId() != null && existing.getId().equals(showtime.getId())) continue;

            LocalDateTime existStart = existing.getStartTime();
            LocalDateTime existEnd = existStart.plusMinutes(existing.getMovie().getDuration()).plusMinutes(15);

            if (newStart.isBefore(existEnd) && newEnd.isAfter(existStart)) {
                redirectAttributes.addFlashAttribute("error", "Lịch chiếu trùng lặp với phim: " + existing.getMovie().getTitle() + " (" + existStart + ")");
                return "redirect:/admin/showtimes/create";
            }
        }

        showtimeRepository.save(showtime);
        redirectAttributes.addFlashAttribute("success", "Thêm suất chiếu thành công!");
        return "redirect:/admin/showtimes";
    }
}
