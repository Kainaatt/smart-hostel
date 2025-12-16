package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etStudentId: TextInputEditText
    private lateinit var etRoom: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_signup)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etStudentId = findViewById(R.id.etStudentId)
        etRoom = findViewById(R.id.etRoom)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Sign up button
        findViewById<MaterialButton>(R.id.btnSignUp).setOnClickListener {
            performSignUp()
        }

        // Login link
        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            finish() // Go back to login
        }
    }

    private fun performSignUp() {
        val name = etName.text?.toString()?.trim() ?: ""
        val studentId = etStudentId.text?.toString()?.trim() ?: ""
        val room = etRoom.text?.toString()?.trim() ?: ""
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString() ?: ""
        val confirmPassword = etConfirmPassword.text?.toString() ?: ""

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_name), Toast.LENGTH_SHORT).show()
            etName.requestFocus()
            return
        }

        if (studentId.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_student_id), Toast.LENGTH_SHORT).show()
            etStudentId.requestFocus()
            return
        }

        if (room.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_room), Toast.LENGTH_SHORT).show()
            etRoom.requestFocus()
            return
        }

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

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_confirm_password), Toast.LENGTH_SHORT).show()
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.passwords_dont_match), Toast.LENGTH_SHORT).show()
            etConfirmPassword.requestFocus()
            return
        }

        // Dummy sign up - just go to main activity
        Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

