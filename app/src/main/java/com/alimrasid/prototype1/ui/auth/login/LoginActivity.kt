package com.alimrasid.prototype1.ui.auth.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.ui.MainActivity
import com.alimrasid.prototype1.ui.auth.register.RegisterActivity
import com.alimrasid.prototype1.utils.ResponseState
import com.alimrasid.prototype1.utils.Status
import com.alimrasid.prototype1.utils.Status.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var btnToRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var btnLoginWithGoogle: Button
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: LoginViewModel

    private val SIGN_IN_RESULT_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)

        btnToRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)
        btnLoginWithGoogle = findViewById(R.id.btnLoginWithGoogle)

        progressBar = findViewById(R.id.progressBar)

        isLoggedIn()
        observeIsLoading()

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (email.isEmpty()) {
                edtEmail.error = "Tidak boleh kosong"
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                edtPassword.error = "Tidak boleh kosong"
                edtPassword.requestFocus()
                return@setOnClickListener
            }

            viewModel.loginWithEmailFlow(email, password)
        }

        btnLoginWithGoogle.setOnClickListener { loginWithGoogleFlow() }

        observeLoginWithEmailStatus()

        btnToRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
    }

    private fun isLoggedIn() {
        viewModel.isLoggedIn().observe(this, { isLoggedIn ->
            if (isLoggedIn) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        })
    }

    private fun observeLoginWithEmailStatus() {
        viewModel.loginStatus().observe(this) { stringResponseState ->
            when (stringResponseState.status) {
                SUCCESS -> {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }

                FAILURE -> Toast.makeText(
                    this@LoginActivity,
                    stringResponseState.message,
                    Toast.LENGTH_SHORT
                ).show()

                ResponseState.Status.SUCCESS -> TODO()
                ResponseState.Status.FAILURE -> TODO()
                null -> TODO()
            }
        }
    }

    private fun observeIsLoading() {
        viewModel.isLoading().observe(this, { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
    }

    private fun loginWithGoogleFlow() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val client: GoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInIntent = client.signInIntent
        startActivityForResult(signInIntent, SIGN_IN_RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                account?.let {
                    val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
                    viewModel.loginWithGoogleFlow(credential)
                }
            } catch (e: ApiException) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

