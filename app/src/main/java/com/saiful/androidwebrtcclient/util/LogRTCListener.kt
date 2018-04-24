package com.saiful.androidwebrtcclient.util

import android.util.Log

import org.webrtc.MediaStream

import me.kevingleason.pnwebrtc.PnPeer
import me.kevingleason.pnwebrtc.PnRTCListener
import me.kevingleason.pnwebrtc.PnRTCMessage


open class LogRTCListener : PnRTCListener() {
    override fun onCallReady(callId: String?) {
        Log.i("RTCListener", "OnCallReady - " + callId!!)
    }

    override fun onConnected(userId: String?) {
        Log.i("RTCListener", "OnConnected - " + userId!!)
    }

    override fun onPeerStatusChanged(peer: PnPeer?) {
        Log.i("RTCListener", "OnPeerStatusChanged - " + peer!!.toString())
    }

    override fun onPeerConnectionClosed(peer: PnPeer?) {
        Log.i("RTCListener", "OnPeerConnectionClosed - " + peer!!.toString())
    }

    override fun onLocalStream(localStream: MediaStream?) {
        Log.i("RTCListener", "OnLocalStream - " + localStream!!.toString())
    }

    override fun onAddRemoteStream(remoteStream: MediaStream?, peer: PnPeer?) {
        Log.i("RTCListener", "OnAddRemoteStream - " + peer!!.toString())
    }

    override fun onRemoveRemoteStream(remoteStream: MediaStream?, peer: PnPeer?) {
        Log.i("RTCListener", "OnRemoveRemoteStream - " + peer!!.toString())
    }

    override fun onMessage(peer: PnPeer?, message: Any?) {
        Log.i("RTCListener", "OnMessage - " + message!!.toString())
    }

    override fun onDebug(message: PnRTCMessage?) {
        Log.i("RTCListener", "OnDebug - " + message!!.message)
    }
}
