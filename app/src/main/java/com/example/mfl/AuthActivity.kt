package com.example.mfl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userEmail: EditText = findViewById(R.id.user_email_auth)
        val userPass: EditText = findViewById(R.id.user_pass_auth)
        val button: Button = findViewById(R.id.button_auth)
        val buttonGoogle: Button = findViewById(R.id.button_auth_google)
        val linkToReg: TextView = findViewById(R.id.register_text)

        linkToReg.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        button.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
            } else {
                signInWithEmail(email, pass)
            }
        }

        buttonGoogle.setOnClickListener {
            startGoogleSignIn()
        }
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let {
            val intent = Intent(this, Panel::class.java).apply {
                putExtra("userEmail", it.email ?: "email_not_available")
            }
            startActivity(intent)
            finish()
        }
    }

    private fun signInWithEmail(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MyLog", "Авторизация успешна")
                    val intent = Intent(this, Panel::class.java)
                    intent.putExtra("userEmail", email)
                    startActivity(intent)
                    finish()
                } else {
                    Log.d("MyLog", "Ошибка авторизации: ${task.exception?.message}")
                    Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                Log.e("MyLog", "Google ID Token is null")
                Toast.makeText(this, "Ошибка Google авторизации", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MyLog", "Ошибка получения учетных данных", e)
        }
    }

    private fun startGoogleSignIn() {
        val signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                googleSignInLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener { e ->
                Log.e("MyLog", "Ошибка начала входа через Google", e)
                Toast.makeText(this, "Ошибка входа через Google", Toast.LENGTH_SHORT).show()
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val email = user?.email ?: "no_email"
                    Log.d("MyLog", "Успешный вход: $email")
                    val intent = Intent(this, Panel::class.java).apply {
                        putExtra("userEmail", email)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Log.w("MyLog", "Ошибка входа через Google", task.exception)
                    Toast.makeText(this, "Ошибка входа через Google", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
