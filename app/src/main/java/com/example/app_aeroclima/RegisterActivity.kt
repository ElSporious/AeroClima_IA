package com.example.app_aeroclima

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_aeroclima.db.MySqlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: Button

    private val mysqlManager = MySqlManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etUsername = findViewById(R.id.etRegisterUsername)
        etPassword = findViewById(R.id.etRegisterPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)

        btnRegister.isEnabled = false

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

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

            if (!isValidEmail(user)) {
                Toast.makeText(this, "Por favor, ingrese un email válido.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            Toast.makeText(this, "Creando usuario...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch(Dispatchers.IO) {
                val hashedPassword = BCrypt.hashpw(pass, BCrypt.gensalt())

                mysqlManager.registerUser(
                    user,
                    hashedPassword,
                    onSuccess = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    onFailure = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity, "Error al registrar. ¿Usuario ya existe?", Toast.LENGTH_SHORT).show()
                            btnRegister.isEnabled = true
                        }
                    }
                )
            }
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return if (target == null) {
            false
        } else {
            android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }
}