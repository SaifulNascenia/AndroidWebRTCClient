package com.saiful.androidwebrtcclient.adt


class ChatMessage(val sender: String, val message: String, val timeStamp: Long) {

    override fun hashCode(): Int {
        return (this.sender + this.message + this.timeStamp).hashCode()
    }
}
