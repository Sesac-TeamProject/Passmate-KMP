package org.sesacteamproject.passmate.user.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.user.data.dto.BadgesResponse
import org.sesacteamproject.passmate.user.data.dto.ClaimGuestRecordRequest
import org.sesacteamproject.passmate.user.data.dto.GradeResponse
import org.sesacteamproject.passmate.user.data.dto.HostProfileResponse
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse
import org.sesacteamproject.passmate.user.data.dto.NotificationSettingsDto
import org.sesacteamproject.passmate.user.data.dto.ReportRequest
import org.sesacteamproject.passmate.user.data.dto.UpdateProfileRequest
import org.sesacteamproject.passmate.user.data.dto.UserProfileResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class UserRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchMyPage(cursor: String?): MyPageResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/rooms/joined") {
            if (cursor != null) {
                parameter("cursor", cursor)
            }
        }.body()
    }

    suspend fun claimGuestRecord(request: ClaimGuestRecordRequest) {
        apiClient.http.post("${apiClient.baseUrl}/guest-records/claim") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun fetchGrade(): GradeResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/grade").body()
    }

    suspend fun fetchBadges(): BadgesResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/badges").body()
    }

    suspend fun fetchHostProfile(userId: Long): HostProfileResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/$userId/profile").body()
    }

    suspend fun blockUser(userId: Long) {
        apiClient.http.post("${apiClient.baseUrl}/users/$userId/block")
    }

    suspend fun unblockUser(userId: Long) {
        apiClient.http.delete("${apiClient.baseUrl}/users/$userId/block")
    }

    suspend fun submitReport(request: ReportRequest) {
        apiClient.http.post("${apiClient.baseUrl}/reports") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun fetchMyProfile(): UserProfileResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me").body()
    }

    suspend fun updateMyProfile(request: UpdateProfileRequest) {
        apiClient.http.put("${apiClient.baseUrl}/users/me") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteAccount() {
        apiClient.http.delete("${apiClient.baseUrl}/users/me")
    }

    suspend fun fetchNotificationSettings(): NotificationSettingsDto {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/notification-settings").body()
    }

    suspend fun updateNotificationSettings(request: NotificationSettingsDto) {
        apiClient.http.put("${apiClient.baseUrl}/users/me/notification-settings") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
