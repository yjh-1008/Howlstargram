package com.example.howlstargram

import android.app.Activity
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import bolts.Task
import com.example.howlstargram.R
import com.example.howlstargram.databinding.ActivityAddPhotoBinding
import com.example.howlstargram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.lang.Integer.toString
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    private lateinit var binding:ActivityAddPhotoBinding
    var PICK_IMAGE_FROM_ALBUM=0
    var storage: FirebaseStorage?=null
    var photoUri: Uri?=null

    var auth: FirebaseAuth?=null
    var firestore:FirebaseFirestore?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivityAddPhotoBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //Initiate storage
        storage=FirebaseStorage.getInstance()
        auth=FirebaseAuth.getInstance()
        firestore= FirebaseFirestore.getInstance()


        //Open the album
        var photoPickerIntent= Intent(Intent.ACTION_PICK)
        photoPickerIntent.type="image/*"
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)
        //add image upload event
        binding.addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PICK_IMAGE_FROM_ALBUM){
            if(resultCode== Activity.RESULT_OK){
                //This is path to the selected image
                photoUri=data?.data
                binding.addPhotoImage.setImageURI(photoUri)

            }else{
                //Exit the addPhotoActivity if you leave the album selecting it
            }
        }
    }
    fun contentUpload(){
        //Make filename
        var timestamp=SimpleDateFormat("yyyyMMDD_HHmmss").format(Date())
        var imageFileName="Image_"+timestamp+"_.png"

        var storageRef=storage?.reference?.child("images")?.child(imageFileName)

        //Promise method
        /*storageRef?.putFile(photoUri!!)?.continueWithTask { task: com.google.android.gms.tasks.Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener {uri->
            var contentDTO=ContentDTO()

            //Insert downloadUri of image
            contentDTO.imageUrl = uri.toString()
            //Insert uid of user
            contentDTO.uid= auth?.currentUser?.uid
            //Insert user Id
            contentDTO.userId=auth?.currentUser?.email
            //Insert explain of content
            contentDTO.explain=binding.addphotoEditExplain.text.toString()
            //Insert timestamp
            contentDTO.timestamp=System.currentTimeMillis()
            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)
            finish()
        }*/

        //Callbak method
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl?.addOnSuccessListener { uri->
                var contentDTO=ContentDTO()

                //Insert downloadUri of image
                contentDTO.imageUrl = uri.toString()
                //Insert uid of user
                contentDTO.uid= auth?.currentUser?.uid
                //Insert user Id
                contentDTO.userId=auth?.currentUser?.email
                //Insert explain of content
                contentDTO.explain=binding.addphotoEditExplain.text.toString()
                //Insert timestamp
                contentDTO.timestamp=System.currentTimeMillis()
                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)
                finish()
            }

        }
    }

}