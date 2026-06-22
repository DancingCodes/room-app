package love.moonc.room.data.api

import love.moonc.room.data.model.ApiResponse
import love.moonc.room.data.model.AuthResult
import love.moonc.room.data.model.AvatarPayload
import love.moonc.room.data.model.CreateMessageRequest
import love.moonc.room.data.model.CreateRoomRequest
import love.moonc.room.data.model.EmailCodeRequest
import love.moonc.room.data.model.LoginRequest
import love.moonc.room.data.model.MemberPayload
import love.moonc.room.data.model.MessageListPayload
import love.moonc.room.data.model.MessagePayload
import love.moonc.room.data.model.RegisterRequest
import love.moonc.room.data.model.ResetPasswordRequest
import love.moonc.room.data.model.RoomDetail
import love.moonc.room.data.model.RoomListPayload
import love.moonc.room.data.model.UpdateMeRequest
import love.moonc.room.data.model.UpdateMicRequest
import love.moonc.room.data.model.UserPayload
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface RoomApi {
    @Multipart
    @POST("api/v1/uploads/avatar")
    suspend fun uploadRegisterAvatar(@Part file: MultipartBody.Part): ApiResponse<AvatarPayload>

    @POST("api/v1/auth/register-code")
    suspend fun sendRegisterCode(@Body body: EmailCodeRequest): ApiResponse<Unit>

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthResult>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthResult>

    @POST("api/v1/auth/password-reset-code")
    suspend fun sendPasswordResetCode(@Body body: EmailCodeRequest): ApiResponse<Unit>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): ApiResponse<Unit>

    @GET("api/v1/users/me")
    suspend fun me(): ApiResponse<UserPayload>

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body body: UpdateMeRequest): ApiResponse<UserPayload>

    @Multipart
    @POST("api/v1/users/me/avatar")
    suspend fun updateMyAvatar(@Part file: MultipartBody.Part): ApiResponse<UserPayload>

    @GET("api/v1/rooms")
    suspend fun rooms(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ApiResponse<RoomListPayload>

    @POST("api/v1/rooms")
    suspend fun createRoom(@Body body: CreateRoomRequest): ApiResponse<RoomDetail>

    @GET("api/v1/rooms/{room_id}")
    suspend fun roomDetail(@Path("room_id") roomId: Long): ApiResponse<RoomDetail>

    @POST("api/v1/rooms/{room_id}/join")
    suspend fun joinRoom(@Path("room_id") roomId: Long): ApiResponse<RoomDetail>

    @POST("api/v1/rooms/{room_id}/leave")
    suspend fun leaveRoom(@Path("room_id") roomId: Long): ApiResponse<Unit>

    @PATCH("api/v1/rooms/{room_id}/mic")
    suspend fun updateMic(
        @Path("room_id") roomId: Long,
        @Body body: UpdateMicRequest,
    ): ApiResponse<MemberPayload>

    @GET("api/v1/rooms/{room_id}/messages")
    suspend fun messages(
        @Path("room_id") roomId: Long,
        @Query("limit") limit: Int = 20,
        @Query("before_id") beforeId: Long? = null,
    ): ApiResponse<MessageListPayload>

    @POST("api/v1/rooms/{room_id}/messages")
    suspend fun createMessage(
        @Path("room_id") roomId: Long,
        @Body body: CreateMessageRequest,
    ): ApiResponse<MessagePayload>
}
