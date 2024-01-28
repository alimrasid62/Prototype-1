package com.alimrasid.prototype1.utils

import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.IdlingResource

object EspressoIdlingResource {
    private const val RESOURCE = "GLOBAL"
    private val idlingResource = CountingIdlingResource(RESOURCE)

    fun increment() {
        idlingResource.increment()
    }

    fun decrement() {
        idlingResource.decrement()
    }

    fun getEspressoIdlingResource(): IdlingResource {
        return idlingResource
    }
}
