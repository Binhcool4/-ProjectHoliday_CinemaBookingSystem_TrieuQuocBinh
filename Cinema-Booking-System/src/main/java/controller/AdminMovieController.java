package controller;

import model.entity.Movie;
import repository.GenreRepository;
import repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @GetMapping
    public String listMovies(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        return "admin/movies/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        Movie movie = new Movie();
        movie.setGenre(new model.entity.Genre()); // Ngăn lỗi NullPointerException của Thymeleaf khi binding genre.id
        model.addAttribute("movie", movie);
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/movies/form";
    }

    @PostMapping("/save")
    public String saveMovie(@Valid @ModelAttribute("movie") Movie movie, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreRepository.findAll());
            return "admin/movies/form";
        }
        movieRepository.save(movie);
        return "redirect:/admin/movies";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id).orElseThrow();
        model.addAttribute("movie", movie);
        model.addAttribute("genres", genreRepository.findAll());
        return "admin/movies/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id) {
        movieRepository.deleteById(id);
        return "redirect:/admin/movies";
    }
}
