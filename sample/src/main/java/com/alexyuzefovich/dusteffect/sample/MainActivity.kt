package com.alexyuzefovich.dusteffect.sample

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexyuzefovich.dusteffect.DustEffectRenderer
import com.alexyuzefovich.dusteffect.sample.adapter.AndyAdapter
import com.alexyuzefovich.dusteffect.sample.model.Andy
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var contentView: View

    private val andyAdapter: AndyAdapter by lazy {
        AndyAdapter { position, view ->
            // For API 14-20 content is not full-screen (behind status bar),
            // so we include status bar height as additional offset.
            val contentViewLocation = intArrayOf(0, 0).apply {
                contentView.getLocationOnScreen(this)
                for (i in indices) {
                    this[i] *= -1
                }
            }
            dustEffectRenderer.composeView(view, contentViewLocation)

            val updatedAndyList = andyAdapter.currentList.toMutableList().apply {
                removeAt(position)
            }
            andyAdapter.submitList(updatedAndyList)
        }
    }

    private val dustEffectRenderer: DustEffectRenderer by lazy {
        DustEffectRenderer(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        contentView = createContentView()
        setContentView(contentView)
        fillAndyList()
    }

    private fun createContentView(): View {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val defaultContentOffset = resources.getDimensionPixelOffset(R.dimen.content_offset)
        val fabSize = resources.getDimensionPixelOffset(R.dimen.fab_size)

        val rv = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = andyAdapter

            updatePadding(
                top = defaultContentOffset,
                bottom = fabSize + 2 * defaultContentOffset
            )
            clipToPadding = false
        }.also {
            root.addView(it)
        }

        val fab = FloatingActionButton(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                updateMargins(
                    right = defaultContentOffset,
                    bottom = defaultContentOffset
                )
            }
            setImageResource(R.drawable.ic_restart_black_24dp)
            setOnClickListener {
                fillAndyList()
            }
        }.also {
            root.addView(it)
        }

        GLSurfaceView(this).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setEGLContextClientVersion(2);
            setZOrderOnTop(true);
            setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            holder.setFormat(PixelFormat.RGBA_8888);
            setRenderer(dustEffectRenderer);
        }.also {
            root.addView(it)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            rv.updatePadding(
                top = topInset + defaultContentOffset,
                bottom = bottomInset + fabSize + 2 * defaultContentOffset
            )

            fab.updateLayoutParams<FrameLayout.LayoutParams> {
                updateMargins(bottom = bottomInset + defaultContentOffset)
            }

            insets
        }

        return root
    }

    private fun fillAndyList() {
        val andyList = listOf(
            Andy(R.drawable.image_andy_1, Gravity.END),
            Andy(R.drawable.image_andy_2, Gravity.CENTER_HORIZONTAL),
            Andy(R.drawable.image_andy_3, Gravity.START),
            Andy(R.drawable.image_andy_4, Gravity.CENTER_HORIZONTAL),
            Andy(R.drawable.image_andy_5, Gravity.END)
        )
        andyAdapter.submitList(andyList)
    }

}