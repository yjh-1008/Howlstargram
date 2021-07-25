package com.example.howlstargram

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.howlstargram.databinding.ActivityMainBinding
import com.example.howlstargram.navigation.*
import com.example.howlstargram.navigation.model.*
import com.example.howlstargram.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        binding=ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            setToolbarDefault()
            when(it.itemId){
                R.id.action_home->{
                    val detailViewFragment= DetailViewFragment()
                    supportFragmentManager.beginTransaction().replace(R.id.main_content,detailViewFragment).commit()
                    true
                }
                R.id.action_search->{
                    val gridFragment= GridFragment()
                    supportFragmentManager.beginTransaction().replace(R.id.main_content,gridFragment).commit()
                    true
                }
                R.id.action_add_phoeo->{
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                        startActivity(Intent(this, AddPhotoActivity::class.java))
                    }

                    true
                }
                R.id.action_favorite_alarm->{
                    val alarmFragment= AlarmFragment()
                    supportFragmentManager.beginTransaction().replace(R.id.main_content,alarmFragment).commit()
                    true
                }
                R.id.action_account->{
                    val userFragment= UserFragment()
                    var bundle=Bundle()
                    var uid= FirebaseAuth.getInstance().currentUser?.uid
                    bundle.putString("destinationUid",uid)
                    userFragment.arguments=bundle
                    supportFragmentManager.beginTransaction().replace(R.id.main_content,userFragment).commit()
                    true
                }
                else->false

            }

        }
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        //Set default screen
        bottom_navigation.selectedItemId=R.id.action_home
        registerPushToken()
    }

    override fun onStop() {
        super.onStop()
        FcmPush.instance.sendMessage("7lXCrfH97IWyNw6Zsvp2NAVlJ6J3","hi","bye")
    }
    fun registerPushToken(){
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            val token=it.result?.token
            val uid=FirebaseAuth.getInstance().currentUser?.uid
            val map=mutableMapOf<String,Any>()
            map["pushToken"]=token!!
            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
        }
    }

    fun setToolbarDefault(){
        toolbar_username.visibility=View.GONE
        toolbar_btn_back.visibility= View.GONE
        toolbar_title_image.visibility=View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode== Activity.RESULT_OK){
            var imageUri= data?.data
            var uid=FirebaseAuth.getInstance().currentUser?.uid
            var storageRef=FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageRef.putFile(imageUri!!).continueWithTask {
                return@continueWithTask storageRef.downloadUrl

                }.addOnSuccessListener {
                    var map=HashMap<String,Any>()
                    map["image"]=it.toString()
                    FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
                }
            }

        }
    }
