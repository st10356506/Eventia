package com.example.eventplanner

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.eventplanner.viewmodel.UserViewModel
import com.example.eventplanner.network.RetrofitClient
import com.example.eventplanner.network.EventiaApi
import com.example.eventplanner.models.UserRequest
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {

    // UI Elements
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var backToRegisterText: TextView
    private lateinit var googleLoginButton: Button

    // Firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // ViewModel
    private val viewModel: UserViewModel by viewModels()

    // Retrofit API
    private val eventiaApi: EventiaApi = RetrofitClient.eventiaApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_login)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()

        // ---------------- Google SSO Setup ----------------
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ---------------- Bind UI ----------------
        emailEditText = findViewById(R.id.etUsername)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        backToRegisterText = findViewById(R.id.tvbackToReg)
        googleLoginButton = findViewById(R.id.btnGoogleLogin)

        // ---------------- Email/Password Login ----------------
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            user?.let {
                                sendUserToApi(
                                    UserRequest(
                                        firstName = it.displayName?.split(" ")?.firstOrNull() ?: "Unknown",
                                        lastName = it.displayName?.split(" ")?.lastOrNull() ?: "",
                                        email = it.email ?: "",
                                        age = 0
                                    )
                                )
                            }
                            navigateToMain(user?.email ?: "User")
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // ---------------- Google SSO Login ----------------
        googleLoginButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // ---------------- Back to register ----------------
        backToRegisterText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    // ---------------- Handle Google Sign-In ----------------
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                if (authResult.isSuccessful) {
                    val firebaseUser = firebaseAuth.currentUser
                    firebaseUser?.let {
                        // Save user data to API
                        sendUserToApi(
                            UserRequest(
                                firstName = toString(),
                                lastName = toString(),
                                email = it.email ?: "",
                                age = 0
                            )
                        )
                        navigateToMain(it.email ?: "Google User")
                    }
                } else {
                    Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Send user to REST API ----------------
    private fun sendUserToApi(user: UserRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = eventiaApi.createUser(user)
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@Login, "User saved to API!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@Login, "API error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---------------- Navigate to MainActivity ----------------
    private fun navigateToMain(username: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("username", username)
        }
        startActivity(intent)
        finish()
    }
}
