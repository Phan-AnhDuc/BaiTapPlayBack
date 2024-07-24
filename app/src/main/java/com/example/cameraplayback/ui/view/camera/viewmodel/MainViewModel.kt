package com.example.cameraplayback.ui.view.camera.viewmodel

import com.example.cameraplayback.manager.VNTTCamManagerImpl
import android.util.Log
import android.util.LongSparseArray
import androidx.core.util.forEach
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraplayback.R
import com.example.cameraplayback.di.scheduler.SchedulerProviderImpl
import com.example.cameraplayback.manager.SessionCameraManager
import com.example.cameraplayback.manager.VNTTCamManager
import com.example.cameraplayback.model.Device
import com.example.cameraplayback.model.PlaybackFileModel
import com.example.cameraplayback.model.VideoStreamData
import com.example.cameraplayback.resource.ResourcesService
import com.example.cameraplayback.ui.theme.ColorData
import com.example.cameraplayback.utils.Constant
import com.example.cameraplayback.utils.CryptoAES.Companion.decrypt
import com.example.cameraplayback.utils.extension.getEndOfDay
import com.vnpttech.ipcamera.Constants
import com.vnpttech.model.DeviceInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang.StringUtils
import vn.vnpt.ONEHome.di.component.scheduler.SchedulerProvider
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class MainViewModel : ViewModel()  {
    private val _device = MutableLiveData<Device>()
    val device: LiveData<Device> get() = _device

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error
    private var devicesCamera: Device? = null
    private var seekTimeValue: Long = 0
    private var idCam = 0

    private var isNextFile: Boolean = false

    private var uidCam = "VNTTA-017631-MVEGE"

    private var passCam = Constant.EMPTY_STRING

    var vnttCamManager: VNTTCamManager = VNTTCamManagerImpl()

    private lateinit var _compositeDisposable: CompositeDisposable

    val compositeDisposable: CompositeDisposable
        get() {
            // Khi Xoay ngang màn hình thì compositeDisposable bị disposed đi => Các request sẽ ko Submit được
            // Trong trường hợp này khởi tạo lại
            if (_compositeDisposable.isDisposed) {
                _compositeDisposable = CompositeDisposable()
            }
            return _compositeDisposable
        }

    init {
        _compositeDisposable = CompositeDisposable()
    }

    var schedulerProvider: SchedulerProvider = SchedulerProviderImpl()

    private val _cameraState: MutableLiveData<Constant.CameraState> by lazy { MutableLiveData() }
    val cameraState: LiveData<Constant.CameraState> get() = _cameraState

    private var isCameraOnline = false

    private val _cameraInfo: MutableLiveData<DeviceInfo> by lazy { MutableLiveData() }
    val cameraInfo: LiveData<DeviceInfo> get() = _cameraInfo

    var startDay = 1720823164000

    private var disposableVideoStream: Disposable? = null

    private var disposableAudioStream: Disposable? = null

    private var disposableTimeStamp: Disposable? = null

    private var disposableVideoRecordStream: Disposable? = null

    private var isCancelledPlayback = false

    private var hasDataVideo: Boolean = false

    private var startSeekEvent: Boolean = false

    private val _dataVideoStream: PublishSubject<VideoStreamData> by lazy { PublishSubject.create() }
    val dataVideoStream: PublishSubject<VideoStreamData> get() = _dataVideoStream

    var currentFilePlay: PlaybackFileModel? = null

    private var currentFileName: String = Constant.EMPTY_STRING      // File playback đang được play

    private val _videoRecordQuality: MutableLiveData<Int> by lazy { MutableLiveData() }
    val videoRecordQuality: LiveData<Int> get() = _videoRecordQuality

    private val _playbackState: MutableLiveData<Constant.PlaybackSdCardStateUI> by lazy { MutableLiveData() }
    val playbackState: LiveData<Constant.PlaybackSdCardStateUI> get() = _playbackState

    private var listPlayback: LongSparseArray<ArrayList<PlaybackFileModel>> = LongSparseArray()

    private var eventToPlay: Constant.PlayPlaybackFileEvent = Constant.PlayPlaybackFileEvent.FIRST_TIME

    private var emptyFileAllDay: Boolean = true

    private var isNextDay: Boolean = false

    var firstTimeComeIn = System.currentTimeMillis()

    private var startTimeOfSelectedDay = getStartOfDay(firstTimeComeIn)

    private lateinit var resourcesService: ResourcesService

    val cursorTimebar: LiveData<Pair<Boolean, Long>> get() = _cursorTimebar

    private val _colorDataTimeline: MutableLiveData<List<ColorData>> by lazy { MutableLiveData() }
    val colorDataTimeline: LiveData<List<ColorData>> get() = _colorDataTimeline

    private val _cursorTimebar: MutableLiveData<Pair<Boolean, Long>> by lazy { MutableLiveData() }

    open var isInitialized: Boolean = false

    private var isPlayLastFileComplete = false

    private var isPaused: Boolean = false

    private val _dateList: MutableLiveData<ArrayList<Long>> by lazy { MutableLiveData() }

    private var isDay: Boolean = true

    fun setDataCamera(device: Device) {
        devicesCamera = device
        idCam = device.id ?: 0
        uidCam = device.uid ?: device.name.toString()
        passCam = device.password ?: Constant.EMPTY_STRING

    }

    internal fun init(
        schedulerProvider: SchedulerProvider,
        resourcesService: ResourcesService,
    ) {
        this.schedulerProvider = schedulerProvider
        this.resourcesService = resourcesService
        _compositeDisposable = CompositeDisposable()
        isInitialized = true
    }

    /**
     * Set time seek value
     */
    fun setTimeSeekValue(value: Long) {
        seekTimeValue = value
    }

    /**
     * 1. Set playback mode for camera manager
     * 2. Get id, uid, password camera
     * 3. Initialize camera manager
     * 4. Set camera live data value
     */

    fun prepareAndInitializeCamera(camera: Device) {
        getIdCamera(camera)
        initializeCameraManager(idCam, camera.name.toString(), passCam)
        setPlaybackMode()
//        setCameraDevice(camera)
    }

    /**
     * Trường hợp app ở trạng thái onResume (vào app lần đầu sẽ không chạy hàm này)
     * 1. Nếu trong Session manager đã tồn tại cam, tiếp tục play file playback
     * 2. Nếu Session đã bị disconnect, thì connect lại camera
     */
    fun reconnectCameraAgain() {
        if (!SessionCameraManager.isSessionExist(uidCam)) {
//            isReconnect = true
            hasDataVideo = false
            startSeekEvent = false
            connectCameraWithNewSession(idCam, uidCam, passCam)
        }
    }

    /**
     * Set playback mode
     */
    private fun setPlaybackMode() {
        vnttCamManager.setPlaybackMode(true)
    }



    /**
     * B1: Khởi tạo camera manager
     * - Nếu trong com.example.cameraplayback.manager.SessionCameraManager đã tồn tại uid, thì lấy camera manager để sử dụng luôn
     * - Nếu chưa tồn tại uid, khởi tạo camera manager mới, và add vào com.example.cameraplayback.manager.SessionCameraManager
     *
     * B2: Observe camera state
     */
    private fun initializeCameraManager(id: Int, uid: String, pass: String) {
        if (SessionCameraManager.isSessionExist(uid)) {
            SessionCameraManager.getCameraManagerByUID("VNTTA-017631-MVEGE")?.let { camManager ->
                vnttCamManager = camManager
            }
        } else {
            connectCameraWithNewSession(id, "VNTTA-017631-MVEGE", "02iRqCkk")
        }

        observeCameraState()
    }

    /**
     * Observe camera state from camera manager
     */
    private fun observeCameraState() {
        compositeDisposable.add(
            vnttCamManager.observeCameraState()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe({ state ->
                    Log.e("observeCameraState","Camera name: ${getDataCamera().name}, uid: ${getDataCamera().uid} --> ${state.value}")

                    if (state == Constant.CameraState.CameraSleep || state == Constant.CameraState.CameraOnline) {
                        // Khi connect camera trả về status = 0 -> Camera online -> Thêm cam vào Session Manager
                        SessionCameraManager.addCamera(uidCam, vnttCamManager)
                    }

                    // Đối với state online: Cần đợi đến thời điểm video data stream trả về mới set state online
                    // Các trường hợp còn lại thì set luôn
                    if (state != Constant.CameraState.CameraOnline) {
                        setCameraState(state)
                    }

                    processCameraState(state)
                }, {
                    Log.e("observeCameraState","Observe camera state failed: ${it.message}")
                })
        )


    }

    /**
     * Xử lý các logic tương ứng với từng state camera
     */
    private fun processCameraState(state: Constant.CameraState) {
        when (state) {
            Constant.CameraState.CameraOnline,
            Constant.CameraState.CameraSleep -> {
                if (!isCameraOnline) {
                    isCameraOnline = true
                    observeCameraInfo()
                    observeListVideoPlayback()
                    observeCommandSet()
                    observeDataStream()
                    vnttCamManager.getDeviceInfo()
                }
                getListVideoPlaybackFromCamera(startDay)
            }

            Constant.CameraState.CameraLossConnection -> {
//                isReconnect = true
//                hasDataVideo = false
            }

            Constant.CameraState.CameraOffline -> {
//                disconnectAndRemoveCameraFromListSession()
            }

            else -> {}
        }
    }

    /**
     * Lấy danh sách video playback sd card của 1 ngày từ camera
     * @param startTime: thời gian đầu ngày (00:00:00) của ngày được chọn
     */
    private fun getListVideoPlaybackFromCamera(startTime: Long) {
        vnttCamManager.getListVideoPlayBackSdCard(startTime)
    }

    /**
     * Observe data stream of playback sdcard
     * Audio stream, video stream, timestamp stream
     */
    private fun observeDataStream() {
        if (disposableVideoStream == null) {
            disposableVideoStream = vnttCamManager.observePlaybackVideoStream()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.computation())
                .subscribe { videoData ->
                    if (hasDataVideo) {
                        _dataVideoStream.onNext(videoData)
                    }

                    if (!hasDataVideo && !startSeekEvent) {
                        // Nếu bắt đầu có data của video trả về và không phải là sự kiện seek video
                        // ==> Set playback state là playing
                        hasDataVideo = true
                        isPaused = false
//                        isReconnect = false

                        getQualityVideo(videoData.widthData, videoData.heightData)
                        setPlaybackStateUI(Constant.PlaybackSdCardStateUI.PLAYING)
                    }
                }
        }

        if (disposableAudioStream == null) {
            disposableAudioStream = vnttCamManager.observePlaybackAudioStream()
                .subscribeOn(schedulerProvider.newThread())
                .observeOn(schedulerProvider.newThread())
                .doOnNext { dataAudio ->
                    // Nếu đang trong chế độ record, thì tiến hành ghi data vào file
//                    writeData(dataAudio, true)
                }
                .subscribe { audioData ->
                    if (!startSeekEvent) {
//                        audioTrack.write(
//                            audioData,
//                            0,
//                            audioData.size
//                        )
                    }
                }
        }

        if (disposableTimeStamp == null) {
            disposableTimeStamp = vnttCamManager.observeTimestampPlayback()
                .subscribeOn(schedulerProvider.newThread())
                .observeOn(schedulerProvider.newThread())
                .subscribe { offset ->
                    if (!startSeekEvent) {
                        currentFilePlay?.let { file ->
                            val timestamp = convertSecondToMillis(file.timestamp)

                            if (ceil((offset / 1000.0)) >= file.duration) {
                                // Đã play hết file hiện tại ==> Next sang file tiếp theo
                                processNextFile()
                            }

                            _cursorTimebar.postValue(Pair(true, timestamp + offset))
                        }
                    }
                }
        }

        disposableVideoStream?.let {
            compositeDisposable.delete(it)
            compositeDisposable.add(it)
        }

        disposableAudioStream?.let {
            compositeDisposable.delete(it)
            compositeDisposable.add(it)
        }

        disposableTimeStamp?.let {
            compositeDisposable.delete(it)
            compositeDisposable.add(it)
        }
    }

    fun convertSecondToMillis(time: Int): Long {
        return TimeUnit.SECONDS.toMillis(time.toLong())
    }

    /**
     * Set state play video playback
     */
    private fun setPlaybackStateUI(state: Constant.PlaybackSdCardStateUI) {
        _playbackState.postValue(state)
    }

    /**
     * Lấy chất lượng của video playback đang phát
     * SD: 640 x 360
     * FHD: 1920 x 1080
     */
    private fun getQualityVideo(widthData: Int, heightData: Int) {
        currentFilePlay?.let { file ->
            if (!StringUtils.equals(file.name, currentFileName)) {
                currentFileName = file.name.toString()

                if (widthData == 1920 && heightData == 1080) {
                    // FHD
                    _videoRecordQuality.postValue(Constant.ConfigCamera.VNPT_CAMERA_RECORD_RESOLUTION_FHD)
                } else if (widthData == 640 && heightData == 360) {
                    // SD
                    _videoRecordQuality.postValue(Constant.ConfigCamera.VNPT_CAMERA_RECORD_RESOLUTION_SD)
                } else {
                    // Nếu không lấy được độ phân giải từ video của cam thì gọi lệnh xuống cam để lấy config
                    observeVideoRecordQuality()
                    getVideoRecordQualityValue()
                }
            }
        }
    }

    /**
     * Set timestamp vào thanh timeline, sau đó play video playback
     * @param dataOfDay: danh sách model chứa thông tin video playback của ngày hiện tại được chọn
     */
    private fun setDataToTimelineAndPlayFile(
        dataOfDay: ArrayList<PlaybackFileModel>
    ) {
        compositeDisposable.add(
            Observable.create<ArrayList<ColorData>> { emitter ->
                val listColorData = ArrayList<ColorData>()
                var start: Long
                var end: Long
                val color = resourcesService.getColor(R.color.orange)
                dataOfDay.forEach { playbackFile ->
                    start = convertSecondToMillis(playbackFile.timestamp)
                    end = convertSecondToMillis(playbackFile.timestamp) + convertSecondToMillis(
                        playbackFile.duration
                    )
                    listColorData.add(
                        ColorData(start, end, color)
                    )

                }
                Log.d("ducpa", "listColorData: $listColorData")
                emitter.onNext(listColorData)
            }.subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe { colorData ->
                    _colorDataTimeline.postValue(colorData)
                   playVideoPlayback(eventToPlay)
                }
        )
    }

    /**
     * Observe video record quality
     */
    private fun observeVideoRecordQuality() {
        compositeDisposable.add(
            vnttCamManager.observeVideoQualityRecord()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe({ quality ->
                    _videoRecordQuality.value = quality
                }, {
                    Log.e("observeVideoRecordQuality","Observe video record quality failed")
                })
        )
    }

    /**
     * Get video record quality value
     */
    private fun getVideoRecordQualityValue() {
        vnttCamManager.getVideoRecordQuality()
    }

    /**
     * Observe commandset from camera
     */
    private fun observeCommandSet() {
        compositeDisposable.add(
            vnttCamManager.observeCommandSet()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe { commandSet ->
                    commandSet?.first?.let { command ->
                        val status = commandSet.second
//                        setStateByCommandSet(command, status)
                    }
                }
        )
    }



    /**
     * Observe Camera Information
     */
    private fun observeCameraInfo() {
        compositeDisposable.add(
            vnttCamManager.observeDeviceInfo()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe { deviceInfo ->
                    _cameraInfo.value = deviceInfo
                }
        )
    }

    /**
     * Set camera state
     */
    private fun setCameraState(state: Constant.CameraState) {
        _cameraState.postValue(state)
    }

    /**
     * Connect to camera with new session
     */
    private fun connectCameraWithNewSession(id: Int, uid: String, pass: String) {
        vnttCamManager.apply {
            createCameraManager(uid, pass)
            initCallback()
            receiveVideo(false)
            receiveAudio(false)
            connectToCamera(id)
        }
    }

    /**
     * Lấy thông tin id, uid, password camera
     */
    private fun getIdCamera(camera: Device) {
        camera.id?.let { idCam = it }
        camera.uid?.let { uidCam = "VNTTA-017631-MVEGE" }
        decryptPassword(camera)?.let { passCam = "02iRqCkk" }
    }

    private fun decryptPassword(camera: Device): String? {
        return decrypt(camera.password)
    }





    fun getDataCamera() : Device {
        return devicesCamera ?: Device(name = "Camera")
    }

    fun prepareAndSetUpCamera() {
//        getDetailDevices(getDataCamera())
    }

    fun fetchDevice(deviceId: Int) {
        viewModelScope.launch {
            try {
                val device = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getDevice(deviceId)
                }
                _device.postValue(device)
            } catch (e: Exception) {
                _error.postValue(e.message)
            }
        }
    }

    /**
     * Observe list video playback sd card
     * Trừ đi 1s trong duration của mỗi video, bởi vì trong quá trình next file, sẽ có lúc offset cuối cùng trả về sẽ < duration.
     * Vì vậy cần trừ đi 1s để đảm bảo offset cuối cùng luôn >= duration. Lúc đó mới next file được
     */
    private fun observeListVideoPlayback() {
        compositeDisposable.add(
            vnttCamManager.observeListVideoPlayback()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .map { data ->
                    data.forEach { _, listVideoInDay ->
                        // Phải là isNullOrEmpty() -> không được dùng isNotEmpty()
                        if (!listVideoInDay.isNullOrEmpty()) {
                            listVideoInDay.forEach { file ->
                                val decreaseDuration = file.duration - 1
                                file.duration = decreaseDuration
                            }
                            Log.d("ducpa", "listVideoInDay: $listVideoInDay")
                        }
                    }
                    return@map data
                }
                .subscribe({ playbackData ->
                    Log.d("ducpa", "playbackData1: $playbackData")
                    processPlaybackData(playbackData)
                }, {
                })
        )
    }

    /**
     * Xử lý danh sách video playback lấy được từ camera sau khi query:
     *    - Nếu danh sách video của ngày hiện tại rỗng:
     *          + Trường hợp 1: Khi người dùng chọn ngày, mà ngày đó rỗng, thì show luôn thông báo "không có dữ liệu"
     *          + Trường hợp 2: Thời điểm đầu tiên vào xem playback, nếu ngày hiện tại không có video
     *                          --> Tiếp tục query từng ngày trước đó. Nếu ngày trước đó có dữ liệu thì query và play ngày đó, nếu vẫn empty thì tiếp tục query.
     *                              Lặp lại cho đến khi hết 30 ngày
     *          + Trường hợp 3: Tất cả các ngày đều không có video playback -> show thông báo
     *    - Nếu có data của ngày hiện tại:
     *          + Set timestamp vào thanh timeline
     *
     * @param playbackData: danh sách video playback lấy từ cam
     */
    private fun processPlaybackData(
        playbackData: LongSparseArray<ArrayList<PlaybackFileModel>>
    ) {
        val timeKeyIndex = playbackData.indexOfKey(startDay)
        Log.d("ducpa", "timeKeyIndex: $timeKeyIndex")
        val timeKey = playbackData.keyAt(timeKeyIndex)
        Log.d("ducpa", "timeKey: $timeKey")
        val dataOfDay = playbackData.get(timeKey)
        Log.d("ducpa", "dataOfDay: $dataOfDay")
        if (dataOfDay.isNullOrEmpty()) {
            listPlayback.put(timeKey, arrayListOf())
            currentFilePlay = PlaybackFileModel(Constant.EMPTY_STRING, Constant.EMPTY_STRING, 0, 0)
            Log.d("ducpa", "eventToPlay: $eventToPlay $listPlayback")
            when (eventToPlay) {
                Constant.PlayPlaybackFileEvent.SELECT_DAY -> {
                    setPlaybackStateUI(Constant.PlaybackSdCardStateUI.EMPTY_FILE)
                }
                Constant.PlayPlaybackFileEvent.FIRST_TIME -> {}
                Constant.PlayPlaybackFileEvent.SEEK -> {
                    processSeekVideoInEmptyFile()
                }
                Constant.PlayPlaybackFileEvent.NEXT_FILE -> {}
                Constant.PlayPlaybackFileEvent.SCAN_PREVIOUS -> {}
                Constant.PlayPlaybackFileEvent.SCAN_NEXT -> {}
                Constant.PlayPlaybackFileEvent.NEXT_DAY -> {}
                Constant.PlayPlaybackFileEvent.PAUSE -> {}
                Constant.PlayPlaybackFileEvent.NOTIFICATION -> {}
            }

        } else {
            Log.d("ducpa", "listPlaybackData2: $listPlayback")
            listPlayback.put(timeKey, dataOfDay)
            emptyFileAllDay = false
//            playVideoPlayback(eventToPlay)
            setDataToTimelineAndPlayFile(dataOfDay)
        }
    }

    private fun processSeekVideoInEmptyFile() {
        if (emptyFileAllDay) {
            setPlaybackStateUI(Constant.PlaybackSdCardStateUI.EMPTY_FILE_ALL_DAY)
        } else {
            scanPreviousDay()
        }
    }

    private fun scanPreviousDay() {
        _dateList.value?.let { dates ->
            val indexDay = dates.indexOf(startTimeOfSelectedDay)
            if (indexDay != 0) {
                // Ngày hiện tại -> bắt đầu quét về quá khứ
                startTimeOfSelectedDay = dates[indexDay - 1]
                setEventToPlay(Constant.PlayPlaybackFileEvent.SCAN_PREVIOUS)
                setPlaybackStateUI(Constant.PlaybackSdCardStateUI.SCAN)
                //startQueryFileAfterScan()
            }
        }
    }




    /**
     * Play video
     */
    private fun playVideoPlayback(event: Constant.PlayPlaybackFileEvent) {
        setEventToPlay(event)
        Log.d("ducpa", "event: $event")
        when (event) {
            Constant.PlayPlaybackFileEvent.FIRST_TIME -> {
                Log.d("ducpa", "seekTimeValue FIRST_TIME: $seekTimeValue ")
                playLastFileInDay()
            }

            Constant.PlayPlaybackFileEvent.SEEK -> {
                Log.d("ducpa", "seekTimeValue SEEK: $seekTimeValue ")
                processSeekVideo(seekTimeValue)
            }
//
            Constant.PlayPlaybackFileEvent.NOTIFICATION -> {
                processSeekVideo(seekTimeValue)
            }
//            NEXT_FILE -> {
//                // Stop phiên playback của file hiện tại --> đợi command SDK_START_STOP_PLAYBACK_COMMAND trả về và play file mới
//                stopPlayback()
//            }
//
//            SELECT_DAY -> {
//                // Chọn ngày xem playback: Sẽ focus vào thời điểm giữa ngày (12h)
//                // Tương tự như lúc tua file với seek time = 12:00:00
//
//                processSeekVideo(
//                    getMidDayTime(startTimeOfSelectedDay)
//                )
//            }
//
            Constant.PlayPlaybackFileEvent.SCAN_PREVIOUS -> {
                // Play file cuối cùng của ngày khi scan ra ngày có dữ liệu
                playLastFileInDay()
            }
//
            Constant.PlayPlaybackFileEvent.SCAN_NEXT -> {
                // Play file đầu tiên của ngày khi scan ra ngày có dữ liệu
                isNextDay = false
                playFirstFileInDay()
            }
//
//            NEXT_DAY -> {
//                stopPlayback()
//            }

            else -> {}
        }
    }

    /**
     * Xử lý thời gian tua video playback để lấy ra được file cần play
     * Emit Triple<PlaybackFileModel, Int, Boolean>>
     *     - PlaybackFileModel: file để play
     *     - Int: Timestamp để bắt đầu play
     *     - Boolean: Biến để quyết định next sang ngày mới hay play file cuối cùng của ngày
     *          + true: Play file cuối cùng của ngày
     *          + false: Next sang ngày mới gần nhất và play file đầu tiên của ngày đó
     */
    private fun processSeekVideo(seekTime: Long) {
        compositeDisposable.add(
            Single.create<Triple<PlaybackFileModel, Int, Boolean>> { emitter ->
                val listPlaybackInDay = getListFilePlaybackInDay(startDay)
                val seekTimeSecond = convertMillisToSecond(seekTime)
                for (index in 0 until listPlaybackInDay.size) {
                    val file = listPlaybackInDay[index]
                    val startTime = file.timestamp
                    val endTime = file.timestamp + file.duration
                    if (convertMillisToSecond(seekTime) == convertMillisToSecond(
                            getEndOfDay(
                                startDay
                            )
                        )
                    ) {
                        // Nếu thời điểm seek là 23:59:59 thì next luôn sang ngày hôm sau
                        emitter.onSuccess(
                            Triple(file, startTime, false)
                        )
                        break
                    } else if (index == 0 && seekTimeSecond < startTime) {
                        // Nếu seekTime nhỏ hơn startTime của file đầu tiên trong danh sách
                        // ==> Play file đầu tiên trong danh sách với seekTime là startTime của file
                        if (eventToPlay == Constant.PlayPlaybackFileEvent.NOTIFICATION) setPlaybackStateUI(Constant.PlaybackSdCardStateUI.EMPTY_FILE_NOTIFICATION)
                        emitter.onSuccess(
                            Triple(file, startTime, true)
                        )
                        break
                    } else if ((index == listPlaybackInDay.size - 1) && seekTimeSecond >= endTime) {
                        // Nếu seekTime lớn hơn hoặc bằng endTime của file cuối cùng trong danh sách
                        // TH1: Nếu đang trong event chọn ngày để xem playback hoặc đang đứng ở ngày hiện tại và seek qua file cuối cùng
                        //      ==> Play file cuối cùng trong danh sách với seekTime là startTime của file
                        // TH2: Các trường hợp còn lại, thì tự động next sang ngày tiếp theo và play file đầu tiên của ngày đó
                        if (eventToPlay == Constant.PlayPlaybackFileEvent.NOTIFICATION) setPlaybackStateUI(Constant.PlaybackSdCardStateUI.EMPTY_FILE_NOTIFICATION)
                        if (eventToPlay == Constant.PlayPlaybackFileEvent.SELECT_DAY || isDay) {
                            emitter.onSuccess(
                                Triple(file, startTime, true)
                            )
                        } else {
                            emitter.onSuccess(
                                Triple(file, startTime, eventToPlay == Constant.PlayPlaybackFileEvent.NOTIFICATION)
                            )
                        }
                        break
                    } else if (seekTimeSecond in startTime until endTime) {
                        // Nếu seekTime nằm trong khoảng startTime và endTime của 1 file (>= startTime và < endTime)
                        // ==> Play file đó với seekTime
                        emitter.onSuccess(
                            Triple(file, seekTimeSecond, true)
                        )
                        break
                    } else {
                        // Trường hợp tua đến thời điểm giữa 2 file nhưng thời điểm đó không có dữ liệu playback
                        // ==> play file gần nhất của thời điểm seekTime
                        if (index < listPlaybackInDay.size - 1) {
                            val nextFile =
                                listPlaybackInDay[index + 1]     // File kế tiếp của file hiện tại
                            val startTimeNextFile = nextFile.timestamp

                            if (seekTimeSecond in endTime..startTimeNextFile) {
                                if (eventToPlay == Constant.PlayPlaybackFileEvent.NOTIFICATION) setPlaybackStateUI(Constant.PlaybackSdCardStateUI.EMPTY_FILE_NOTIFICATION)

                                emitter.onSuccess(
                                    Triple(nextFile, startTimeNextFile, true)
                                )
                                break
                            }
                        }
                    }
                }

            }.subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe { dataPlay ->
                    if (dataPlay.third) {
                        // Play file cuối cùng của ngày
                        playFileWithSeekEvent(
                            dataPlay.first,
                            dataPlay.second
                        )
                    } else {
                        // Next sang ngày gần nhất và play file đầu tiên của ngày đó
//                        scanNextDay()
                    }
                }
        )
    }

    /**
     * Play file trong trường hợp tua video bằng thanh timeline
     */
    private fun playFileWithSeekEvent(file: PlaybackFileModel, time: Int) {
        if (isPlayLastFileComplete) {
            isPlayLastFileComplete = false
            setPlaybackStateUI(Constant.PlaybackSdCardStateUI.PLAYING)
        }
        currentFilePlay = file
        playFile(file, time)
    }

    private fun convertMillisToSecond(time: Long): Int {
        return TimeUnit.MILLISECONDS.toSeconds(time).toInt()
    }


    fun setEventToPlay(event: Constant.PlayPlaybackFileEvent) {
        eventToPlay = event
    }

    private fun playLastFileInDay() {
        Log.d("ducpa", "listFile: ${getListFilePlaybackInDay(startDay)}")
        val listFile = getListFilePlaybackInDay(startDay)
        currentFilePlay = listFile[listFile.size - 1]

        currentFilePlay?.let { file ->
            playFile(file, file.timestamp)
        }
    }

    /**
     * Get list file playback trong ngày
     */
    private fun getListFilePlaybackInDay(
        timeKey: Long
    ): ArrayList<PlaybackFileModel> {
        Log.d("ducpa", "listPlayback: ${listPlayback.get(timeKey)}")
        // Lấy danh sách video theo ngày, với timeKey là ngày được chọn
        return listPlayback.get(timeKey)
    }

    /**
     * Get the starting point of the current day（00:00:00）
     *
     * @param currentTime
     * @return
     */
    fun getStartOfDay(currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime)
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.timeInMillis
    }

    /**
     * Bắt đầu tua
     */
    fun seekTimePlaybackFile() {
        startSeekEvent = true
        hasDataVideo = false
        setEventToPlay(Constant.PlayPlaybackFileEvent.SEEK)
        setPlaybackStateUI(Constant.PlaybackSdCardStateUI.SEEK)
        resetPlaybackTime()
    }

    private fun resetPlaybackTime() {
        _cursorTimebar.value = Pair(false, 0)
    }


    /**
     * Bắt đầu quá trình seekvideo
     *      - Nếu đang đứng ở ngày hiện tại và biến emptyFileAllDay = true thì query lên cam để lấy dữ liệu mới nhất
     *      - Nếu tất cả các ngày không có dữ liệu thì show luôn thông báo
     *      - Nếu đang phát playback thì stop lại video và đợi command start_stop để tiếp tục xử lý
     *      - Nếu emptyFileAllDay = false, đứng ở ngày không có dữ liệu và seek:
     *          + Đầu tiên phải quét đến tương lai và play file đầu tiên của ngày đó
     *          + Nếu quét đến tương lai và không có ngày nào để play thì quét về quá khứ và play file cuối cùng của ngày đó
     */
    fun startSeekVideo() {
        if (emptyFileAllDay) {
            if (_dateList.value?.indexOf(startDay) == 30) {
                // Nếu là ngày hiện tại thì query lên cam để lấy dữ liệu mới nhất
                // ngày hiện tại là ngày có index = 30 trong dateList
                getListVideoPlaybackFromCamera(startDay)
            } else {
                setPlaybackStateUI(Constant.PlaybackSdCardStateUI.EMPTY_FILE_ALL_DAY)
            }
        } else if (currentFilePlay?.name == Constant.EMPTY_STRING) {
            // Nếu current file = EMPTY_STRING ==> Đang đứng ở ngày không có dữ liệu
            scanFileAfterSeekEmptyDay()
        } else {
            if (isPaused) {
                // Trong trường hợp video đã được pause rồi, thì gọi luôn hàm play với event là SEEK
                playVideoPlayback(Constant.PlayPlaybackFileEvent.SEEK)
            } else {
                stopPlayback()
            }
        }
    }

    /**
     * Stop playback
     * @param byUser: true - người dùng chủ động stop playback bằng cách nhấn nút play/pause
     *                false - tự động stop playback để next file, seek, next day
     */
    private fun stopPlayback(byUser: Boolean = false) {
        compositeDisposable.add(
            vnttCamManager.stopPlayback()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe({
                    if (!byUser) {
                        resetPlaybackTime()
                    }
                }, {
                })
        )
    }

    /**
     * Đứng ở ngày không có dữ liệu và seek
     * - Đứng ở ngày hiện tại và ngày đó chưa có dữ liệu, khi seek file cần query lại ngày đó để lấy dữ liệu mới nhất
     * - Quét đến tương lai, nếu tương lai không có dữ liệu thì quay về quá khứ
     * - Khi quét đến tương lai, gặp ngày có dữ liệu thì dừng luôn và play file đầu tiên của ngày đó
     * - Khi quét về quá khứ, gặp ngày có dữ liệu thì dừng luôn và play file cuối cùng của ngày đó
     */
    private fun scanFileAfterSeekEmptyDay() {
        _dateList.value?.let { dates ->
//            val indexDay = dates.indexOf(startDay)
//
//            if (indexDay != 30) {
//                scanNextDay()
//            } else {
                // Nếu đang là ngày hiện tại -> seek file và cần query lấy data mới nhất
                getListVideoPlaybackFromCamera(startDay)
//            }
        }

    }




    /**
     * Play video playback
     * @param file: file được play
     * @param timestamp: timestamp của file playback đang được play (đơn vị: giây)
     */
    private fun playFile(file: PlaybackFileModel, timestamp: Int) {
        compositeDisposable.add(
            vnttCamManager.playBackVideoStart(file, timestamp)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe({}, {})
        )
    }

    /**
     * Play file đầu tiên của ngày
     */
    private fun playFirstFileInDay() {
        val listFile = getListFilePlaybackInDay(startDay)
        currentFilePlay = listFile[5]
        currentFilePlay?.let { file ->
            playFile(file, file.timestamp)
            Log.d("ducpa", "currentFilePlay: $currentFilePlay")
        }
    }

    /**
     * Xử lý quá trình next file khi đã play xong file hiện tại:
     *      - Nếu file đang play là file cuối cùng:
     *          + Nếu ngày đó là ngày cuối cùng trong danh sách 30 ngày query từ đầu --> Finish --> Kết thúc xem playback
     *          + Nếu chưa là ngày cuối cùng: Thực hiện next sang ngày tiếp theo
     *      - Nếu chưa phải là file cuối cùng: Thực hiện next sang file kế tiếp
     *
     * Emit ra giá trị Pair<PlaybackFileModel, Boolean>, trong đó biến boolean kiểm tra xem đó đã phải là file cuối cùng trong ngày hay chưa
     */
    private fun processNextFile() {
        if (!isNextFile) {
            isNextFile = true
            startSeekEvent = true

            compositeDisposable.add(
                Single.create<Pair<PlaybackFileModel, Boolean>> { emitter ->
                    val listPlaybackInDay = getListFilePlaybackInDay(startTimeOfSelectedDay)

                    for (index in 0 until listPlaybackInDay.size) {
                        val file = listPlaybackInDay[index]

                        if (file.timestamp == currentFilePlay?.timestamp) {
                            if (index == listPlaybackInDay.size - 1) {
                                // File đang play là file cuối cùng
                                emitter.onSuccess(Pair(currentFilePlay!!, true))
                            } else {
                                // Chưa phải là file cuối cùng: Thực hiện next sang file kế tiếp
                                val nextFile = listPlaybackInDay[index + 1]
                                emitter.onSuccess(Pair(nextFile, false))
                                break
                            }
                        }
                    }

                }.subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribe { data ->
                        if (data.second) {
                            // Nếu file đã là file cuối cùng trong ngày
                            // --> Next sang ngày mới gần nhất và play file đầu tiên của ngày đó
                            processNextDay()
                        } else {
                            // Update UI tương ứng với trạng thái next file
                            setPlaybackStateUI(Constant.PlaybackSdCardStateUI.NEXT_FILE)

                            currentFilePlay = data.first
                            playVideoPlayback(Constant.PlayPlaybackFileEvent.NEXT_FILE)
                        }
                    }
            )
        }
    }

    /**
     * Next sang ngày mới và play file đầu tiên của ngày đó
     * Nếu đang đứng ở ngày hiện tại và đã play hết tất cả các file thì show luôn thông báo
     */
    private fun processNextDay() {
        if (isDay) {
            isNextFile = false
            isPlayLastFileComplete = true
            setPlaybackStateUI(Constant.PlaybackSdCardStateUI.COMPLETE)
        } else {
            startSeekEvent = true
            hasDataVideo = false

            setPlaybackStateUI(Constant.PlaybackSdCardStateUI.NEXT_DAY)
            playVideoPlayback(Constant.PlayPlaybackFileEvent.NEXT_DAY)
        }
    }

}
