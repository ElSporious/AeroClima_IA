package com.example.app_aeroclima

import DatabaseHelper
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import android.text.Editable
import android.text.TextWatcher


class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        etUsername = findViewById<EditText>(R.id.etLoginUsername)
        etPassword = findViewById<EditText>(R.id.etLoginPassword)
        btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoToRegister = findViewById<Button>(R.id.btnGoToRegister)

        btnLogin.isEnabled = false

        val loginTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val user = etUsername.text.toString().trim()
                val pass = etPassword.text.toString().trim()

                btnLogin.isEnabled = user.isNotEmpty() && pass.isNotEmpty()
            }
        }

        etUsername.addTextChangedListener(loginTextWatcher)
        etPassword.addTextChangedListener(loginTextWatcher)

        btnLogin.setOnClickListener {

            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            val userExists = dbHelper.checkUser(user, pass)

            if (userExists) {
                Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }


        btnGoToRegister.setOnClickListener {

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}