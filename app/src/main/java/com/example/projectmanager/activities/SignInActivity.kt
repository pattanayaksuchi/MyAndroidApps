package com.example.projectmanager.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.projectmanager.R
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.models.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_sign_in.*
import kotlinx.android.synthetic.main.activity_sign_in.btn_signIn


class SignInActivity : BaseActivity() {

    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setActionBar()

        auth = FirebaseAuth.getInstance()

        btn_signIn.setOnClickListener {
            signInUser()
        }

    }

    private fun setActionBar() {
        setSupportActionBar(tb_signIn)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_24dp)
        }

        tb_signIn.setNavigationOnClickListener { onBackPressed() }
    }

    private fun signInUser() {
        val email: String = et_email_signIn.text.toString()
        val password: String = et_password_signIn.text.toString()

        if (validateForm(email, password)) {
            showProgressDialog(resources.getString(R.string.pleaseWait))
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                run {
                    if (task.isSuccessful) {
                        FireStoreClass().loadUserData(this)
                    }
                    else {
                        hideProgressDialog()
                        Log.i("Sign In: ", "signInWithEmail:failure with ${task.exception}")
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateForm(email: String, password: String) : Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please enter an email")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please enter a password")
                false
            }
            else -> true
        }
    }

    fun signInSuccess(user: User) {
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }


}