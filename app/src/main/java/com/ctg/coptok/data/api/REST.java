package com.ctg.coptok.data.api;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import com.ctg.coptok.data.models.Advertisement;
import com.ctg.coptok.data.models.Article;
import com.ctg.coptok.data.models.ArticleSection;
import com.ctg.coptok.data.models.Balance;
import com.ctg.coptok.data.models.Challenge;
import com.ctg.coptok.data.models.Clip;
import com.ctg.coptok.data.models.ClipSection;
import com.ctg.coptok.data.models.Comment;
import com.ctg.coptok.data.models.Credit;
import com.ctg.coptok.data.models.Exists;
import com.ctg.coptok.data.models.Gift;
import com.ctg.coptok.data.models.Hashtag;
import com.ctg.coptok.data.models.Item;
import com.ctg.coptok.data.models.LiveStream;
import com.ctg.coptok.data.models.LiveStreamAgora;
import com.ctg.coptok.data.models.Message;
import com.ctg.coptok.data.models.Notification;
import com.ctg.coptok.data.models.Promotion;
import com.ctg.coptok.data.models.Redirect;
import com.ctg.coptok.data.models.Redemption;
import com.ctg.coptok.data.models.Song;
import com.ctg.coptok.data.models.SongSection;
import com.ctg.coptok.data.models.Sticker;
import com.ctg.coptok.data.models.StickerSection;
import com.ctg.coptok.data.models.Thread;
import com.ctg.coptok.data.models.Token;
import com.ctg.coptok.data.models.UnreadNotifications;
import com.ctg.coptok.data.models.User;
import com.ctg.coptok.data.models.Wrappers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface REST {

    @GET("advertisements")
    Call<Wrappers.Paginated<Advertisement>> advertisementsIndex(@Query("page") int page);

    @GET("articles")
    Call<Wrappers.Paginated<Article>> articlesIndex(
            @Query("q") @Nullable String q,
            @Query("sections[]") @Nullable Iterable<Integer> sections,
            @Query("page") int page,
            @Query("count") int count
    );

    @GET("articles/sections")
    Call<Wrappers.Paginated<ArticleSection>> articleSectionsIndex(
            @Query("q") @Nullable String q,
            @Query("page") int page
    );

    @GET("articles/sections/{id}")
    Call<Wrappers.Single<ArticleSection>> articleSectionsShow(@Path("id") int section);

    @GET("articles/{id}")
    Call<Wrappers.Single<Article>> articlesShow(@Path("id") int article);

    @POST("users/{id}/blocked")
    Call<ResponseBody> blockedBlock(@Path("id") int user);

    @DELETE("users/{id}/blocked")
    Call<ResponseBody> blockedUnblock(@Path("id") int user);

    @GET("challenges")
    Call<Wrappers.Paginated<Challenge>> challengesIndex();

    @Headers("Accept: application/json")
    @Multipart
    @POST("clips")
    Call<Wrappers.Single<Clip>> clipsCreate(
            @Part MultipartBody.Part video,
            @Part MultipartBody.Part screenshot,
            @Part MultipartBody.Part preview,
            @Part("song") @Nullable RequestBody song,
            @Part("description") @Nullable RequestBody description,
            @Part("language") RequestBody language,
            @Part("private") RequestBody _private,
            @Part("comments") RequestBody comments,
            @Part("duet") RequestBody duet,
            @Part("duration") RequestBody duration,
            @Part("cta_label") @Nullable RequestBody ctaLabel,
            @Part("cta_link") @Nullable RequestBody ctaLink,
            @Part("location") RequestBody location,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @PUT("clips/{id}")
    Call<Wrappers.Single<Clip>> clipsUpdate(
            @Path("id") int clip,
            @Field("description") String description,
            @Field("language") String language,
            @Field("private") int _private,
            @Field("comments") int comments,
            @Field("duet") int duet,
            @Field("cta_label") String ctaLabel,
            @Field("cta_link") String ctaLink,
            @Field("location") String location,
            @Field("latitude") Double latitude,
            @Field("longitude") Double longitude
    );

    @DELETE("clips/{id}")
    Call<ResponseBody> clipsDelete(@Path("id") int clip);

    @GET("clips")
    Call<Wrappers.Paginated<Clip>> clipsIndex(
            @Query("mine") @Nullable Boolean mine,
            @Query("q") @Nullable String q,
            @Query("liked") @Nullable Boolean liked,
            @Query("saved") @Nullable Boolean saved,
            @Query("following") @Nullable Boolean following,
            @Query("user") @Nullable Integer user,
            @Query("song") @Nullable Integer song,
            @Query("languages[]") @Nullable Iterable<String> languages,
            @Query("sections[]") @Nullable Iterable<Integer> sections,
            @Query("hashtags") @Nullable Iterable<String> hashtags,
            @Query("seed") @Nullable Integer seed,
            @Query("seen") @Nullable Long seen,
            @Query("first") @Nullable Integer first,
            @Query("before") @Nullable Integer before,
            @Query("after") @Nullable Integer after,
            @Query("page") @Nullable Integer page,
            @Query("count") @Nullable Integer count
    );

    @PATCH("clips/{id}")
    Call<ResponseBody> clipsTouch(@Path("id") int clip);

    @GET("clips/sections")
    Call<Wrappers.Paginated<ClipSection>> clipSectionsIndex(
            @Query("q") String q,
            @Query("page") int page
    );

    @GET("clips/sections/{id}")
    Call<Wrappers.Single<ClipSection>> clipSectionsShow(@Path("id") int section);

    @Headers("Accept: application/json")
    @GET("clips/{id}")
    Call<Wrappers.Single<Clip>> clipsShow(@Path("id") int clip);

    @GET("clips/{id}/comments")
    Call<Wrappers.Paginated<Comment>> commentsIndex(
            @Path("id") int clip,
            @Query("page") int page
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("clips/{id}/comments")
    Call<Wrappers.Single<Comment>> commentsCreate(
            @Path("id") int clip,
            @Field("text") String text
    );

    @GET("clips/{id1}/comments/{id2}")
    Call<Wrappers.Single<Comment>> commentsShow(@Path("id1") int clip, @Path("id2") int comment);

    @DELETE("clips/{id1}/comments/{id2}")
    Call<ResponseBody> commentsDelete(@Path("id1") int clip, @Path("id2") int comment);

    @GET("credits")
    Call<Wrappers.Paginated<Credit>> creditsIndex(@Query("page") int page);

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("devices")
    Call<ResponseBody> devicesCreate(
            @Field("platform") String platform,
            @Field("push_service") String pushService,
            @Field("push_token") String pushToken
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @PUT("devices/{id}")
    Call<ResponseBody> devicesUpdate(
            @Path("id") int device,
            @Field("push_token") String pushToken
    );

    @GET("users/{id}/followers")
    Call<Wrappers.Paginated<User>> followersIndex(
            @Path("id") int user,
            @Query("following") boolean following,
            @Query("page") int page
    );

    @POST("users/{id}/followers")
    Call<ResponseBody> followersFollow(@Path("id") int user);

    @DELETE("users/{id}/followers")
    Call<ResponseBody> followersUnfollow(@Path("id") int user);

    @GET("hashtags")
    Call<Wrappers.Paginated<Hashtag>> hashtagsIndex(
            @Query("q") String q,
            @Query("page") int page
    );

    @GET("items")
    Call<Wrappers.Paginated<Item>> itemsIndex(@Query("page") int page);

    @POST("clips/{id}/likes")
    Call<ResponseBody> likesLike(@Path("id") int clip);

    @DELETE("clips/{id}/likes")
    Call<ResponseBody> likesUnlike(@Path("id") int clip);

    @Headers("Accept: application/json")
    @GET("live-streams")
    Call<Wrappers.Paginated<LiveStream>> liveStreamsIndex(@Query("page") int page);

    @Headers("Accept: application/json")
    @POST("live-streams")
    @FormUrlEncoded
    Call<Wrappers.Single<LiveStream>> liveStreamsCreate(@Field("private") int _private);

    @Headers("Accept: application/json")
    @GET("live-streams/{id}")
    Call<Wrappers.Single<LiveStream>> liveStreamsShow(@Path("id") int id);

    @PATCH("live-streams/{id}")
    Call<Wrappers.Single<LiveStream>> liveStreamsTouch(@Path("id") int id);

    @DELETE("live-streams/{id}")
    Call<ResponseBody> liveStreamsDelete(@Path("id") int id);

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("live-streams/{id}/join")
    Call<LiveStreamAgora> liveStreamsJoinAgora(@Path("id") int id, @Field("uid") int uid);

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/facebook")
    Call<Token> loginFacebook(
            @Field("token") String token,
            @Field("referrer") @Nullable String referrer
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/firebase")
    Call<Token> loginFirebase(
            @Field("token") String token,
            @Field("referrer") @Nullable String referrer
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/google")
    Call<Token> loginGoogle(
            @Field("token") String token,
            @Field("referrer") @Nullable String referrer
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/email")
    Call<Token> loginEmail(
            @Field("email") String email,
            @Field("otp") String otp,
            @Field("name") String name,
            @Field("referrer") @Nullable String referrer
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/email/otp")
    Call<Exists> loginEmailOtp(@Field("email") String email);

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/phone")
    Call<Token> loginPhone(
            @Field("cc") String cc,
            @Field("phone") String phone,
            @Field("otp") String otp,
            @Field("name") String name,
            @Field("referrer") @Nullable String referrer
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/phone/otp")
    Call<Exists> loginPhoneOtp(
            @Field("cc") String cc,
            @Field("phone") String phone
    );

    @GET("threads/{thread}/messages")
    Call<Wrappers.Paginated<Message>> messagesIndex(
            @Path("thread") int thread,
            @Query("page") int page
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("threads/{thread}/messages")
    Call<Wrappers.Single<Message>> messagesCreate(
            @Path("thread") int thread,
            @Field("body") String body
    );

    @DELETE("threads/{thread}/messages/{message}")
    Call<ResponseBody> messagesDestroy(
            @Path("thread") int thread,
            @Path("message") int message
    );

    @GET("notifications")
    Call<Wrappers.Paginated<Notification>> notificationsIndex(@Query("page") int page);

    @GET("notifications/unread")
    Call<UnreadNotifications> notificationsUnread();

    @GET("notifications/{id}")
    Call<ResponseBody> notificationsShow(@Path("id") String notification);

    @DELETE("notifications/{id}")
    Call<ResponseBody> notificationsDelete(@Path("id") String notification);

    @Headers("Accept: application/json")
    @GET("profile")
    Call<Wrappers.Single<User>> profileShow();

    @DELETE("profile")
    Call<ResponseBody> profileDelete();

    @Headers("Accept: application/json")
    @Multipart
    @POST("profile")
    Call<ResponseBody> profileUpdate(
            @Part MultipartBody.Part photo,
            @Part("username") RequestBody username,
            @Part("bio") RequestBody bio,
            @Part("name") RequestBody name,
            @Part("email") RequestBody email,
            @Part("phone") RequestBody phone,
            @Part("location") RequestBody location,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @PartMap() Map<String, RequestBody> extras
    );

    @DELETE("profile/photo")
    Call<ResponseBody> profilePhotoDelete();

    @GET("promotions")
    Call<Wrappers.Paginated<Promotion>> promotionsIndex(@Query("after") long after);

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("reports")
    Call<ResponseBody> reportsCreate(
            @Field("subject_type") String subjectType,
            @Field("subject_id") long subjectId,
            @Field("reason") String reason,
            @Field("message") String message
    );

    @Headers("Accept: application/json")
    @POST("clips/{id}/saves")
    Call<ResponseBody> savesSave(@Path("id") int clip);

    @DELETE("clips/{id}/saves")
    Call<ResponseBody> savesUnsave(@Path("id") int clip);

    @GET("songs")
    Call<Wrappers.Paginated<Song>> songsIndex(
            @Query("q") String q,
            @Query("sections[]") Iterable<Integer> sections,
            @Query("page") int page
    );

    @GET("songs/sections")
    Call<Wrappers.Paginated<SongSection>> songSectionsIndex(
            @Query("q") String q,
            @Query("page") int page
    );

    @GET("songs/sections/{id}")
    Call<Wrappers.Single<SongSection>> songSectionsShow(@Path("id") int section);

    @GET("songs/{id}")
    Call<Wrappers.Single<Song>> songsShow(@Path("id") int song);

    @GET("stickers")
    Call<Wrappers.Paginated<Sticker>> stickersIndex(
            @Query("q") String q,
            @Query("sections[]") Iterable<Integer> sections,
            @Query("page") int page
    );

    @GET("stickers/sections")
    Call<Wrappers.Paginated<StickerSection>> stickerSectionsIndex(
            @Query("q") String q,
            @Query("page") int page
    );

    @GET("stickers/sections/{id}")
    Call<Wrappers.Single<StickerSection>> stickerSectionsShow(@Path("id") int section);

    @GET("stickers/{id}")
    Call<Wrappers.Single<Sticker>> stickersShow(@Path("id") int sticker);

    @GET("suggestions")
    Call<Wrappers.Paginated<User>> suggestionsIndex(@Query("page") int page);

    @GET("threads")
    Call<Wrappers.Paginated<Thread>> threadsIndex(@Query("page") int page);

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("threads")
    Call<Wrappers.Single<Thread>> threadsCreate(@Field("user") int user);

    @GET("threads/{id}")
    Call<Wrappers.Single<Thread>> threadsShow(@Path("id") int thread);

    @GET("users")
    Call<Wrappers.Paginated<User>> usersIndex(
            @Query("q") String q,
            @Query("page") int page
    );

    @GET("users/{id}")
    Call<Wrappers.Single<User>> usersShow(@Path("id") int user);

    @GET("users/{username}/find")
    Call<Wrappers.Single<User>> usersFind(@Path("username") String username);

    @Headers("Accept: application/json")
    @Multipart
    @POST("verifications")
    Call<ResponseBody> verificationsCreate(
            @Part MultipartBody.Part document,
            @Part("business") RequestBody business
    );

    @GET("wallet/balance")
    Call<Wrappers.Single<Balance>> walletBalance();

    @GET("wallet/gifts")
    Call<Wrappers.Paginated<Gift>> walletGifts(@Query("page") int page);

    @Headers("Accept: application/json")
    @POST("wallet/gifts")
    @FormUrlEncoded
    Call<Wrappers.Single<Balance>> walletGift(@Field("to") int to, @FieldMap Map<String, String> items);

    @Headers("Accept: application/json")
    @POST("wallet/recharge")
    @FormUrlEncoded
    Call<Wrappers.Single<Redirect>> walletRecharge(@Field("credit") int credit);

    @Headers("Accept: application/json")
    @POST("wallet/recharge/iab")
    @FormUrlEncoded
    Call<Wrappers.Single<Balance>> walletRechargeIab(@Field("sku") String sku, @Field("token") String token);

    @Headers("Accept: application/json")
    @POST("wallet/redeem")
    @FormUrlEncoded
    Call<Wrappers.Paginated<Gift>> walletRedeem(@Field("items[]") List<Integer> items);

    @GET("wallet/redemptions")
    Call<Wrappers.Paginated<Redemption>> walletRedemptions(@Query("page") int page);
}
