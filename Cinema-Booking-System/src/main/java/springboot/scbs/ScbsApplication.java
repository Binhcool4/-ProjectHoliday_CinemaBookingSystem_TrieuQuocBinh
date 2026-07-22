package springboot.scbs;

import model.entity.Genre;
import model.entity.Movie;
import model.entity.Role;
import model.entity.Room;
import model.entity.Seat;
import model.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import repository.GenreRepository;
import repository.MovieRepository;
import repository.RoomRepository;
import repository.SeatRepository;
import repository.UserRepository;

import java.time.LocalDate;

@SpringBootApplication
@EntityScan(basePackages = {"model.entity"})
@EnableJpaRepositories(basePackages = {"repository"})
@ComponentScan(basePackages = {"springboot.scbs", "config", "controller", "service"})
public class ScbsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScbsApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      GenreRepository genreRepository,
                                      RoomRepository roomRepository,
                                      MovieRepository movieRepository,
                                      SeatRepository seatRepository) {
        return args -> {
            User admin = userRepository.findByUsername("admin");
            if (admin == null) {
                admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@gmail.com");
                admin.setFullName("Admin"); // set full name để phù hợp schema
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
            } else {
                if (admin.getEmail() == null || admin.getEmail().isEmpty()) {
                    admin.setEmail("admin@gmail.com");
                }
                if (admin.getFullName() == null || admin.getFullName().isEmpty()) {
                    admin.setFullName("Admin");
                }
                userRepository.save(admin);
            }

            User staff = userRepository.findByUsername("staff");
            if (staff == null) {
                staff = new User();
                staff.setUsername("staff");
                staff.setEmail("staff@gmail.com");
                staff.setFullName("Staff");
                staff.setPassword(passwordEncoder.encode("123456"));
                staff.setRole(Role.STAFF);
                userRepository.save(staff);
            } else {
                if (staff.getEmail() == null || staff.getEmail().isEmpty()) {
                    staff.setEmail("staff@gmail.com");
                }
                if (staff.getFullName() == null || staff.getFullName().isEmpty()) {
                    staff.setFullName("Staff");
                }
                userRepository.save(staff);
            }

            if (genreRepository.count() == 0) {
                Genre g1 = new Genre(); g1.setName("Hanh dong"); genreRepository.save(g1);
                Genre g2 = new Genre(); g2.setName("Hai huoc"); genreRepository.save(g2);
                Genre g3 = new Genre(); g3.setName("Tinh cam"); genreRepository.save(g3);
                Genre g4 = new Genre(); g4.setName("Kinh di"); genreRepository.save(g4);
                Genre g5 = new Genre(); g5.setName("Vien tuong"); genreRepository.save(g5);
            }

            if (roomRepository.count() == 0) {
                Room r1 = new Room(); r1.setName("Phong 01"); r1.setCapacity(50); roomRepository.save(r1);
                Room r2 = new Room(); r2.setName("Phong 02 IMAX"); r2.setCapacity(80); roomRepository.save(r2);
                Room r3 = new Room(); r3.setName("Phong 03 3D"); r3.setCapacity(40); roomRepository.save(r3);
            }

            if (seatRepository.count() == 0) {
                for (Room room : roomRepository.findAll()) {
                    for (int i = 1; i <= room.getCapacity(); i++) {
                        Seat seat = new Seat();
                        seat.setRoom(room);
                        seat.setSeatName(toSeatName(i));
                        seatRepository.save(seat);
                    }
                }
            }

            if (movieRepository.count() == 0 && genreRepository.count() > 0) {
                Movie movie = new Movie();
                movie.setTitle("Avenger: Hoi ket");
                movie.setDescription("Phim hanh dong sieu anh hung.");
                movie.setDuration(180);
                movie.setReleaseDate(LocalDate.now());
                movie.setGenre(genreRepository.findAll().get(0));
                movieRepository.save(movie);
            }
        };
    }

    private String toSeatName(int index) {
        int zeroBased = index - 1;
        char row = (char) ('A' + (zeroBased / 10));
        int number = (zeroBased % 10) + 1;
        return row + String.valueOf(number);
    }
}
