package com.alimrasid.prototype1.ui.auth.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.ui.auth.login.LoginActivity
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var edtName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword1: TextInputEditText
    private lateinit var edtPassword2: TextInputEditText
    private lateinit var viewModel: RegisterViewModel
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)

        progressBar = findViewById(R.id.progressBar)
        edtName = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword1 = findViewById(R.id.edtPassword)
        edtPassword2 = findViewById(R.id.edtPassword2)
        val btnToLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            registerFlow()
        }

        observeRegisterStatus()
        observeIsLoading()
    }

    private fun registerFlow() {
        val name = edtName.text.toString()
        val email = edtEmail.text.toString()
        val password = edtPassword1.text.toString()
        val password2 = edtPassword2.text.toString()

        if (name.isEmpty()) {
            edtName.error = "Tidak boleh kosong"
            edtName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            edtEmail.error = "Tidak boleh kosong"
            edtEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            edtPassword1.error = "Tidak boleh kosong"
            edtPassword1.requestFocus()
            return
        }

        if (password2 != password) {
            edtPassword2.error = "Password tidak sama"
            edtPassword2.requestFocus()
            return
        }

        viewModel.registerFlow(name, email, password)
    }

    private fun observeRegisterStatus() {
        viewModel.registerStatus().observe(this) { stringResponseState ->
            when (stringResponseState.getStatus()) {
                ResponseStatus.SUCCESS -> {
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }

                ResponseStatus.FAILURE -> {
                    Toast.makeText(
                        this@RegisterActivity, stringResponseState.getMessage(), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeIsLoading() {
        viewModel.isLoading().observe(this, { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        })
    }
}
