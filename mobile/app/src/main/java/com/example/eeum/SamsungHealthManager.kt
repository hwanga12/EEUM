package com.example.eeum

import android.app.Activity
import android.content.Context
import com.google.gson.Gson
// 와일드카드 대신 명시적으로 임포트 [cite: 40]
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.Ordering
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import java.time.LocalDateTime
//import com.samsung.android.sdk.health.data.*
//import com.samsung.android.sdk.health.data.permission.AccessType
//import com.samsung.android.sdk.health.data.permission.Permission
//import com.samsung.android.sdk.health.data.request.Ordering
//import com.samsung.android.sdk.health.data.error.*
//import java.time.LocalDateTime

class SamsungHealthManager(private val context: Context) {
    // 삼성 헬스 데이터 스토어 인스턴스 가져오기
    private val healthDataStore = HealthDataService.getStore(context)

    // 1. 권한 체크 및 요청
    suspend fun checkAndRequestPermissions(activity: Activity): Boolean {
        val permSet = setOf(
            Permission.of(DataTypes.HEART_RATE, AccessType.READ),
            Permission.of(DataTypes.STEPS, AccessType.READ)
        )

        return try {
            val granted = healthDataStore.getGrantedPermissions(permSet)
            if (granted.containsAll(permSet)) {
                true
            } else {
                healthDataStore.requestPermissions(permSet, activity)
                false
            }
        } catch (e: HealthDataException) {
            // 삼성 헬스 미설치 등 해결 가능한 에러 처리
            if (e is ResolvablePlatformException && e.hasResolution) {
                e.resolve(activity)
            }
            false
        }
    }

    // 2. 최신 심박수 데이터 1건 조회
    suspend fun getLatestHeartRate(): String? {
        val endTime = LocalDateTime.now()
        val startTime = endTime.minusMinutes(10) // 최근 10분 데이터 필터

        val readRequest = DataTypes.HEART_RATE.readDataRequestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(startTime, endTime))
            .setOrdering(Ordering.DESC) // 최신순 정렬
            .setLimit(1)
            .build()

        return try {
            val response = healthDataStore.readData(readRequest)
            val latestData = response.dataList.firstOrNull()
            Gson().toJson(latestData) // Vue로 전달하기 위해 JSON 변환
        } catch (e: Exception) {
            null
        }
    }
}