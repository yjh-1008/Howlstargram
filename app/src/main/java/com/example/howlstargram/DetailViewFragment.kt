package com.example.howlstargram

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.howlstargram.navigation.model.AlarmDTO
import com.example.howlstargram.navigation.model.ContentDTO
import com.example.howlstargram.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){
    var firestore:FirebaseFirestore?=null
    var uid:String?=null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view= LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        uid=FirebaseAuth.getInstance().currentUser?.uid
        firestore=FirebaseFirestore.getInstance()
        view.detailviewfragment_recyclerview.adapter=DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager=LinearLayoutManager(activity)
        return view
    }
    inner class DetailViewRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs:ArrayList<ContentDTO> = arrayListOf()
        var contentUidList:ArrayList<String> = arrayListOf()

        init{
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{querySnapshot,firebaseFirestoreException->
                contentDTOs.clear()
                contentUidList.clear()
                if(querySnapshot==null) return@addSnapshotListener
                for(snapshot in querySnapshot!!.documents){
                    var item=snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view=LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)

            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView
            viewholder.detailviewitem_profile_text.text=contentDTOs!![position].userId

            //image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            //explain of content
            viewholder.detailviewitem_explain_textview.text=contentDTOs!![position].explain

            //likes
            viewholder.detailviewitem_favoritecounter_textview.text="Likes "+ contentDTOs!![position].favoriteCount

            //This code is when the button is clicked
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            //this code is when the page is loaded
            if(contentDTOs!![position].favorites.containsKey(uid)){
                //This is like status
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                //This is unlike status
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_profile_image)

            //This code is when the profile image is clicked
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment= UserFragment()
                var bundle=Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                fragment.arguments=bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            viewholder.detailviewitem_comment_imageview.setOnClickListener {
                var intent= Intent(it.context, CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        fun favoriteEvent(potition:Int) {
            var tsDoc = firestore?.collection("images")?.document(contentUidList[potition])
            firestore?.runTransaction {
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO = it.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    //when the button is clicked
                    contentDTO.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO?.favorites.remove(uid)

                } else {
                    //when the button is not clicked
                    contentDTO.favoriteCount = contentDTO?.favoriteCount + 1
                    contentDTO?.favorites[uid!!]=true
                    favoriteAlarm(contentDTOs[potition].uid!!)
                }
                it.set(tsDoc, contentDTO)

            }
        }
        fun favoriteAlarm(destinationUid:String){
            var alarmDTO= AlarmDTO()
            alarmDTO.destinationUid=destinationUid
            alarmDTO.userId=FirebaseAuth.getInstance()?.currentUser?.email
            alarmDTO.uid=FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind=0
            alarmDTO.timestamp=System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            var message=FirebaseAuth.getInstance()?.currentUser?.email+getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid,"HowlStargram",message)
        }

    }
}