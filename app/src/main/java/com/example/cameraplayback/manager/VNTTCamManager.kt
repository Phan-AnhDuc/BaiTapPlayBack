package com.example.cameraplayback.manager

import android.util.LongSparseArray
import com.example.cameraplayback.model.PlaybackFileModel
import com.example.cameraplayback.model.VideoStreamData
import com.example.cameraplayback.utils.Constant
import com.vnpttech.ipcamera.Constants
import com.vnpttech.model.DeviceInfo
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

interface VNTTCamManager {
    /**
     * Khởi tạo camera
     */
    fun createCameraManager(uid: String, pass: String)

    /**
     * Initialize callback
     */
    fun initCallback()

    /**
     * Get current flip camera
     */
    fun getCurrentFlipCamera()

    /**
     * Receive audio
     */
    fun receiveAudio(enable: Boolean): Int

    /**
     * Receive video
     */
    fun receiveVideo(enable: Boolean): Int

    /**
     * Connect to camera
     */
    fun connectToCamera(instanceId: Int)

    fun observeCameraState(): BehaviorSubject<Constant.CameraState>

    /**
     * Lấy uid của camera
     */
    fun getCameraUID(): String

    fun observeDeviceInfo(): PublishSubject<DeviceInfo>

    fun observeListVideoPlayback(): PublishSubject<LongSparseArray<ArrayList<PlaybackFileModel>>>

    fun observeCommandSet(): PublishSubject<Pair<Constants.Command?, Int>>

    fun observePlaybackVideoStream(): PublishSubject<VideoStreamData>

    fun observeVideoQualityRecord(): PublishSubject<Int>

    /**
     * Get video record quality
     */
    fun getVideoRecordQuality()

    /**
     * Function create Observable data list play back video Sd card
     * @param startDay
     * @return Emitter data -> ArrayList<Camera.fileTimeInfo>
     */
    fun getListVideoPlayBackSdCard(startDay: Long)

    /**
     * Get camera device info
     */
    fun getDeviceInfo()

    fun observePlaybackAudioStream(): PublishSubject<ByteArray>

    /**
     * Set playback mode
     */
    fun setPlaybackMode(enable: Boolean)

    fun setVideo()

    /**
     * Disconnect camera
     */
    fun disconnectCamera(): Int

    fun playBackVideoStart(playbackFile: PlaybackFileModel, offset: Int): Single<Int>
}