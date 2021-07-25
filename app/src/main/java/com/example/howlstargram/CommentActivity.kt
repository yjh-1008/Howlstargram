package com.example.howlstargram

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlstargram.databinding.ActivityCommentBinding
import com.example.howlstargram.navigation.model.AlarmDTO
import com.example.howlstargram.navigation.model.ContentDTO
import com.example.howlstargram.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid:String?=null
    var destinationUid:String?=null
    private lateinit var binding:ActivityCommentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding= ActivityCommentBinding.inflate(layoutInflater)
        contentUid=intent.getStringExtra("contentUid")
        destinationUid=intent.getStringExtra("destinationUid")
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.commentRecyclerview.adapter=CommentRecyclerViewAdapter()
        binding.commentRecyclerview.layoutManager=LinearLayoutManager(this)
        binding.commentBtnSend.setOnClickListener {
            var comment= ContentDTO.Comment()
            comment.userId= FirebaseAuth.getInstance().currentUser?.email
            comment.uid=FirebaseAuth.getInstance().currentUser?.uid
            comment.comment=binding.commnetEditMessage.text.toString()
            comment.timestamp=System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!,binding.commnetEditMessage.text.toString())
            binding.commnetEditMessage.setText("")
        }

    }

    fun commentAlarm(destinationUid:String, message:String){
        var alarmDTO= AlarmDTO()
        alarmDTO.destinationUid=destinationUid
        alarmDTO.userId=FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind=1
        alarmDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp=System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        var message=FirebaseAuth.getInstance()?.currentUser?.email+" "+getString(R.string.alarm_comment)+"of"+message
        FcmPush.instance.sendMessage(destinationUid,"HowlStargram",message)
    }

    inner class CommentRecyclerViewAdapter:RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var comments:ArrayList<ContentDTO.Comment> = arrayListOf()
        init{
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot==null) return@addSnapshotListener
                    for(snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view=LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
            return CustomViewHolder(view)
        }
        private inner class CustomViewHolder(view: View):RecyclerView.ViewHolder(view)


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view=holder.itemView
            view.commentviewitem_textview_profile.text=comments[position].comment
            view.commentviewitem_textview_comment.text=comments[position].userId
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener{
                    if(it.isSuccessful){
                        var url=it.result!!["image"]
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                    }
                }
        }

        override fun getItemCount(): Int {
            return comments.size
        }

    }
}