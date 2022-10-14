package com.sorbonne.atom_d.tools

import com.sorbonne.atom_d.services.Socket
import org.json.JSONObject

class JsonServerMessage {

    enum class NewConnectionOptions {
        D2D_DEVICE_CONNECTION,
        RELAY
    }

    companion object {
        fun newConnection(type: NewConnectionOptions, id: String): JSONObject {

            val jsonMessage = JSONObject()
            val messageParameter = JSONObject()

            jsonMessage.put("command", Socket.JsonCommands.NOTIFY_SERVER.ordinal)
            jsonMessage.put("value", "NEW CONNECTION")

            messageParameter.put("type", type.name)
            messageParameter.put("id", id)

            jsonMessage.put("parameters", messageParameter)

            return jsonMessage
        }

        fun isAliveMessage(value: String): JSONObject {
            val jsonMessage = JSONObject()
            val messageParameter = JSONObject()

            jsonMessage.put("command", Socket.JsonCommands.NOTIFY_SERVER.ordinal)
            jsonMessage.put("value", value)

            return jsonMessage
        }

        fun d2dDisconnection(id: String) : JSONObject{

            val jsonMessage = JSONObject()
            val messageParameter = JSONObject()

            jsonMessage.put("command", Socket.JsonCommands.NOTIFY_SERVER.ordinal)
            jsonMessage.put("value", "D2D DISCONNECTION")

            messageParameter.put("id", id)

            jsonMessage.put("parameters", messageParameter)

            return jsonMessage
        }
    }
}