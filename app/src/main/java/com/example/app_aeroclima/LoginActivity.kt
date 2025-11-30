package com.example.app_aeroclima

import DatabaseHelper
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth // IMPORTANTE
import com.google.firebase.auth.GoogleAuthProvider // IMPORTANTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: SignInButton
    private lateinit var googleSignInClient: GoogleSignInClient

    // Agregamos Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        dbHelper = DatabaseHelper(this)

        setupViews()
        setupGoogleSignIn()
    }

    private fun setupViews() {
        etUsername = findViewById(R.id.etLoginUsername)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
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
            performLocalLogin(user, pass)
        }

        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // AHORA SÍ: Usamos el token de Google para entrar a Firebase
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Fallo Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- EL PUENTE MÁGICO ---
    private fun firebaseAuthWithGoogle(idToken: String) {
        Toast.makeText(this, "Conectando con Firebase...", Toast.LENGTH_SHORT).show()
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // ¡ÉXITO! Firebase ya sabe quién eres
                    val user = auth.currentUser
                    handleLoginSuccess(user?.email ?: "")
                } else {
                    Toast.makeText(this, "Error Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleLoginSuccess(email: String) {
        if (email.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            // Guardamos en local solo por si acaso, pero lo importante es Firebase
            val fakePassword = "google_auth_token"
            if (!dbHelper.checkUser(email, fakePassword)) {
                dbHelper.addUser(email, fakePassword)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "Bienvenido $email", Toast.LENGTH_SHORT).show()
                goToMain()
            }
        }
    }

    private fun performLocalLogin(user: String, pass: String) {
        // El login local no conecta con Firebase en este ejemplo
        btnLogin.isEnabled = false
        Toast.makeText(this, "Verificando...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val userExists = dbHelper.checkUser(user, pass)
            withContext(Dispatchers.Main) {
                if (userExists) {
                    goToMain()
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}