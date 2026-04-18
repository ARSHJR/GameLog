package com.example.gamelog;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Interface defining the API endpoints
 */
public interface ApiService {
    @GET("/games")
    Call<List<GameApiItem>> getGames();

    @GET("/games/{id}")
    Call<GameApiItem> getGameById(@Path("id") String gameId);

    @POST("/users/{userId}/collection")
    Call<AddToCollectionResponse> addToCollection(
            @Path("userId") String userId,
            @Body AddToCollectionRequest request
    );

    @POST("/users/{userId}/favourites/{gameId}/toggle")
    Call<FavouriteToggleResponse> toggleFavourite(
            @Path("userId") String userId,
            @Path("gameId") String gameId
    );

    @GET("/users/{userId}/favourites")
    Call<List<FavouriteGameItem>> getFavourites(@Path("userId") String userId);

    @GET("/users/{userId}/collection")
    Call<List<CollectionEntryItem>> getCollection(@Path("userId") String userId);

    @GET("/users/{userId}/profile")
    Call<UserProfileResponse> getUserProfile(@Path("userId") String userId);

    @GET("/users/resolve")
    Call<ResolvedBackendUser> resolveBackendUser(
            @Query("authUserId") String authUserId,
            @Query("email") String email,
            @Query("displayName") String displayName
    );

    @GET("/users/{userId}/activity")
    Call<List<UserActivityItem>> getUserActivity(
            @Path("userId") String userId,
            @Query("limit") Integer limit
    );

    @POST("/users/{userId}/activity")
    Call<UserActivityItem> createUserActivity(
            @Path("userId") String userId,
            @Body CreateUserActivityRequest request
    );

    @GET("/users/{userId}/notes")
    Call<List<CollectionNoteItem>> getUserNotes(
            @Path("userId") String userId,
            @Query("type") String type
    );

    @GET("/collection/{userGameId}/notes")
    Call<List<CollectionNoteItem>> getCollectionNotes(
            @Path("userGameId") String userGameId,
            @Query("type") String type
    );

    @POST("/collection/{userGameId}/notes")
    Call<CollectionNoteItem> createCollectionNote(
            @Path("userGameId") String userGameId,
            @Body CreateCollectionNoteRequest request
    );

    @PATCH("/notes/{noteId}/pin")
    Call<CollectionNoteItem> toggleNotePin(@Path("noteId") String noteId);

    @PATCH("/notes/{noteId}/task-status")
    Call<CollectionNoteItem> updateReminderTaskStatus(
            @Path("noteId") String noteId,
            @Body UpdateTaskStatusRequest request
    );

        @DELETE("/notes/{noteId}")
        Call<CollectionNoteItem> deleteNote(@Path("noteId") String noteId);
}
