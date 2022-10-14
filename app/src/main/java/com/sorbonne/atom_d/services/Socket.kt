package com.sorbonne.atom_d.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import com.sorbonne.atom_d.R
import com.sorbonne.atom_d.services.socket.SocketListener
import com.sorbonne.atom_d.services.socket.SocketViewModel
import com.sorbonne.atom_d.tools.JsonServerMessage
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class Socket : Service() {

    private val tag = Socket::class.simpleName
    private val viewModel = SocketViewModel()


    private var serviceRunningInForeground = false
    private val notificationId = 2
    private var notificationManager : NotificationManager?= null
    private val channelId = "channel_2"

    /*==========================================================================================*/
    //                                     socket
    /*==========================================================================================*/

    private var socketChannel : SocketChannel ?= null
    private var socketThread : Thread ?= null

    private var isSchedulerRunning : Boolean = false
    private var self : ScheduledFuture<*>?= null

    private val stack = Stack()

    private val schedulerLock = Object()
    private val stopAllLock   = Object()

    enum class JsonCommands{
        SOCKET_EXAMPLE,
        SERVER_MESSAGE,
        NOTIFY_SERVER
    }

    fun setListener(owner: LifecycleOwner, listener: SocketListener){
        viewModel.receivedMessage.observe(owner){ message ->
            listener.receivedMessage(message)
        }
    }

    fun initSocketConnection(serverAddress: SocketAddress, deviceId: String){
        socketThread = Thread(initSocket(serverAddress, deviceId))
        socketThread?.start()
        Log.e(tag,"initSocketConnection")
    }

    fun sendMessage(message: String){
        val buffer = ByteBuffer.allocate(message.length + 1)
        buffer.put(0, 0x01)
        val messageToByteArray = message.toByteArray()
        for(index in messageToByteArray.indices){
            buffer.put(index+1, messageToByteArray[index])
        }
        stack.push(buffer)
    }

    fun disconnectSocket(){

        socketChannel?.close()
        isSchedulerRunning = true //-> to block the scheduler

        synchronized(schedulerLock){
            schedulerLock.wait(100)
        }

        synchronized (stopAllLock){
            stopAllLock.notifyAll();
        }
        self?.cancel(true)
        socketThread?.interrupt()
        socketThread = null
    }

    fun getIsSchedulerRunning() : Boolean{
        return isSchedulerRunning
    }


    private fun readOnChannel(selectionKey : SelectionKey){
        val server = selectionKey.channel() as SocketChannel
        val buffer = ByteBuffer.allocate(1024)

        if(!server.isConnected){
            Log.e(tag, "socket is not connected")
            server.close()
            disconnectSocket()
            retryToReconnect()
            return
        }

        try {
            val messageSize = server.read(buffer)

            if(messageSize == -1){
                socketChannel?.close()
                return
            }
            buffer.flip()

            if(buffer[0].compareTo(0x01) == 0){
                val receivedMessage = ByteBuffer.allocate(messageSize-1)
                receivedMessage.put(buffer.array(), 1,messageSize-1)

                val decoder = Charset.forName("UTF-8").newDecoder()
                receivedMessage.flip()

                viewModel.receivedMessage.value = JSONObject(decoder.decode(receivedMessage).toString())
            }
        } catch ( e: IOException){
            e.printStackTrace()
//            disconnectSocket()
            retryToReconnect()
        }
    }

    private fun writeOnChannel(selectionKey: SelectionKey){
        val server = selectionKey.channel() as SocketChannel
        if(stack.getStackSize() > 0){
            stack.pop()?.let{ message ->
                server.write(message)
            }
        }
    }

    private fun retryToReconnect(){
        if(!isSchedulerRunning) {
            isSchedulerRunning = true
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            self = scheduler.scheduleWithFixedDelay(socketThread, 5, 5, TimeUnit.SECONDS)
        }
    }

    private fun initSocket(serverAddress : SocketAddress, deviceId: String) : Runnable{
        return Runnable {
            var selector : Selector ?= null

            try {
                selector = Selector.open()
                socketChannel = SocketChannel.open()

            } catch (e : IOException){
                e.printStackTrace()
            }

            socketChannel?.let { _channel ->
                selector?.let { _selector ->
                    _channel.configureBlocking(false)
                    _channel.register(_selector, SelectionKey.OP_CONNECT)
                    _channel.connect(serverAddress)
                    while (_selector.isOpen) {
                        _selector.select()
                        val iterator = _selector.selectedKeys().iterator()
                        while (iterator.hasNext()) {
                            val selection = iterator.next()
                            iterator.remove()
                            val mChannel = selection.channel() as SocketChannel

                            if (mChannel.isOpen) {
                                if(isSchedulerRunning){
                                    self?.cancel(false)
                                    isSchedulerRunning = false
                                }

                                if (selection.isConnectable) {
                                    try {
                                        mChannel.finishConnect()
                                        mChannel.register(
                                            _selector,
                                            SelectionKey.OP_READ or SelectionKey.OP_WRITE
                                        )

                                        val notifyServer = JsonServerMessage.newConnection( JsonServerMessage.NewConnectionOptions.RELAY , deviceId)
                                        sendMessage(notifyServer.toString())

                                    } catch (e: Exception) {
                                        Log.e(tag, "Unable to connect to the server")

                                        _selector.close()
                                        mChannel.close()

                                        retryToReconnect()
                                    }
                                }
                                if (mChannel.isConnected) {
                                    if (selection.isValid && selection.isReadable) {
                                        readOnChannel(selection)
                                    }

                                    if (selection.isValid && selection.isWritable) {
                                        writeOnChannel(selection)
                                    }
                                }
                            }
                        }
                        if(!_channel.isOpen && !isSchedulerRunning){
                            Log.w(tag, "disconnected from tcp socket")

                            synchronized(schedulerLock){
                                schedulerLock.notifyAll()
                                synchronized (stopAllLock){
                                    stopAllLock.wait(100)
                                }
                            }
                            if(!isSchedulerRunning) {
                                retryToReconnect()
                            }
                            break
                        }
                    }
                }
            }
        }
    }

    /*==========================================================================================*/
    //                                     Service
    /*==========================================================================================*/


    inner class LocalBinder: Binder(){
        fun getService(): Socket{
            return this@Socket
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder {
        serviceRunningInForeground = false
        return LocalBinder()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        serviceRunningInForeground = false
        stopForeground(true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)
        serviceRunningInForeground = true
        startForeground(notificationId, getNotification("AtomD connected in foreground"))
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun getNotification(notificationAlert : String) : Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentText(notificationAlert)
            .setContentTitle(ServiceUtils.getDeviceToDeviceTitle(this))
            .setSmallIcon(R.mipmap.ic_launcher)
        return builder.build()
    }


    override fun onDestroy() {
        super.onDestroy()
    }


    fun displayNotification(notificationAlert : String){
        if(serviceRunningInForeground){
            notificationManager?.notify(notificationId, getNotification(notificationAlert))
        }
    }

    fun isServiceRunningInForeground(): Boolean{
        return serviceRunningInForeground
    }

}

/*==========================================================================================*/
//                                     socket stack
/*==========================================================================================*/

class StackOfBuffers{
    private var buffer : ByteBuffer ?= null
    private var next : StackOfBuffers? = null

    fun setBuffer(mBuffer: ByteBuffer){
        buffer = mBuffer
    }

    fun getBuffer() : ByteBuffer?{
        return buffer
    }

    fun setNext(stackOfBuffers: StackOfBuffers){
        next = stackOfBuffers
    }

    fun getNext() : StackOfBuffers?{
        return next
    }
}

class Stack {
    private val tag = Stack::class.simpleName

    private var stackSize = 0
    private var stackOfBuffers: StackOfBuffers? = null

    init {
        stackOfBuffers = StackOfBuffers()
    }

    fun push(buffer: ByteBuffer) {
        this.stackOfBuffers?.let {
            val newStackOfBuffers = StackOfBuffers()

            newStackOfBuffers.setBuffer(buffer)
            newStackOfBuffers.setNext(it)

            this.stackOfBuffers = newStackOfBuffers
            this.stackSize += 1
        }
    }

    fun pop(): ByteBuffer? {
        var mBuffer: ByteBuffer? = null
        stackOfBuffers?.let {
            mBuffer = it.getBuffer()
            this.stackOfBuffers = this.stackOfBuffers!!.getNext()
            this.stackSize -= 1
        }
        return mBuffer
    }

    fun getStackSize(): Int {
        return stackSize
    }
}