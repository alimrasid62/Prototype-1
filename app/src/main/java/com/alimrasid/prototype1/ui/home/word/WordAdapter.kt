package com.alimrasid.prototype1.ui.home.word

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.data.word.Word

class WordAdapter(private val words: ArrayList<Word>) :
    RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private var onButtonClicked: OnButtonClicked? = null

    fun setOnButtonClicked(listener: OnButtonClicked) {
        this.onButtonClicked = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.word_item, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        holder.btnWord.text = word.word
        holder.btnWord.setOnClickListener {
            onButtonClicked?.onWordClicked(word)
        }
    }

    override fun getItemCount(): Int {
        return words.size
    }

    class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnWord: Button = itemView.findViewById(R.id.btnWord)
    }

    interface OnButtonClicked {
        fun onWordClicked(word: Word)
    }
}

