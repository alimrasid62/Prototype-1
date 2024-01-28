package com.alimrasid.prototype1.ui.home.word

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.alimrasid.prototype1.R
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alimrasid.prototype1.data.Data
import com.alimrasid.prototype1.data.word.Word
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import javax.sql.DataSource

class WordActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter
    private lateinit var words: ArrayList<Word>
    private lateinit var imgWord: ImageView
    private lateinit var fabBack: FloatingActionButton
    private lateinit var tvCategory: TextView
    private lateinit var rvWord: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word)

        rvWord = findViewById(R.id.rvWord)
        imgWord = findViewById(R.id.imgWord)
        tvCategory = findViewById(R.id.tvCategory)
        fabBack = findViewById(R.id.fabBack)

        fabBack.setOnClickListener { onBackPressed() }

        words = Data().getWord()

        adapter = WordAdapter(words)
        adapter.setOnButtonClicked { word ->
            tvCategory.visibility = View.GONE
            Glide.with(this)
                .asGif()
                .load(word.gesture)
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.setLoopCount(1)
                        return false
                    }
                })
                .into(imgWord)
        }

        rvWord.adapter = adapter
        rvWord.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvWord.setHasFixedSize(true)
    }
}
