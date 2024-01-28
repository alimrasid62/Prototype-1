package com.alimrasid.prototype1.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alimrasid.prototype1.utils.EspressoIdlingResource
import com.alimrasid.prototype1.utils.ResponseState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterViewModel : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private var registerStatus: MutableLiveData<ResponseState<String>>? = null

    fun registerStatus(): LiveData<ResponseState<String>> {
        if (registerStatus == null) {
            registerStatus = MutableLiveData()
        }
        return registerStatus!!
    }

    private val isLoading = MutableLiveData<Boolean>()

    fun isLoading(): LiveData<Boolean> {
        return isLoading
    }

    fun registerFlow(name: String, email: String, password: String) {
        isLoading.postValue(true)
        EspressoIdlingResource.increment()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val request = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    val user: FirebaseUser? = firebaseAuth.currentUser
                    user?.updateProfile(request)

                    firebaseAuth.signOut()

                    registerStatus?.postValue(ResponseState<String>().success("Register berhasil, silahkan Login dengan email!"))
                } else {
                    registerStatus?.postValue(task.exception?.message?.let {
                        ResponseState<String>().failure(
                            it
                        )
                    })
                }
                EspressoIdlingResource.decrement()
                isLoading.postValue(false)
            }
    }
}
