package com.example.cameraplayback.utils.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.cameraplayback.databinding.ItemEmptyBinding
import com.example.cameraplayback.ui.theme.dpToPx
import com.example.cameraplayback.ui.theme.getScreenHeight
import com.example.cameraplayback.ui.theme.hide
import com.example.cameraplayback.ui.theme.setHeight
import com.example.cameraplayback.ui.theme.setWidthHeightToView

abstract class BaseViewHolder<T : ViewBinding>(open val binding: T) : RecyclerView.ViewHolder(binding.root) {
    abstract fun bindData(position: Int)
}

class EmptyViewHolder : BaseViewHolder<ItemEmptyBinding> {

    constructor(itemEmptyBinding: ItemEmptyBinding) : super(itemEmptyBinding)

    constructor(itemEmptyBinding: ItemEmptyBinding, contentEmpty: String, imageEmpty: Int) : super(itemEmptyBinding) {
        binding.tvErrorContent.text = contentEmpty
        if (imageEmpty == 0) {
            binding.ivEmpty.hide()
        } else {
            binding.ivEmpty.visibility= View.INVISIBLE
        }
    }
    constructor(itemEmptyBinding: ItemEmptyBinding, contentEmpty: String) : super(itemEmptyBinding) {
        binding.tvErrorContent.text = contentEmpty
        binding.root.setHeight(getScreenHeight()- dpToPx(56F))
    }


    constructor(itemEmptyBinding: ItemEmptyBinding, contentEmpty: String, imageEmpty: Int, sizeIcon: Float) : super(itemEmptyBinding) {
        binding.tvErrorContent.text = contentEmpty
        binding.ivEmpty.apply {

            setWidthHeightToView(dpToPx(sizeIcon), dpToPx(sizeIcon))
        }
    }

    override fun bindData(position: Int) {

    }

}