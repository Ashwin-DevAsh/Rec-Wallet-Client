package com.DevAsh.recwallet.Sync

import com.DevAsh.recwallet.Context.ApiContext
import com.DevAsh.recwallet.Context.DetailsContext
import com.DevAsh.recwallet.Context.StateContext
import com.DevAsh.recwallet.Helper.SnackBarHelper
import com.DevAsh.recwallet.Home.HomePage
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import org.json.JSONObject
import java.text.DecimalFormat

object SocketHelper {

    private val url = ApiContext.apiUrl+ ApiContext.syncPort
    lateinit var socket:Socket

    fun connect(){
        println("Called . . .")
        socket = IO.socket(url)
        socket.connect()

        socket.on("connect") {
            println("connecting ....")
            val data = JSONObject()
            data.put("number",DetailsContext.phoneNumber)
            data.put("fcmToken",DetailsContext.fcmToken)
            socket.emit("getInformation",data)
        }

        socket.on("doUpdate"){
              println("updating ....")
              getState()
        }

        socket.on("disconnect"){
            println("disconnecting...")
        }

    }

    private fun getState(){
        AndroidNetworking.get(ApiContext.apiUrl + ApiContext.paymentPort + "/getState")
            .addHeaders("jwtToken",DetailsContext.token)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    StateContext.state = response
                }

                override fun onError(anError: ANError?) {
                    socket.disconnect()
                }

            })
    }
}