package com.DevAsh.recwallet.Home.Transactions

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recwallet.Context.*
import com.DevAsh.recwallet.Helper.AlertHelper
import com.DevAsh.recwallet.Helper.TransactionsHelper
import com.DevAsh.recwallet.Models.Contacts
import com.DevAsh.recwallet.Models.Transaction
import com.DevAsh.recwallet.R
import com.DevAsh.recwallet.SplashScreen
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import kotlinx.android.synthetic.main.activity_password_prompt.*
import kotlinx.android.synthetic.main.activity_single_object_transaction.*
import kotlinx.android.synthetic.main.activity_single_object_transaction.avatarContainer
import kotlinx.android.synthetic.main.activity_single_object_transaction.back
import kotlinx.android.synthetic.main.activity_single_object_transaction.cancel
import kotlinx.android.synthetic.main.activity_single_object_transaction.loadingScreen
import kotlinx.android.synthetic.main.activity_single_object_transaction.profile
import org.json.JSONArray

class SingleObjectTransaction : AppCompatActivity() {

    var allActivityAdapter: AllActivityAdapter?=null
    private lateinit var badge: TextView
    lateinit var context: Context


    var transaction = ArrayList<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_object_transaction)

        context=this

        badge = findViewById(R.id.badge)


        avatarContainer.setBackgroundColor(Color.parseColor(TransactionContext.avatarColor))

        val transactionObserver = Observer<ArrayList<Transaction>> {
            try{
                getData()
            }catch (e:Throwable){ }

        }

        loadAvatar()

        StateContext.model.allTransactions.observe(this,transactionObserver)

        badge.text = TransactionContext.selectedUser!!.name[0].toString()

        try {
            allActivityAdapter = Cache.singleObjecttransactionCache[TransactionContext.selectedUser!!.id.replace("+","")]!!
            transactionContainer.layoutManager = LinearLayoutManager(context)
            transactionContainer.adapter = allActivityAdapter

            scrollContainer.post {
                scrollContainer.fullScroll(View.FOCUS_DOWN)
                Handler().postDelayed({
                    loadingScreen.visibility = View.INVISIBLE
                    scrollContainer.visibility = View.VISIBLE
                },300)
            }
        }catch (e:Throwable){

        }

        if (TransactionContext.selectedUser!!.name.startsWith("+")) {
           badge.text = TransactionContext.selectedUser!!.name.subSequence(1, 3)
           badge.textSize = 18F
        }

        name.text = TransactionContext.selectedUser!!.name
        number.text = TransactionContext.selectedUser!!.number

        back.setOnClickListener{
            super.onBackPressed()
        }


        pay.setOnClickListener{
            startActivity(Intent(context,AmountPrompt::class.java))
        }

        cancel.setOnClickListener{
            onBackPressed()
        }


    }

    private fun loadAvatar(){
        UiContext.loadProfileImage(context,TransactionContext.selectedUser?.id!!,object:LoadProfileCallBack{
            override fun onSuccess() {
                if(!TransactionContext.selectedUser?.id!!.contains("rpay")){
                    profile.setBackgroundColor( context.resources.getColor(R.color.textDark))
                    profile.setColorFilter(Color.WHITE,  android.graphics.PorterDuff.Mode.SRC_IN)
                    profile.setPadding(35,35,35,35)
                }
                avatarContainer.visibility=View.GONE
                profile.visibility = View.VISIBLE
            }

            override fun onFailure() {
                avatarContainer.visibility= View.VISIBLE
                profile.visibility = View.GONE

            }

        },profile)
    }


    private fun getData(){
        transaction.clear()
        Handler().postDelayed({
            AndroidNetworking.get(
                ApiContext.apiUrl
                    + ApiContext.paymentPort
                    + "/getTransactionsBetweenObjects?id1=${DetailsContext.id}&id2=${TransactionContext.selectedUser!!.id.replace("+","")}")
                .addHeaders("jwtToken", DetailsContext.token)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONArray(object: JSONArrayRequestListener {
                    override fun onResponse(response: JSONArray?) {
                        val transactions = ArrayList<Transaction>()
                        val transactionObjectArray = response!!
                        println(transactionObjectArray)
                        for (i in 0 until transactionObjectArray.length()) {
                            val from = transactionObjectArray.getJSONObject(i).getJSONObject("From")
                            val to = transactionObjectArray.getJSONObject(i).getJSONObject("To")
                            val isSend =
                                TransactionsHelper.isSend(DetailsContext.id, from.getString("Id"))

                            val name = if (isSend) to.getString("Name") else from.getString("Name")
                            val number = if (isSend) to.getString("Number") else from.getString("Number")
                            val email = if (isSend) to.getString("Email") else from.getString("Email")
                            val id = if (isSend) to.getString("Id") else from.getString("Id")

                            val contacts = Contacts(name, number,id,email)
                            transactions.add(
                                Transaction(
                                    contacts = contacts,
                                    amount = transactionObjectArray.getJSONObject(i)["Amount"].toString(),
                                    time =(if (transactionObjectArray.getJSONObject(i)["From"] == DetailsContext.id)
                                        "Paid  "
                                    else "Received  ")+ SplashScreen.dateToString(
                                        transactionObjectArray.getJSONObject(
                                            i
                                        )["TransactionTime"].toString()
                                    ),
                                    type = if (isSend)
                                        "Send"
                                    else "Received",
                                    transactionId =  transactionObjectArray.getJSONObject(i)["TransactionID"].toString(),
                                    isGenerated = transactionObjectArray.getJSONObject(i).getBoolean("IsGenerated")
                                )
                            )
                        }

                        if(transactions.size!=allActivityAdapter?.itemCount) {
                            transaction = transactions
                            transactionContainer.layoutManager = LinearLayoutManager(context)
                            allActivityAdapter = AllActivityAdapter(transaction, context)
                            transactionContainer.adapter = allActivityAdapter
                            Cache.singleObjecttransactionCache[TransactionContext.selectedUser!!.id.replace("+","")] = allActivityAdapter!!
                            scrollContainer.post {
                                scrollContainer.fullScroll(View.FOCUS_DOWN)
                                Handler().postDelayed({
                                    loadingScreen.visibility = View.INVISIBLE
                                    scrollContainer.visibility = View.VISIBLE
                                },300)
                            }
                        }
                    }

                    override fun onError(anError: ANError?) {
                        AlertHelper.showServerError(this@SingleObjectTransaction)
                    }

                })
        },0)
    }
}

class AllActivityAdapter(private val items : ArrayList<Transaction>, val context: Context) : RecyclerView.Adapter<AllActivityViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllActivityViewHolder {
        return AllActivityViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_transactions, parent, false),context)
    }

    override fun onBindViewHolder(holder: AllActivityViewHolder, position: Int) {
        holder.amount.text = "${items[position].amount}"
        holder.time.text = items[position].time

        holder.item = items[position]

        if(items[position].type=="Received"){
            holder.container.gravity = Gravity.START
            holder.contentWidget.background= context.getDrawable(R.drawable.transaction_received_ripple)
        }
    }
}

class AllActivityViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null) : RecyclerView.ViewHolder(view) {
    val amount = view.findViewById(R.id.amount) as TextView
    val time = view.findViewById(R.id.time) as TextView
    val container = view.findViewById(R.id.container) as RelativeLayout
    val contentWidget = view.findViewById(R.id.contentWidget) as RelativeLayout

    init {
        view.setOnClickListener{
            TransactionContext.selectedTransaction = item
            context.startActivity(Intent(context,TransactionDetails::class.java))
        }
    }
}



