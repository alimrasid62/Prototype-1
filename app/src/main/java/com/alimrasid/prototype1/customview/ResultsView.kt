package com.alimrasid.prototype1.customview

import com.alimrasid.prototype1.tflite.Classifier

interface ResultsView {
    fun setResults(results: List<Classifier.Recognition>)
}

