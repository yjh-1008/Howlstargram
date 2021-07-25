package com.example.howlstargram

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlstargram.navigation.model.AlarmDTO
import com.example.howlstargram.navigation.model.ContentDTO
import com.example.howlstargram.navigation.model.FollowDTO
import com.example.howlstargram.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*
class UserFragment : Fragment(){
    var fragmentView:View?=null
    var firestore:FirebaseFirestore?=null
    var uid:String?=null
    var auth: FirebaseAuth?=null
    var currnetUserUid:String?=null
    companion object{
        var PICK_PROFILE_FROM_ALBUM=10
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        fragmentView= LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid=arguments?.getString("destinationUid")
        firestore= FirebaseFirestore.getInstance()
        auth=FirebaseAuth.getInstance()
        currnetUserUid=auth?.currentUser?.uid

        if(currnetUserUid==uid){
            //MyPage
            fragmentView?.account_btn_follow_signout?.text= getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //Other Page
            fragmentView?.account_btn_follow_signout?.text= getString(R.string.follow)
            var mainactivity  = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity?.toolbar_btn_back.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId=R.id.action_home
            }

            mainactivity?.toolbar_title_image?.visibility=View.GONE
            mainactivity?.toolbar_username?.visibility=View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility=View.VISIBLE
            fragmentView?.account_btn_follow_signout?.setOnClickListener{

                requestFollow()
            }
        }
        fragmentView?.account_recyclerview?.layoutManager= GridLayoutManager(requireActivity(),3)
        fragmentView?.account_recyclerview?.adapter=UserFragmentRecyclerViewAdapter()


        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent=Intent(Intent.ACTION_PICK)
            photoPickerIntent.type="image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        getProfileImage()
        getFollowerAndFollowing()
    }

    fun followerAlarm(destinationUid:String){
        var alarmDTO= AlarmDTO()
        alarmDTO.destinationUid=destinationUid
        alarmDTO.userId=auth?.currentUser?.email
        alarmDTO.uid=auth?.currentUser?.uid
        alarmDTO.kind=2
        alarmDTO.timestamp=System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        var message=auth?.currentUser?.email+getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid,"HowlStargram",message)
    }

    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestroeException ->
            if(documentSnapshot==null) return@addSnapshotListener
            var followDTO=documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followerCount!=null){
                fragmentView?.account_tv_follower_count?.text=followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currnetUserUid!!)){
                    fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(requireActivity(),R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else{

                    if(uid!=currnetUserUid){
                        fragmentView?.account_btn_follow_signout?.text=getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter=null

                    }
                }
            }
            if(followDTO?.followingCount!=null){
                fragmentView?.account_tv_following_count?.text=followDTO?.followingCount?.toString()
            }
        }
    }

    fun requestFollow(){
        //save data to my account
        var tsDocFollowing=firestore?.collection("users")?.document(currnetUserUid!!)
        firestore?.runTransaction{
            var followDTO= it.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO= FollowDTO()
                followDTO!!.followingCount=1
                followDTO!!.followers[uid!!]=true
                it.set(tsDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followers.containsKey(uid)){
                //IT remove followint third person when a third person follow me
                followDTO?.followingCount=followDTO?.followingCount-1
                followDTO.followers?.remove(uid)
            }else{
                followDTO?.followingCount=followDTO?.followingCount+1
                followDTO.followers[uid!!]=true
            }
            it.set(tsDocFollowing,followDTO)
            return@runTransaction
        }
        //save data to third person
        var tsDocFollower= firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction {
            var followDTO=it.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO=FollowDTO()
                followDTO!!.followingCount=1
                followDTO!!.followers[currnetUserUid!!]=true
                followerAlarm(uid!!)
                it.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currnetUserUid)){
                //It cancel my followers when I follow a third person
                followDTO!!.followerCount=followDTO!!.followerCount-1
                followDTO!!.followers.remove(currnetUserUid!!)
            }else{
                followDTO!!.followerCount=followDTO!!.followerCount+1
                followDTO!!.followers[currnetUserUid!!]=true
                followerAlarm(uid!!)

            }
            it.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }

    }

    //프로필 사진 다운
    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSanpshot, firebaseFireStroeException ->
            if(documentSanpshot==null) return@addSnapshotListener
            if(documentSanpshot.data!=null){
                var uri=documentSanpshot?.data!!["image"]
                Glide.with(requireActivity())
                        .load(uri)
                        .apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }

    inner class UserFragmentRecyclerViewAdapter:RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs:ArrayList<ContentDTO> = arrayListOf()
        init{
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener{querySnapshot,firebaseFirestoreException->


                if(querySnapshot==null) return@addSnapshotListener

                //Get data
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text=contentDTOs.size.toString()
                notifyDataSetChanged()

            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width=resources.displayMetrics.widthPixels/3

            var imageview= ImageView(parent.context)
            imageview.layoutParams =  LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview=(holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }


    }
}