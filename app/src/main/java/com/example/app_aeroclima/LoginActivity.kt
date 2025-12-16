package com.example.app_aeroclima

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.app_aeroclima.db.MySqlManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {

    private val mysqlManager = MySqlManager()
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: SignInButton
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
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

        // Lógica de inicio de sesión Local
        btnLogin.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            performLocalLogin(user, pass)
        }

        btnGoogleSignIn.setOnClickListener { signInWithGoogle() }
        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId).requestEmail().build()
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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Fallo Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                handleGoogleLoginSuccess(user?.email ?: "")
            } else {
                Toast.makeText(this, "Error Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleGoogleLoginSuccess(email: String) {
        if (email.isEmpty()) return
        Toast.makeText(this, "Bienvenido $email", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun performLocalLogin(user: String, pass: String) {
        btnLogin.isEnabled = false
        Toast.makeText(this, "Verificando...", Toast.LENGTH_SHORT).show()

        mysqlManager.loginUser(
            user,
            pass,
            onSuccess = { email ->
                goToMain()
            },
            onFailure = {
                Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = true
            }
        )
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}