package com.example.cameraplayback.manager

import android.annotation.SuppressLint
import android.util.Log
import android.util.LongSparseArray
import com.example.cameraplayback.di.scheduler.SchedulerProviderImpl
import com.example.cameraplayback.model.PlaybackByDateModel
import com.example.cameraplayback.model.PlaybackFileModel
import com.example.cameraplayback.model.VideoStreamData
import com.example.cameraplayback.utils.Constant
import com.example.cameraplayback.utils.Constant.PHP_SERVER
import com.vnpttech.ipcamera.CameraCallback
import com.vnpttech.ipcamera.Constants
import com.example.cameraplayback.utils.Constant.VNPTCameraStatusCode.*
import com.example.cameraplayback.utils.CryptoAES
import com.google.gson.Gson
import com.vnpttech.ipcamera.VNPTCamera
import com.vnpttech.model.DeviceInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import vn.vnpt.ONEHome.di.component.scheduler.SchedulerProvider
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit


class VNTTCamManagerImpl : VNTTCamManager, CameraCallback {
    private lateinit var camera: VNPTCamera

    private var playbackMode = false

    private var isPlayVideoPlayback = false

    private val playbackAudioStream: PublishSubject<ByteArray> by lazy { PublishSubject.create() }

    private val liveAudioStream: PublishSubject<ByteArray> by lazy { PublishSubject.create() }

    private val playbackTimestamp: PublishSubject<Long> by lazy { PublishSubject.create() }

    private val playbackVideoStream: PublishSubject<VideoStreamData> by lazy { PublishSubject.create() }

    private val liveVideoStream: PublishSubject<VideoStreamData> by lazy { PublishSubject.create() }

    private var isOnBackPress = false

    private var needChangePass = false

    val schedulerProvider: SchedulerProvider = SchedulerProviderImpl()

    private var needSetDefaultConfig: Boolean = false

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    private var backendPassword = Constant.EMPTY_STRING

    private val stateCameraSubject: BehaviorSubject<Constant.CameraState> by lazy { BehaviorSubject.create() }

    private var countPasswordFailed: Int = 0

    private val cameraInfo: PublishSubject<DeviceInfo> by lazy { PublishSubject.create() }

    private val listVideoPlayback: PublishSubject<LongSparseArray<ArrayList<PlaybackFileModel>>> by lazy { PublishSubject.create() }


    private val commandSetSubject: PublishSubject<Pair<Constants.Command?, Int>> by lazy { PublishSubject.create() }

    private val qualityVideoRecordSubject: PublishSubject<Int> by lazy { PublishSubject.create() }

    // Page của playback sd card
    private var pageNumber = 0

    private var startDayQueryPlayback: Long = Long.MIN_VALUE

    private var audioCodec: Int =
        0                 // Audio codec: phục vụ cho push to talk, 1 - PCMA, 0: PCM

    private val commandGetSubject: PublishSubject<Triple<Constants.Command?, Int, String?>> by lazy { PublishSubject.create() }

    private val stopPlayback: PublishSubject<Boolean> by lazy { PublishSubject.create() }




    override fun createCameraManager(uid: String, pass: String) {
        if (!::camera.isInitialized) {
            camera = VNPTCamera("VNTTA-017631-MVEGE", pass)
        }
    }

    override fun setVideo(){
        camera.setVideo(true)
    }

    override fun initCallback() {
        camera.init(this)
    }

    override fun observeTimestampPlayback(): PublishSubject<Long> {
        return playbackTimestamp
    }

    override fun playBackVideoStart(playbackFile: PlaybackFileModel, offset: Int): Single<Int> {
        return Single.create { emitter ->
            val playbackFileName = playbackFile.name
            val result = camera.playSDVideo(playbackFileName!!, offset)
            emitter.onSuccess(result)
        }
    }

    override fun onReceiveAudio(
        data: ByteArray?,
        length: Int,
        sequence: Int,
        timestamp: Int,
    ) {
        if (playbackMode && isPlayVideoPlayback) {
            playbackAudioStream.onNext(data!!)
        } else {
            liveAudioStream.onNext(data!!)
        }
    }

    override fun onReceiveVideo(
        byteArray_Data_fy: ByteArray?,
        byteArray_Data_fu: ByteArray?,
        byteArray_Data_fv: ByteArray?,
        widthData: Int,
        heightData: Int,
        sequence: Int,
        timestamp: Int,
    ) {
        if (byteArray_Data_fy != null &&
            byteArray_Data_fu != null &&
            byteArray_Data_fv != null
        ) {
            val data = VideoStreamData(
                byteArray_Data_fy,
                byteArray_Data_fu,
                byteArray_Data_fv,
                widthData,
                heightData
            )
            if (playbackMode && isPlayVideoPlayback) {
                playbackTimestamp.onNext(timestamp.toLong())
                playbackVideoStream.onNext(data)
            } else {
                liveVideoStream.onNext(data)
            }
        }
    }

    override fun onConnect(
        sessionHandler: Int,
        command: Constants.Command?,
        status: Int
    ) {
        Log.e("onConnect", "onConnect: ${command?.name} status: $status sessionId $sessionHandler")
        checkStatusCode(status)
    }


    override fun onConnectionLoss(p0: Int, p1: Constants.Command?) {
        TODO("Not yet implemented")
    }

    override fun onCommandSet(
        command: Constants.Command?,
        status: Int
    ) {
        Log.e("onCommandSet","onCommandSet: ${command?.name} status: $status sessionId: ${camera.getSessionHandle()}")

        commandSetSubject.onNext(Pair(command, status))

        when (command) {
            Constants.Command.SDK_PLAY_SD_VIDEO_COMMAND -> {
                isPlayVideoPlayback = true
            }

            Constants.Command.SDK_START_STOP_PLAYBACK_COMMAND -> {
                if (isOnBackPress) {
                    receiveVideo(true)
                }
                isOnBackPress = false
                isPlayVideoPlayback = false
            }

            Constants.Command.SDK_SET_PASSWORD_COMMAND -> {
                needChangePass = status != 0
                needSetDefaultConfig = status == 0
                countPasswordFailed = 0
                // Nếu set password thành công --> disconnect và connect lại với password mới
                reconnectWithNewPassword(status == 0)
            }

            Constants.Command.SDK_START_RECV_VIDEO_COMMAND -> {
                    forceVideoQualityToSD()


                if (status != 0) {
                    stateCameraSubject.onNext(Constant.CameraState.CameraOffline)
                    return
                }
//                if (qualityCameraSubject.value == Constants.CameraVideoQuality.SD.value && camera.isVideoEnable()) {
//                    forceVideoQualityToSD()
//                }
            }

            Constants.Command.SDK_SET_VIDEO_QUALITY_COMMAND -> {
//                qualityCameraSubject.onNext(requestQuality)
            }

            Constants.Command.SDK_START_SEND_AUDIO_COMMAND -> {
//                if (pushToTalk) {
//                    if (status == 0) {
//                        delayFunction(300) {
//                            initAudioRecorder()
//                            pushAudioToCamera(audioCodec)
//                        }
//                    }
//
//                    pushToTalkState.onNext(status == 0)
//                }
            }

            else -> {}
        }
    }

    /**
     * Hàm set SD video quality
     */
    fun forceVideoQualityToSD() {
        compositeDisposable.add(
            Single.create<Int> { emitter ->
                val result = camera.setVideoQuality(Constants.CameraVideoQuality.SD.value, -1)
                emitter.onSuccess(result)
            }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.io())
                .subscribe({ result ->
                    Log.e("forceVideoQualityToSD","Force video quality SD: $result")
                }, {
                    Log.e("forceVideoQualityToSD","Force video quality SD: failed")
                })
        )
    }

    fun delayFunction(timeDelay: Long, function: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(timeMillis = timeDelay)
            function.invoke()
        }
    }

    /**
     * Disconnect session cũ và connect lại với password mới
     */
    private fun reconnectWithNewPassword(reconnect: Boolean) {
        if (reconnect) {
            disconnectCamera()
            delayFunction(1000) {
                receiveAudio(false)
                receiveVideo(true)
                camera.setPassword(backendPassword)

                val instanceId = (System.currentTimeMillis() / 1000).toInt()
                connectToCamera(instanceId)
            }
        } else {
            stateCameraSubject.onNext(Constant.CameraState.CameraOffline)
        }
    }

    override fun disconnectCamera(): Int {
        val result = camera.disconnect()
        compositeDisposable.clear()
        Log.e("disconnectCamera","Disconnect camera ${getCameraUID()} sessionId: ${camera.getSessionHandle()} -> result = $result")
        return result
    }

    override fun onCommandGet(
        command: Constants.Command?,
        status: Int,
        resultInt: Int,
        resultString: String?,
    ) {
        Log.e("onCommandGet"," ${command?.name} status: $status resultInt: $resultInt sessionId: ${camera.getSessionHandle()} resultString: $resultString")

        when (command) {
            Constants.Command.SDK_GET_ALL_VIDEO_TIME_COMMAND -> {
                // Có trường hợp SDK_GET_ALL_VIDEO_TIME_COMMAND trả về nhanh hơn quá trình xử lý của điện thoại
                // -> cần delay 150ms để đủ thời gian xử lý tuần tự từng bản tin một
                delayFunction(150) {
                    processQueryPlaybackResult(status, resultString)
                }
            }

            Constants.Command.SDK_SET_RUN_COMMAND -> {
                if (status != Constant.VNPTCameraStatusCode.DeviceRequestExpired.value) {
                    resultString?.let { data ->
//                        ptzDirectionValue.onNext(
//                            getPtzDirectionValue(data)
//                        )
                    }
                }
            }

            Constants.Command.SDK_CHANGE_FLIP_COMMAND -> {
                if (status != Constant.VNPTCameraStatusCode.DeviceRequestExpired.value) {
                    // 1: flip, 0: không flip
//                    flipCameraSubject.onNext(status == 1)

                    resultString?.let { data ->
//                        ptzDirectionValue.onNext(
//                            getPtzDirectionValue(data)
//                        )
                    }
                }
            }

            else -> {}
        }

        if (status == 0) {
            commandGetSubject.onNext(Triple(command, resultInt, resultString))

            when (command) {
                Constants.Command.SDK_GET_FLIPPING_COMMAND -> {
//                    flipCameraSubject.onNext(resultInt == 1)
                }

                Constants.Command.SDK_GET_DEVICEINFO_COMMAND -> {
                    resultString?.let { result ->
                        gson.fromJson(result, DeviceInfo::class.java)?.let { info ->
                            audioCodec = info.audioCodec ?: 0
                            cameraInfo.onNext(info)
                        }
                    }
                }

                Constants.Command.SDK_GET_RECORD_VIDEO_QUALITY_COMMAND -> {
                    qualityVideoRecordSubject.onNext(resultInt)
                }

                Constants.Command.SDK_GET_VIDEO_RECORD_TYPE_COMMAND -> {
//                    resultString?.let { result ->
//                        val responseBody = JSONObject(result)
//
//                        if (responseBody.has("record_mode")) {
//                            val record: Int = responseBody.optInt("record_mode")
//                            videoRecordType.onNext(record)
//                        }
//                    }
                }

                Constants.Command.SDK_GET_LIST_RECORD_SCHEDULE_COMMAND -> {
//                    resultString?.let { result ->
//                        val responseBody = JSONObject(result)
//                        if (responseBody.has("record")) {
//                            val data = gson.fromJson(
//                                responseBody.getString("record"),
//                                GetListScheduleRecordResponse::class.java
//                            )
//                            listRecordScheduleSubject.onNext(data)
//                        }
//                    }
                }

                Constants.Command.SDK_ZONE_MD_NOTI_COMMAND -> {
//                    resultString?.let {
//                        val json = JSONObject(it)
//
//                        if (json.has("motion_detect")) {
//                            val motionDetect = gson.fromJson(
//                                json.getString("motion_detect"),
//                                MotionDetectModel::class.java
//                            )
//                            motionDetectArea.onNext(motionDetect)
//                        }
//                    }
                }

                Constants.Command.SDK_LIST_MD_NOTI_COMMAND -> {
//                    resultString?.let {
//                        val json = JSONObject(it)
//
//                        if (json.has("motion_detect")) {
//                            val motionDetect = gson.fromJson(
//                                json.getString("motion_detect"),
//                                MotionDetectModel::class.java
//                            )
//                            listMDNotification.onNext(motionDetect)
//                        }
//                    }
                }

                Constants.Command.SDK_GET_DEVICE_SPEAKER_VOLUME_COMMAND -> {
//                    speakerVolume.onNext(resultInt)
                }

                Constants.Command.SDK_GET_RSSI_COMMAND -> {
//                    rssi.onNext(resultInt)
                }

                Constants.Command.SDK_GET_WIFI_SCAN_COMMAND -> {

//                    resultString?.let {
//                        val json = JSONObject(it)
//
//                        val wifi = gson.fromJson(
//                            json.toString(),
//                            WifiModel::class.java
//                        )
//
//                        wifiList.onNext(wifi)
//                    }
                }

                else -> {}
            }
        } else {
//            commandGetFailed.onNext(true)
        }
    }

    private val gson = Gson()

    private val playbackData: LongSparseArray<ArrayList<PlaybackFileModel>> = LongSparseArray()

    /**
     * Xử lý kết quả playback nhận được từ cam
     * + Nếu status = 0: Kiểm tra xem đã query hết tất cả các page chưa, nếu chưa thì tiếp tục query. Nếu đã query hết thì onNext playbackData sang ViewModel
     * + Nếu status != 0: dừng lại và onNext playbackData đã query được sang ViewModel
     */
    private fun processQueryPlaybackResult(status: Int, resultString: String?) {
        if (status == 0) {
            val playbackFilesByDate = gson.fromJson(resultString, PlaybackByDateModel::class.java)

            Log.d("ducpa", "playbackFilesByDate: $playbackFilesByDate")

            if (playbackFilesByDate.playbackFiles.isNullOrEmpty()) {
                playbackFilesByDate.playbackFiles = ArrayList()
            } else {
                playbackFilesByDate.playbackFiles.map { playbackModel ->
                    playbackModel.page = playbackFilesByDate.page
                    playbackModel.pageTotal = playbackFilesByDate.pageTotal
                }
            }

            if (pageNumber == 0) {
                playbackData.put(startDayQueryPlayback, playbackFilesByDate.playbackFiles)
                Log.d("ducpa", "playbackData page 0: $playbackData")
            } else {
                playbackData.get(startDayQueryPlayback).addAll(playbackFilesByDate.playbackFiles)
                Log.d("ducpa", "playbackData: $playbackData")
            }
            if (playbackFilesByDate.pageTotal == 0) {
                // Trường hợp pageTotal = 0 --> ko có video nào trong ngày hôm đó
                listVideoPlayback.onNext(playbackData)
                clearPageNumberAndDataPlayback()
            }

            if (playbackFilesByDate.page < playbackFilesByDate.pageTotal - 1) {
                // Chưa query hết các page -> tiếp tục query
                pageNumber += 1
                getListVideoPlayBackSdCard(startDayQueryPlayback)
            }

            if (playbackFilesByDate.page == playbackFilesByDate.pageTotal - 1) {
                // Đã query hết tất cả các page -> onNext sang viewmodel để play
                listVideoPlayback.onNext(playbackData)

                clearPageNumberAndDataPlayback()
            }
        } else {
            if (pageNumber == 0) {
                // Nếu query bị lỗi ngay từ page đầu --> set trạng thái offline cho cam luôn
                stateCameraSubject.onNext(Constant.CameraState.CameraOffline)
            } else {
                // Đã query được 1 vài page nhưng gặp lỗi -> onNext sang viewmodel, query được bao nhiêu play bấy nhiêu
                listVideoPlayback.onNext(playbackData)
                clearPageNumberAndDataPlayback()
            }
        }
    }

    private fun clearPageNumberAndDataPlayback() {
        pageNumber = 0
        delayFunction(100) {
            playbackData.clear()
        }
    }

    override fun onStatistic(
        p0: Int,
        p1: Constants.Command?,
        p2: Int,
        p3: Long,
        p4: Long,
        p5: Long,
        p6: Long
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Lấy trạng thái của camera theo status
     */
    private fun checkStatusCode(status: Int) {
        isOnBackPress =
            false // Nếu lúc thoát khỏi màn playback mà cam mất kết nối phải gán lại vì stop playback sẽ k đc thực hiện
        when (status) {
            ConnectSuccessful.value -> {
                when {
                    needChangePass -> {
                        Log.e("needChangePass", "Start change password")
                        changeCameraPassword()
                    }

                    needSetDefaultConfig -> {
                        needSetDefaultConfig = false
                        stateCameraSubject.onNext(Constant.CameraState.CameraOnline)
                        setDefaultConfigCamera()
                    }

                    else -> {
                        // Cam online thật lòng :))
                        getCurrentFlipCamera()
                        stateCameraSubject.onNext(Constant.CameraState.CameraOnline)
                    }

                }
            }

            LossConnection.value -> {
                reconnectCamera()
                stateCameraSubject.onNext(Constant.CameraState.CameraLossConnection)
            }

            SessionAlreadyConnected.value -> { //-128
                reconnectCamera()
            }

            UidNotLicenced.value -> {
                stateCameraSubject.onNext(Constant.CameraState.CameraInvalidUid)
            }

            ClientPasswordInvalid.value -> {
                /*
                 * Đang xem live view, connect tới camera và gặp lỗi -145 (sai password)
                 * B1: Connect lại camera với default password (không cần disconnect vì khi login bị sai pass thì chưa khởi tạo session mới)
                 * B2: Set new password cho camera (camera.changCameraPassword)
                 * B3: Disconnect
                 * B4: Connect lại với password mới và set default config cho camera
                 */
                countPasswordFailed += 1
                needChangePass = true

                when (countPasswordFailed) {
                    1 -> {
                        connectWithDefaultPass()
                    }

                    2 -> {
                        // Khi connect lại với pass mặc định vẫn bị sai, thì show thông báo lỗi
                        countPasswordFailed = 0
                        stateCameraSubject.onNext(Constant.CameraState.CameraOffline)
                    }
                }
            }

            else -> {
                stateCameraSubject.onNext(Constant.CameraState.CameraOffline)
            }
        }
    }

    /**
     * Kết nối camera với pass mặc định
     */
    private fun connectWithDefaultPass() {
        receiveAudio(false)
        receiveVideo(false)
        camera.setPassword(Constant.ConfigCamera.VNPT_CAMERA_DEFAULT_PASSWORD)

        val instanceId = (System.currentTimeMillis() / 1000).toInt()

        connectToCamera(instanceId)
    }

    override fun connectToCamera(instanceId: Int) {
        compositeDisposable.add(
            Single.create<Int> { emitter ->
                stateCameraSubject.onNext(Constant.CameraState.Init)
                val result = camera.connect(instanceId, Constant.BLANSEARCH)
                emitter.onSuccess(result)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ result ->
                    Log.e("connectToCamera","Connect to camera: $result")
                }, {
                    Log.e("connectToCamera","Connect to failed: ${it.message}")
                })
        )
    }

    override fun observeCameraState(): BehaviorSubject<Constant.CameraState> {
        return stateCameraSubject
    }

    override fun getCameraUID(): String = "VNTTA-017631-MVEGE"

    override fun receiveAudio(enable: Boolean): Int {
        if (checkCameraInitialize()) return camera.setAudio(enable)
        return -1
    }

    override fun receiveVideo(enable: Boolean): Int {
        if (checkCameraInitialize()) return camera.setVideo(enable)
        return -1
    }

    private fun checkCameraInitialize() = ::camera.isInitialized

    override fun getCurrentFlipCamera() {
        compositeDisposable.add(
            Single.create<Int> { emitter ->
                val result = camera.getFLipping()
                emitter.onSuccess(result)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ result ->
                    Log.e("getCurrentFlipCamera","Get current flip camera: $result")
                }, { error ->
                    Log.e("getCurrentFlipCamera","Get current flip camera failed: ${error.message}")
                })
        )
    }

    override fun stopPlayback(): Single<Int> {
        return Single.create { emitter ->
            val result = camera.startStopPlayback(0)
            emitter.onSuccess(result)
        }
    }

    /**
     * Đổi mật khẩu camera
     */
    private fun changeCameraPassword() {
        compositeDisposable.add(
            Single.create<Int> { emitter ->
                val result = camera.setPassword(
                    oldValue = Constant.ConfigCamera.VNPT_CAMERA_DEFAULT_PASSWORD,
                    newValue = backendPassword
                )
                emitter.onSuccess(result)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.e(
                        "changeCameraPassword",
                        "Change camera password after add camera: status = $it"
                    )
                }, {
                    Log.e("changeCameraPassword", "Change camera password failed -> ${it.message}")
                })
        )
    }

    /**
     * Set default config camera
     * 1. setTimeZone(timeZone = 7) => khu vực (NOTE: Đã bỏ)
     * 2. setDeviceVolume(capture = -1, playback = 75) => set volume phát hiện chuyển động (NOTE: Đã bỏ)
     * 3. setDeviceVolume(capture = 100, playback = -1) => set mic (NOTE: Đã bỏ)
     * 4. setRecordVideoQuality( quality = Constants.CameraVideoQuality.FHD.value,frameRate = -1) => độ phân giải FHD
     * 5. setUrlServer() => sever trỏ đến để nhận thông báo
     */
    private fun setDefaultConfigCamera() {
        val apiLink = CryptoAES.decrypt3DES(PHP_SERVER)

        compositeDisposable.add(
            setUrlServer(
                eventServer = apiLink
            ).toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    Log.e("setDefaultConfigCamera", "Set default config camera after add cam")
                }, {
                    Log.e("setDefaultConfigCamera", "Set default config failed")
                })
        )
    }

    /**
     * Set URL server, PHP server: eventServer
     * Khi thiết lập url nào thì nhập giá trị cho trường đó, các trường còn lại để String trống ""
     */
    private fun setUrlServer(
        eventServer: String,
        ntpServer: String = Constant.EMPTY_STRING,
        cloudServer: String = Constant.EMPTY_STRING,
        otaServer: String = Constant.EMPTY_STRING,
    ): Single<Int> {
        return Single.create { emitter ->
            val result = camera.setUrlServer(ntpServer, cloudServer, otaServer, eventServer)
            emitter.onSuccess(result)
        }
    }

    private fun reconnectCamera() {
        Log.e("reconnectCamera","Start reconnectCamera ${camera.getSessionHandle()}")

        compositeDisposable.add(
            Single.create<Int> { emitter ->
                val result = camera.reconnect(Constant.BLANSEARCH)
                emitter.onSuccess(result)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ result ->
                    Log.e("reconnectCamera","reconnectCameraResult: $result")
                }, {
                    Log.e("reconnectCamera","reconnectCamera: failed")
                })
        )
    }

    override fun observeDeviceInfo(): PublishSubject<DeviceInfo> {
        return cameraInfo
    }

    override fun observeListVideoPlayback(): PublishSubject<LongSparseArray<ArrayList<PlaybackFileModel>>> {
        return listVideoPlayback
    }

    override fun observeCommandSet(): PublishSubject<Pair<Constants.Command?, Int>> {
        return commandSetSubject
    }

    override fun observePlaybackVideoStream(): PublishSubject<VideoStreamData> {
        return playbackVideoStream
    }

    override fun observeVideoQualityRecord(): PublishSubject<Int> {
        return qualityVideoRecordSubject
    }

    override fun getVideoRecordQuality() {
        compositeDisposable.add(
            Single.create<Int> { emitter ->
                val result = camera.getVideoRecordQuality()
                emitter.onSuccess(result)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    Log.e("getVideoRecordQuality","Get video record quality: $result")
                }, {
                    Log.e("getVideoRecordQuality","Get video record quality failed: ${it.message}")
                })
        )
    }

    override fun getDeviceInfo() {
        compositeDisposable.add(
            Single.create<Int> { emitter ->
                val result = camera.getDeviceInfo()
                emitter.onSuccess(result)
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.e("getDeviceInfo", "Get device info: $it")
                }, {
                    Log.e("getDeviceInfo","Get device info failed: ${it.message}")
                })
        )
    }

    override fun observePlaybackAudioStream(): PublishSubject<ByteArray> {
        return playbackAudioStream
    }

    @SuppressLint("CheckResult")
    override fun getListVideoPlayBackSdCard(startDay: Long) {
        val startTime = TimeUnit.MILLISECONDS.toSeconds(startDay).toInt()
        val endTime = TimeUnit.MILLISECONDS.toSeconds(getEndOfDay(startDay)).toInt()

        Log.e("getListVideoPlayBackSdCard","startGetListPlayback: $startTime")
        Log.e("getListVideoPlayBackSdCard","endGetListPlayback: $endTime")

        Observable.create<Int> { emitter ->
            // convert startDay and endDay to second unit
            // set default video file type is -1
            emitter.onNext(
                camera.getAllVideoTime(
                    startTime,
                    endTime,
                    -1,
                    pageNumber
                )
            )
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                startDayQueryPlayback = startDay
                Log.e("getListVideoPlayBackSdCard","Get list video playback SdCard: Page = $pageNumber, status: $it")
            }, {
                Log.e("getListVideoPlayBackSdCard","Get list video playback SdCard status failed: ${it.message}")
            })
    }

    fun getEndOfDay(currentTime: Long): Long {
        val start = getStartOfDay(currentTime)
        return start + 24 * 60 * 60 * 1000 - 1
    }

    fun getStartOfDay(currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime)
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.timeInMillis
    }

    override fun setPlaybackMode(enable: Boolean) {
        playbackMode = enable
    }

}
