package com.alimrasid.prototype1.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alimrasid.prototype1.data.News
import com.alimrasid.prototype1.utils.EspressoIdlingResource
import com.google.firebase.database.*
import java.util.*

class HomeViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance("https://bisindo-11e09-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private var newsList: MutableLiveData<List<News>>? = null

    fun getNews(): LiveData<List<News>> {
        if (newsList == null) {
            newsList = MutableLiveData()
            loadNewsFromFirebaseDB()
        }
        return newsList!!
    }

    val isLoading = MutableLiveData<Boolean>()
    fun isLoading(): LiveData<Boolean> = isLoading

    val isConnected = MutableLiveData<Boolean>()
    fun isConnected(): LiveData<Boolean> {
        connectionState()
        return isConnected
    }

    private fun loadNewsFromFirebaseDB() {
        val newsList = ArrayList<News>()

        isLoading.postValue(true)
        EspressoIdlingResource.increment()
        database.getReference()
            .child("news")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    newsList.clear()
                    for (keyNode in snapshot.children) {
                        val news = keyNode.getValue(News::class.java)
                        news?.let { newsList.add(it) }
                    }
                    this@HomeViewModel.newsList?.postValue(newsList)
                    isLoading.postValue(false)
                    EspressoIdlingResource.decrement()
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading.postValue(false)
                    EspressoIdlingResource.decrement()
                }
            })
    }

    private fun connectionState() {
        isConnected.postValue(false)
        val reference = database.getReference(".info/connected")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java)
                if (connected == true) {
                    isConnected.postValue(true)
                    Log.d("CONNECTED", "connected")
                } else {
                    isConnected.postValue(false)
                    Log.d("CONNECTED", "not connected")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isConnected.postValue(false)
                Log.w("CONNECTED", "Listener was canceled")
            }
        })
    }
}

