package com.alimrasid.prototype1.data

import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.data.alphabet.Alphabet
import com.alimrasid.prototype1.data.word.Word

class Data {

    private val alphabetData = charArrayOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z')
    private val signData = intArrayOf(
        R.drawable.a,
        R.drawable.b,
        R.drawable.c,
        R.drawable.d,
        R.drawable.e,
        R.drawable.f,
        R.drawable.g,
        R.drawable.h,
        R.drawable.i,
        R.drawable.j,
        R.drawable.k,
        R.drawable.l,
        R.drawable.m,
        R.drawable.n,
        R.drawable.o,
        R.drawable.p,
        R.drawable.q,
        R.drawable.r,
        R.drawable.s,
        R.drawable.t,
        R.drawable.u,
        R.drawable.v,
        R.drawable.w,
        R.drawable.x,
        R.drawable.y,
        R.drawable.z
    )

    fun getAlphabetData(): ArrayList<Alphabet> {
        val alphabetList = ArrayList<Alphabet>()

        for (i in alphabetData.indices) {
            val alphabet = Alphabet().apply {
                this.alphabet = alphabetData[i]
                this.sign = signData[i]
            }
            alphabetList.add(alphabet)
        }

        return alphabetList
    }

    private val wordData = arrayOf("Halo", "Terima kasih", "Sama-sama")
    private val gestureData = intArrayOf(
        R.drawable.halo,
        R.drawable.terimakasih,
        R.drawable.samasama
    )

    fun getWord(): ArrayList<Word> {
        val wordList = ArrayList<Word>()

        for (i in wordData.indices) {
            val word = Word().apply {
                this.word = wordData[i]
                this.gesture = gestureData[i]
            }
            wordList.add(word)
        }

        return wordList
    }
}
