package com.alimrasid.prototype1.ui.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alimrasid.prototype1.utils.EspressoIdlingResource
import com.alimrasid.prototype1.utils.ResponseState
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var isLoggedIn: MutableLiveData<Boolean>? = null

    fun isLoggedIn(): LiveData<Boolean> {
        if (isLoggedIn == null) {
            isLoggedIn = MutableLiveData()
            isLoggedIn?.postValue(firebaseAuth.currentUser != null)
        }
        return isLoggedIn!!
    }

    private val isLoading = MutableLiveData<Boolean>()

    fun isLoading(): LiveData<Boolean> {
        return isLoading
    }

    private var loginStatus: MutableLiveData<ResponseState<String>>? = null

    fun loginStatus(): LiveData<ResponseState<String>> {
        if (loginStatus == null) {
            loginStatus = MutableLiveData()
        }
        return loginStatus!!
    }

    fun loginWithEmailFlow(email: String, password: String) {
        isLoading.postValue(true)
        EspressoIdlingResource.increment()
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loginStatus?.postValue(ResponseState<String>().success("Login success"))
                } else {
                    loginStatus?.postValue(ResponseState<String>().failure("Login Failed: ${task.exception}"))
                }
                EspressoIdlingResource.decrement()
                isLoading.postValue(false)
            }
    }

    fun loginWithGoogleFlow(credential: AuthCredential) {
        isLoading.postValue(true)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loginStatus?.postValue(ResponseState<String>().success("Login with Google success"))
                } else {
                    loginStatus?.postValue(ResponseState<String>().failure("Login with Google failed"))
                }
                isLoading.postValue(false)
            }
    }
}
