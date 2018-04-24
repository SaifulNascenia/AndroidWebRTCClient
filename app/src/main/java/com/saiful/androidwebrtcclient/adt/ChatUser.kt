package com.saiful.androidwebrtcclient.adt


import com.saiful.androidwebrtcclient.util.Constants

/**
 * Created by GleasonK on 7/31/15.
 */
class ChatUser {
    var userId: String? = null
        private set
    var status: String? = null

    constructor(userId: String) {
        this.userId = userId
        this.status = Constants.STATUS_OFFLINE
    }

    constructor(userId: String, status: String) {
        this.userId = userId
        this.status = status
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is ChatUser) return false
        val cu = o as ChatUser?
        return this.userId == o.userId
    }

    override fun hashCode(): Int {
        return this.userId!!.hashCode()
    }
}
