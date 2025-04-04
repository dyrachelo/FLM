package com.example.mfl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
            } else {
                signIn(auth, email, pass)
            }
        }

        buttonGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }


    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Если пользователь уже авторизован, перенаправляем его на главный экран
            val intent = Intent(this, Panel::class.java)
            startActivity(intent)
            finish() // Закрываем экран авторизации
        }
    }

    private fun signIn(auth: FirebaseAuth, email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MyLog", "Авторизация успешна")
                    val intent = Intent(this, Panel::class.java)
                    intent.putExtra("userEmail", email)
                    startActivity(intent)
                } else {
                    Log.d("MyLog", "Ошибка авторизации: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithGoogle() {
        // Создаем запрос для Google Sign-In
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id)) // Укажите ваш Web Client ID
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // Используем корутины для вызова suspend-функции
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Вызываем getCredential в корутине
                val credentialResponse = withContext(Dispatchers.IO) {
                    credentialManager.getCredential(
                        context = this@AuthActivity,
                        request = request
                    )
                }

                // Обрабатываем результат
                val credential = credentialResponse.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Log.e("MyLog", "Unexpected credential type")
                }
            } catch (e: Exception) {
                Log.e("MyLog", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("MyLog", "signInWithCredential:success")
                    val intent = Intent(this, Panel::class.java)

                    startActivity(intent)
                } else {
                    Log.w("MyLog", "signInWithCredential:failure", task.exception)
                }
            }
    }
}