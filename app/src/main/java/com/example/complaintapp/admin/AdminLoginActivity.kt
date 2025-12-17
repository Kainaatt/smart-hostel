package com.example.complaintapp.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.R
import com.example.complaintapp.repository.AuthRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var etAdminId: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_admin_login)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etAdminId = findViewById(R.id.etAdminId)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val adminId = etAdminId.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""

        // Validation
        if (adminId.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_admin_id), Toast.LENGTH_SHORT).show()
            etAdminId.requestFocus()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_password), Toast.LENGTH_SHORT).show()
            etPassword.requestFocus()
            return
        }

        // Show loading
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // Firebase login - treat adminId as email
        lifecycleScope.launch {
            val loginResult = authRepository.login(adminId, password)
            
            loginResult.onSuccess {
                // Check if user is admin
                val isAdmin = authRepository.isAdmin()
                
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE

                if (isAdmin) {
                    Toast.makeText(this@AdminLoginActivity, getString(R.string.admin_login_success), Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this@AdminLoginActivity, AdminDashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    authRepository.logout()
                    Toast.makeText(this@AdminLoginActivity, getString(R.string.invalid_admin_credentials), Toast.LENGTH_SHORT).show()
                }
            }.onFailure { exception ->
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE
                
                val errorMessage = when {
                    exception.message?.contains("password") == true -> 
                        "Invalid password."
                    exception.message?.contains("user") == true -> 
                        "No account found."
                    else -> 
                        "Login failed: ${exception.message}"
                }
                Toast.makeText(this@AdminLoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

