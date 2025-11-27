package com.example.app_aeroclima

import DatabaseHelper
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        etUsername = findViewById(R.id.etRegisterUsername)
        etPassword = findViewById(R.id.etRegisterPassword)
        btnRegister = findViewById(R.id.btnRegister)

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

            btnRegister.isEnabled = false
            Toast.makeText(this, "Creando usuario...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch(Dispatchers.IO) {
                val success = dbHelper.addUser(user, pass)

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@RegisterActivity, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                        finish() // Vuelve a la pantalla anterior (Login)
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error al registrar. ¿Usuario ya existe?", Toast.LENGTH_SHORT).show()
                        // Reactivamos el botón si falla
                        btnRegister.isEnabled = true
                    }
                }
            }
        }
    }
}