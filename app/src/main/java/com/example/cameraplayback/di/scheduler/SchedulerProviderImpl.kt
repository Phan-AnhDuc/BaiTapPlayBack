package com.example.cameraplayback.di.scheduler// File: com.example.cameraplayback.di.scheduler.SchedulerProvider.kt

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import vn.vnpt.ONEHome.di.component.scheduler.SchedulerProvider


class SchedulerProviderImpl : SchedulerProvider {
    override fun ui(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

    override fun computation(): Scheduler {
        return Schedulers.computation()
    }

    override fun io(): Scheduler {
        return Schedulers.io()
    }

    override fun newThread(): Scheduler {
        return Schedulers.newThread()
    }
}

