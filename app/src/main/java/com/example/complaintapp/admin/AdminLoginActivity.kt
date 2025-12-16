package com.example.complaintapp.admin

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.complaintapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var etAdminId: TextInputEditText
    private lateinit var etPassword: TextInputEditText

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
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
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

        // Dummy admin login - check for "admin" / "admin123"
        if (adminId == "admin" && password == "admin123") {
            Toast.makeText(this, getString(R.string.admin_login_success), Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, getString(R.string.invalid_admin_credentials), Toast.LENGTH_SHORT).show()
        }
    }
}

