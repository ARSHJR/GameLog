package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model class for Game API data
 */
public class GameApiItem {
    @SerializedName("game_id")
    @Nullable
    private String gameId;

    @Nullable
    private String title;

    @Nullable
    private String description;

    @SerializedName("cover_image_url")
    @Nullable
    private String coverImageUrl;

    @SerializedName("release_date")
    @Nullable
    private String releaseDate;

    @Nullable
    private String developer;

    @Nullable
    private String publisher;

    @Nullable
    private String platform;

    @Nullable
    private List<String> genres;

    public GameApiItem(@Nullable String gameId, @Nullable String title, @Nullable String description,
                       @Nullable String coverImageUrl, @Nullable String releaseDate,
                       @Nullable String developer, @Nullable String publisher,
                       @Nullable String platform, @Nullable List<String> genres) {
        this.gameId = gameId;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.releaseDate = releaseDate;
        this.developer = developer;
        this.publisher = publisher;
        this.platform = platform;
        this.genres = genres;
    }

    @Nullable
    public String getGameId() {
        return gameId;
    }

    public void setGameId(@Nullable String gameId) {
        this.gameId = gameId;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(@Nullable String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    @Nullable
    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(@Nullable String releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Nullable
    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(@Nullable String developer) {
        this.developer = developer;
    }

    @Nullable
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(@Nullable String publisher) {
        this.publisher = publisher;
    }

    @Nullable
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(@Nullable String platform) {
        this.platform = platform;
    }

    @Nullable
    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(@Nullable List<String> genres) {
        this.genres = genres;
    }
}
