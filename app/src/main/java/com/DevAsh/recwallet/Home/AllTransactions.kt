package com.DevAsh.recwallet.Home


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recwallet.Context.StateContext
import com.DevAsh.recwallet.Context.TransactionContext
import com.DevAsh.recwallet.Context.UiContext.colors
import com.DevAsh.recwallet.Home.Transactions.TransactionDetails
import com.DevAsh.recwallet.Models.Transaction
import com.DevAsh.recwallet.R
import kotlinx.android.synthetic.main.activity_all_transactions.*
import kotlinx.android.synthetic.main.activity_all_transactions.activity
import kotlin.collections.ArrayList


class AllTransactions : AppCompatActivity() {
    lateinit var context: Context
    lateinit var activityAdapter:AllActivityAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)


        context = this


        val transactionObserver = Observer<ArrayList<Transaction>> {updatedList->
         activityAdapter.updateList(updatedList)
        }

        StateContext.model.allTransactions.observe(this,transactionObserver)
        activityAdapter =AllActivityAdapter(StateContext.model.allTransactions.value!!,context)
        Handler().postDelayed({
            activity.layoutManager = LinearLayoutManager(context)
            activity.adapter = activityAdapter
            mainContent.visibility=View.VISIBLE
        },700)
    }

}

class AllActivityAdapter(private var items : ArrayList<Transaction>, val context: Context) : RecyclerView.Adapter<AllActivityViewHolder>() {



    var colorIndex = 0

    var colorMap = HashMap<String,String>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllActivityViewHolder {
        return AllActivityViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_listtile_transaction, parent, false),context)
    }

    override fun onBindViewHolder(holder: AllActivityViewHolder, position: Int) {
        holder.title.text = items[position].name
        holder.subtitle.text = items[position].time
        holder.badge.text = items[position].name[0].toString()

        if (items[position].name.startsWith("+")) {
            holder.badge.text = items[position].name.subSequence(1, 3)
            holder.badge.textSize = 18F
        }

        holder.item = items[position]

        try {
            holder.badge.setBackgroundColor(Color.parseColor(colorMap[items[position].number]))
            holder.color = colorMap[items[position].number]

        }catch (e:Throwable){
            holder.badge.setBackgroundColor(Color.parseColor(colors[colorIndex]))
            colorMap[items[position].number] = colors[colorIndex]
            holder.color = colors[colorIndex]
            colorIndex = (colorIndex+1)%colors.size
        }



        if(items[position].isGenerated){

            holder.badge.text = "RC"
            holder.logo.visibility = View.VISIBLE
            holder.title.text="Added to wallet"
            holder.badge.textSize = 14F
            holder.additionalInfo.setTextColor(Color.parseColor("#ff9100"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#25ff9100"))
            holder.additionalInfo.text= "+${items[position].amount} ${TransactionContext.currency}"
            holder.badge.setBackgroundColor(context.getColor(R.color.highlightButton))
            holder.color = "#035aa6"

        }else if(items[position].type=="Received"){
            holder.additionalInfo.setTextColor(Color.parseColor("#1b5e20"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#151b5e20"))
            holder.additionalInfo.text= "+${items[position].amount} ${TransactionContext.currency}"
        }else if(items[position].type=="Send"){
            holder.additionalInfo.setTextColor(Color.parseColor("#d50000"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#15d50000"))
            holder.additionalInfo.text= "-${items[position].amount} ${TransactionContext.currency}"
        }
    }

    fun updateList(updatedList : ArrayList<Transaction>){
        this.items = updatedList
        notifyDataSetChanged()
    }
}

class AllActivityViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView
    val additionalInfo = view.findViewById(R.id.additionalInfo) as TextView
    val logo = view.findViewById<ImageView>(R.id.logo)

    init {
        view.setOnClickListener{
             TransactionContext.selectedTransaction = item
             TransactionContext.avatarColor = color!!
             context.startActivity(Intent(context,TransactionDetails::class.java))
        }
    }
}


