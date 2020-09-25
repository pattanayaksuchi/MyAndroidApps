package com.example.projectmanager.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.firebase.FireStoreClass
import com.example.projectmanager.models.User
import com.example.projectmanager.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_my_profile.*
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var mUserDetails : User
    private var mDownloadableImageUri : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setActionBar()

        FireStoreClass().loadUserData(this)
        iv_user_image.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), Constants.READ_STORAGE_PERMISSION_CODE)
            }
        }

        btn_update.setOnClickListener {
            if (mSelectedImageFileUri != null) uploadUserImage()
            else {
                showProgressDialog(resources.getString(R.string.pleaseWait))
                updateProfileUserData()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this)
            }
        } else {
            Toast.makeText(this, "Oops, you just denied the permission for storage. You can enable it in settings", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data != null) {
            mSelectedImageFileUri = data.data

            try {
                Glide.with(this).load(mSelectedImageFileUri).centerCrop().placeholder(R.drawable.ic_user_placeholder).into(iv_user_image)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun setActionBar() {
        setSupportActionBar(tb_my_profile_activity)
        val actionBar = supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_24dp)
            actionBar.title = resources.getString(R.string.myProfile)
        }

        tb_my_profile_activity.setNavigationOnClickListener { onBackPressed() }
    }

    fun setuserDataInUI(user: User) {
        mUserDetails = user
        Glide.with(this).load(user.image).centerCrop().placeholder(R.drawable.ic_user_placeholder).into(iv_user_image)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if (user.mobile != 0L) et_mobile.setText(user.mobile.toString())

    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.pleaseWait))

        if (mSelectedImageFileUri != null) {
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child("USER_IMAGE ${System.currentTimeMillis()}.${Constants.getFileExtension(this, mSelectedImageFileUri)}")

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot ->
                Log.i("Firebase Image URL", taskSnapshot.metadata!!.reference!!.downloadUrl.toString())
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                    Log.i("Downloadable Image URI", uri.toString())
                    mDownloadableImageUri = uri.toString()

                    updateProfileUserData()
                }
            }.addOnFailureListener {
                exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                hideProgressDialog()
            }
        }
    }

    private fun updateProfileUserData() {
        val userHashMap : HashMap<String, Any> = HashMap<String, Any>()
        var anyChangesMade = false

        if (mDownloadableImageUri.isNotEmpty()) {
            userHashMap[Constants.IMAGE] = mDownloadableImageUri
            anyChangesMade = true
        }

        if (et_name.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = et_name.text.toString()
            anyChangesMade = true
        }

        if (et_mobile.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = et_mobile.text.toString().toLong()
            anyChangesMade = true
        }

        if (anyChangesMade)
            FireStoreClass().updateUserData(this, userHashMap)
    }

    fun profileUpdateSuccess() {
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }
}