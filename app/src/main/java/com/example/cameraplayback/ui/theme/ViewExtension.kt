package com.example.cameraplayback.ui.theme

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding

fun View.enable() {
    this.isEnabled = true
}

fun View.disable() {
    this.isEnabled = false
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

inline fun <reified V : ViewBinding> ViewGroup.toViewBinding(): V {
    return V::class.java.getMethod(
        "inflate",
        LayoutInflater::class.java,
        ViewGroup::class.java,
        Boolean::class.java
    ).invoke(null, LayoutInflater.from(context), this, false) as V
}

fun View.setWidthView(width: Int) {
    val params: ViewGroup.LayoutParams = layoutParams
    params.width = width
    layoutParams = params
}


/**
 * Get colorCompat
 * @param color : Int
 * @return color :Int
 */
fun Context.getColorCompat(color: Int): Int {
    return ContextCompat.getColor(this, color)
}





/**
 * Set height view
 */
fun View.setHeight(height: Int) {
    val params: ViewGroup.LayoutParams = layoutParams
    params.height = height
    layoutParams = params
}

fun getScreenHeight(): Int {
    return Resources.getSystem().displayMetrics.heightPixels
}

fun dpToPx(dp: Float): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}

fun View.setWidthHeightToView(width: Int, height: Int) {
    val params: ViewGroup.LayoutParams = layoutParams
    params.height = height
    params.width = width
    layoutParams = params
}


