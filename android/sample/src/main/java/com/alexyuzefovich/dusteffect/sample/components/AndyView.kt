package com.alexyuzefovich.dusteffect.sample.components

import android.content.Context
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.updateLayoutParams
import com.alexyuzefovich.dusteffect.sample.model.Andy
import kotlin.math.min

class AndyView(context: Context) : FrameLayout(context) {

    private val imageView: ImageView


    init {
        imageView = object : AppCompatImageView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
                val size = min(desiredWidth, 720)
                val sizeSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
                super.onMeasure(sizeSpec, sizeSpec)
            }
        }.also {
            addView(it)
        }
    }


    override fun setOnClickListener(l: OnClickListener?) {
        val wrapper = l?.let { original ->
            OnClickListener {
                original.onClick(imageView)
            }
        }
        super.setOnClickListener(wrapper)
    }

    fun bind(andy: Andy) {
        with(imageView) {
            setImageResource(andy.imageResId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            updateLayoutParams<LayoutParams> {
                gravity = andy.gravity
            }
        }
    }

}