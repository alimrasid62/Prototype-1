package com.alimrasid.prototype1.ui.home.alphabet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.data.Data
import com.alimrasid.prototype1.data.alphabet.Alphabet

class AlphabetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alphabet)

        val rvAlphabet = findViewById<RecyclerView>(R.id.rvAlphabet)

        val alphabetList: ArrayList<Alphabet> = Data().getAlphabetData()
        val adapter = AlphabetAdapter(alphabetList)
        rvAlphabet.adapter = adapter
        rvAlphabet.setHasFixedSize(true)
        rvAlphabet.layoutManager = GridLayoutManager(this, 2)
    }
}