package com.example.app_aeroclima

import DatabaseHelper
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import android.text.Editable
import android.text.TextWatcher

class RegisterActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        etUsername = findViewById<EditText>(R.id.etRegisterUsername)
        etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.isEnabled = false

        val registerTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val user = etUsername.text.toString().trim()
                val pass = etPassword.text.toString().trim()

                btnRegister.isEnabled = user.isNotEmpty() && pass.isNotEmpty()
            }
        }

        etUsername.addTextChangedListener(registerTextWatcher)
        etPassword.addTextChangedListener(registerTextWatcher)

        btnRegister.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            val success = dbHelper.addUser(user, pass)

            if (success) {
                Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al registrar. ¿Usuario ya existe?", Toast.LENGTH_SHORT).show()
            }
        }
    }
}