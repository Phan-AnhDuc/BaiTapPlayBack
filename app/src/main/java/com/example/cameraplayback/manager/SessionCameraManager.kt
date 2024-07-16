package com.example.cameraplayback.manager

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.internal.filterList

object SessionCameraManager {
    private val listCameraManager = mutableListOf<VNTTCamManager>()
    private val listCameraStatusManager = mutableListOf<VNTTCamManager>()

    /**
     * Kiểm tra trong list camera, đã tồn tại session của camera với UID tương ứng hay chưa
     */
    fun isSessionExist(uid: String): Boolean {
        for (camera in listCameraManager) {
            if (camera.getCameraUID() == uid) return true
        }
        return false
    }

    /**
     * Lấy ra camera manager tương ứng theo UID
     */
    fun getCameraManagerByUID(uid: String): VNTTCamManager? {
        for (camera in listCameraManager) {
            if (camera.getCameraUID() == uid) {
                return camera
            }
        }
        return null
    }
    /**
     * Thêm camera vào listCameraManager
     */
    fun addCamera(uid: String, camera: VNTTCamManager) {
        if (!isSessionExist(uid)) {
            listCameraManager.add(camera)
            Log.e("addCamera", "Add camera to list: $uid -> listCameraManager = ${listCameraManager.size}")
        }
    }


}

sealed class CheckCameraStatusState {
    object StopCheckStatus : CheckCameraStatusState()
    object StartCheckStatus : CheckCameraStatusState()
    object FirstCheckStatus : CheckCameraStatusState()
    object ResumeCheckStatus : CheckCameraStatusState()
}