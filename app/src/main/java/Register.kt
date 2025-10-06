package com.example.eventplanner

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.eventplanner.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class RegisterActivity : AppCompatActivity() {

    // UI references
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginText: TextView
    private lateinit var ageEditText: TextInputEditText
    private lateinit var googleSignIn: Button
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText

    // Firebase
    private lateinit var firebaseAuth: FirebaseAuth

    // Google SSO
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.eventplanner.utils.SettingsManager.applySavedTheme(this)
        applySavedLocale()
        setContentView(R.layout.fragment_register)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()

        // Bind UI elements
        usernameEditText = findViewById(R.id.etUsername)
        firstNameEditText = findViewById(R.id.et_first_name)
        lastNameEditText = findViewById(R.id.et_last_name)
        emailEditText = findViewById(R.id.etEmail)
        passwordEditText = findViewById(R.id.etPassword)
        registerButton = findViewById(R.id.btnRegister)
        backToLoginText = findViewById(R.id.tvAlreadyAccount)
        googleSignIn = findViewById(R.id.btnGoogleLogin)
        ageEditText = findViewById(R.id.etAge)

        // ------------------ Google Sign-In setup ------------------
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ------------------ Email/Password registration ------------------
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val age = ageEditText.text.toString().trim().toIntOrNull() ?: 0

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                passwordEditText.error = "Password must be at least 6 characters, include an uppercase, lowercase, special character and a number"
                return@setOnClickListener
            }

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val currentUser = firebaseAuth.currentUser
                        if (currentUser != null) {
                            val user = User(
                                username = username,
                                name = currentUser.displayName?.split(" ")?.firstOrNull() ?: "",
                                surname = currentUser.displayName?.split(" ")?.lastOrNull() ?: "",
                                email = currentUser.email ?: "",
                                uid = currentUser.uid,
                                age = age
                            )
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(currentUser.uid)
                                .setValue(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, Login::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // ------------------ Back to login ------------------
        backToLoginText.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        // ------------------ Google Sign-In ------------------
        googleSignIn.setOnClickListener { signInWithGoogle() }
    }

    private fun applySavedLocale() {
        val code = com.example.eventplanner.utils.SettingsManager.getSavedLocale(this) ?: return
        val locale = java.util.Locale(code)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Google SSO logic
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Use registerForActivityResult in new code")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser!!
                    val displayName = user.displayName ?: "User"
                    val parts = displayName.split(" ")
                    val name = parts.getOrNull(0) ?: ""
                    val surname = parts.getOrNull(1) ?: ""
                    val email = user.email ?: ""
                    val uid = user.uid

                    val newUser = User(
                        username = displayName,
                        name = name,
                        surname = surname,
                        email = email,
                        uid = uid,
                        age = 0
                    )

                    FirebaseDatabase.getInstance().getReference("users")
                        .child(uid)
                        .setValue(newUser)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Welcome $displayName", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to save Google user", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

// Password validation
private fun isValidPassword(password: String): Boolean {
    val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$")
    return passwordRegex.matches(password)
}