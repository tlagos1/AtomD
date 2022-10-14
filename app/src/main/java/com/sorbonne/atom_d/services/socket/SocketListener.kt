package com.sorbonne.atom_d.services.socket

import org.json.JSONObject

interface SocketListener {
    fun receivedMessage(message: JSONObject){}
}