package com.example.app_aeroclima

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.app_aeroclima.db.MySqlManager
import com.example.app_aeroclima.db.UserSession
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private val mysqlManager = MySqlManager()

    // Instancia de Firebase Auth
    private lateinit var auth: FirebaseAuth

    // Launcher para el resultado de Google
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        } else {
            Toast.makeText(this, "Cancelado o falló Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Verificar sesión guardada (Preferencia local)
        val prefs = getSharedPreferences("AeroClimaPrefs", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("USER_EMAIL", null)

        // Verificar si Firebase ya tiene sesión activa
        val currentUser = auth.currentUser

        if (savedEmail != null) {
            UserSession.currentUserEmail = savedEmail
            goToMain()
        } else if (currentUser != null) {
            // Si Firebase recuerda al usuario pero las SharedPreferences no
            UserSession.currentUserEmail = currentUser.email
            goToMain()
        }

        val etEmail = findViewById<EditText>(R.id.etLoginUsername)
        val etPass = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnGoToRegister)

        // Si en tu XML es un SignInButton de Google:
        val btnGoogle = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        // Si en tu XML es un Button normal, cambia el tipo arriba a Button

        // LOGIN NORMAL (MySQL directo)
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                btnLogin.isEnabled = false
                mysqlManager.loginUser(email, pass,
                    onSuccess = {
                        saveSessionAndEnter(email)
                    },
                    onFailure = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        btnLogin.isEnabled = true
                    }
                )
            }
        }

        // LOGIN GOOGLE (Firebase -> MySQL)
        btnGoogle.setOnClickListener {
            iniciarGoogleSignIn()
        }

        // IR A REGISTRO
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun iniciarGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut() // Cierra sesión previa para permitir elegir cuenta

        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Autenticar con Firebase usando el token de Google
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("GoogleLogin", "Google sign in failed", e)
            Toast.makeText(this, "Fallo conexión Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        Toast.makeText(this, "Autenticando en Firebase...", Toast.LENGTH_SHORT).show()

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Éxito en Firebase
                    val user = auth.currentUser
                    val email = user?.email

                    if (email != null) {
                        // AHORA sincronizamos con MySQL para los favoritos
                        syncUserWithMySql(email)
                    } else {
                        Toast.makeText(this, "Error: No se pudo obtener el email", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Fallo autenticación Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun syncUserWithMySql(email: String) {
        Toast.makeText(this, "Sincronizando datos...", Toast.LENGTH_SHORT).show()

        // script google_login.php
        mysqlManager.loginWithGoogle(email,
            onSuccess = {
                // Todo perfecto: Login Google OK + Base de datos OK
                saveSessionAndEnter(email)
            },
            onFailure = { msg ->
                Toast.makeText(this, "Error guardando usuario en BD: $msg", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun saveSessionAndEnter(email: String) {
        val prefs = getSharedPreferences("AeroClimaPrefs", Context.MODE_PRIVATE)
        UserSession.currentUserEmail = email
        prefs.edit().putString("USER_EMAIL", email).apply()

        Toast.makeText(this, "Bienvenido $email", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}