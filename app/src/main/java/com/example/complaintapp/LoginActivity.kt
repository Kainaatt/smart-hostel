package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.admin.AdminLoginActivity
import com.example.complaintapp.repository.AuthRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        // Login button
        btnLogin.setOnClickListener {
            performLogin()
        }

        // Sign up link
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Forgot password
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            Toast.makeText(this, getString(R.string.forgot_password_message), Toast.LENGTH_SHORT).show()
        }

        // Admin access
        findViewById<TextView>(R.id.tvAdminAccess).setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""

        // Validation
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_email), Toast.LENGTH_SHORT).show()
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.please_enter_valid_email), Toast.LENGTH_SHORT).show()
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_password), Toast.LENGTH_SHORT).show()
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            etPassword.requestFocus()
            return
        }

        // Show loading
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // Firebase login
        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            
            btnLogin.isEnabled = true
            progressBar.visibility = View.GONE

            result.onSuccess {
                Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                val errorMessage = when {
                    exception.message?.contains("password") == true -> 
                        "Invalid password. Please try again."
                    exception.message?.contains("user") == true -> 
                        "No account found with this email."
                    else -> 
                        "Login failed: ${exception.message}"
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

