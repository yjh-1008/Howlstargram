package com.example.howlstargram.navigation.model

import android.app.Notification
import android.media.MediaSession2Service

data class PushDTO(
    var to:String?=null,
    var notification: Notification= Notification()
){
    data class Notification(
        var body:String?=null,
        var title:String?=null
    )
}
