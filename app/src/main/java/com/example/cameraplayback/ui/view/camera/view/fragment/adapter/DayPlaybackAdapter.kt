package com.example.cameraplayback.ui.view.camera.view.fragment.adapter

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.util.Log
import android.view.ViewGroup
import com.example.cameraplayback.R
import com.example.cameraplayback.databinding.ItemDayCloudBinding
import com.example.cameraplayback.ui.theme.dpToPx
import com.example.cameraplayback.ui.theme.getColorCompat
import com.example.cameraplayback.ui.theme.setWidthView
import com.example.cameraplayback.ui.theme.toViewBinding
import com.example.cameraplayback.utils.base.BaseRecycleAdapter
import com.example.cameraplayback.utils.base.BaseViewHolder
import com.example.cameraplayback.utils.extension.getMidDayTime
import com.example.cameraplayback.utils.extension.getStartOfDay
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

class DayPlaybackAdapter @Inject constructor() : BaseRecycleAdapter<Long>() {
    private var selectedDayPosition = 0 // selected day position= 0
    private var canPickDay:Boolean = true  // can pick day or not
    private var tmpSelectDay: Long = 0      // Biến này để check xem có chọn ngày hiện tại hay không

    lateinit var onItemClick: (Long) -> Unit

    override fun setLoadingViewHolder(parent: ViewGroup): BaseViewHolder<*>? = null

    override fun setEmptyViewHolder(parent: ViewGroup): BaseViewHolder<*>? = null

    override fun setNormalViewHolder(parent: ViewGroup): BaseViewHolder<*> =
        DayCloudViewHolder(parent.toViewBinding())

    override fun setErrorViewHolder(parent: ViewGroup): BaseViewHolder<*>? = null

    override fun submitList(list: List<Long>) {
        super.submitList(list)
        selectedDayPosition = itemList.size - 1
    }

    fun setPickableDay(pickable: Boolean) {
        Log.d("ducpa", "nhảy vào setPickableDay")
        this.canPickDay = pickable
    }

    fun setTmpSelectDay(time: Long) {
        tmpSelectDay = getStartOfDay(time)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCheckedItem(currentTime: Long) {
        if (itemList.isEmpty() || selectedDayPosition == -1) return
        if (getStartOfDay(currentTime) != itemList[selectedDayPosition]) {
            selectedDayPosition = itemList.indexOf(getStartOfDay(currentTime))
            notifyDataSetChanged()
        }
    }

    fun selectedDayPosition(): Int {
        return selectedDayPosition
    }

    fun getDateByPosition(position: Int): String {
        if (itemList.isEmpty()) {
            val date = Date(System.currentTimeMillis())
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")

            return dateFormat.format(date)
        } else {
            return DateFormat.format("dd/MM/yyyy", Date(itemList[position])).toString()
        }
    }

    inner class DayCloudViewHolder(binding: ItemDayCloudBinding) :
        BaseViewHolder<ItemDayCloudBinding>(binding) {
        override fun bindData(position: Int) {
            val data = itemList[position]
            val dayMonth = DateFormat.format("dd/MM", Date(data))

            binding.apply {
                tvDay.apply {
                    setWidthView(dpToPx(60F))
                    setTextColor(
                        if (position == selectedDayPosition) itemView.context.getColorCompat(
                            R.color.white
                        ) else itemView.context.getColorCompat(R.color.grey_light)
                    )
//                    background =
//                        if (position == selectedDayPosition) itemView.context.getDrawableCompat(R.drawable.shape_coner_light_blue_radius_13dp) else itemView.context.getDrawableCompat(
//                            R.drawable.shape_bg_white_radius_13dp
//                        )
                    text = dayMonth
//                    safeClickWithRx {
//                        if (this@DayPlaybackAdapter::onItemClick.isInitialized) {
//                            if (canPickDay && tmpSelectDay != data) {
//                                tmpSelectDay = data
//                                setCheckedItem(data)
//                                onItemClick.invoke(getMidDayTime(data))
//                            }
//                        }
//                    }
                }
            }
        }

    }
}
