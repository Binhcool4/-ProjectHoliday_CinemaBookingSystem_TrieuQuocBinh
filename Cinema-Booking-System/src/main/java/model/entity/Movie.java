package model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "movies")
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Không được để trống tên phim")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Vui lòng nhập mô tả ngắn cho phim")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Không được để trống thời lượng")
    @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0 phút")
    @Column(nullable = false)
    private Integer duration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @NotNull(message = "Vui lòng chọn ngày phát hành")
    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "poster_url", length = 255)
    private String posterUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
}
