package com.alimrasid.prototype1.ui.home.alphabet

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.data.alphabet.Alphabet
import com.bumptech.glide.Glide


class AlphabetAdapter(private val alphabetList: List<Alphabet>) :
    RecyclerView.Adapter<AlphabetAdapter.AlphabetViewModel>() {

    class AlphabetViewModel(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvChar: TextView = itemView.findViewById(R.id.tvWord)
        val imgSign: ImageView = itemView.findViewById(R.id.imgWord)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlphabetViewModel {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.word_result_item, parent, false)
        return AlphabetViewModel(view)
    }

    override fun onBindViewHolder(holder: AlphabetViewModel, position: Int) {
        val alphabet = alphabetList[position]
        val context = holder.itemView.context

        holder.tvChar.text = alphabet.alphabet.toString()

        if (alphabet.alphabet == 'r' || alphabet.alphabet == 'j' || alphabet.alphabet == 'y') {
            Glide.with(context)
                .asGif()
                .load(alphabet.sign)
                .into(holder.imgSign)
        } else {
            Glide.with(context)
                .load(alphabet.sign)
                .into(holder.imgSign)
        }

        holder.itemView.layoutParams.width = getScreenWidth(context) / 2
    }

    override fun getItemCount(): Int {
        return alphabetList.size
    }

    private fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }
}
