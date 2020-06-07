package com.DevAsh.recwallet.Home.Transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recwallet.Context.ApiContext
import com.DevAsh.recwallet.Context.DetailsContext
import com.DevAsh.recwallet.Context.TransactionContext
import com.DevAsh.recwallet.Helper.SnackBarHelper
import com.DevAsh.recwallet.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.jacksonandroidnetworking.JacksonParserFactory
import kotlinx.android.synthetic.main.activity_send_money.*
import org.json.JSONArray


class SendMoney : AppCompatActivity() {
    var userAdapter: UserAdapter = UserAdapter(TransactionContext.allUsers,this)
    lateinit var context:Context
    lateinit var searchHandler: Handler
    lateinit var usersContainer: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_money)
        AndroidNetworking.initialize(applicationContext)
        AndroidNetworking.setParserFactory(JacksonParserFactory())

        context=this


        usersContainer = findViewById(R.id.usersContainer)

        Handler().postDelayed({
            getAllUsers()
        },0)

        back.setOnClickListener{
            super.onBackPressed()
        }




        search.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                try {
                    searchHandler.removeCallbacksAndMessages("")
                } catch (e: Exception) {

                }
                searchHandler = Handler()
                searchHandler.postDelayed({
                    val updatedList = ArrayList<Contacts>()
                    for(i in TransactionContext.allUsers){
                        if(i.name.toLowerCase().contains((s.toString().toLowerCase()))){
                            updatedList.add(i)
                        }
                    }
                    userAdapter.updateList(updatedList)
                    if(updatedList.isEmpty()){
                        peopleHeading.visibility=INVISIBLE
                    }else{
                        peopleHeading.visibility=VISIBLE
                    }
                },0)

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })


    }

    override fun onResume(){
        if(TransactionContext.allUsers.isEmpty()){
            peopleHeading.visibility=INVISIBLE
        }else{
            peopleHeading.visibility=VISIBLE
        }
        super.onResume()
    }


    private fun getAllUsers(){
        TransactionContext.allUsers.clear()
        AndroidNetworking.get(ApiContext.apiUrl+ApiContext.registrationPort+"/getUsers")
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONArray(object :JSONArrayRequestListener{
                override fun onResponse(response: JSONArray?) {
                      if(response!=null)

                      for(i in 0 until response.length()){
                          val user = Contacts(
                               response.getJSONObject(i)["name"].toString()
                              ,"+"+response.getJSONObject(i)["number"].toString()
                          )
                          if(user.number!="+"+DetailsContext.phoneNumber) TransactionContext.allUsers.add(user)
                      }
                        usersContainer.layoutManager = LinearLayoutManager(context)
                        userAdapter = UserAdapter(TransactionContext.allUsers,context)
                        usersContainer.adapter = userAdapter

                        if(TransactionContext.allUsers.isEmpty()){
                            peopleHeading.visibility= INVISIBLE
                        }else{
                            peopleHeading.visibility= VISIBLE

                        }
                        mainContent.visibility= VISIBLE
                }

                override fun onError(anError: ANError?) {
                    SnackBarHelper.showError(mainContent,anError!!.errorDetail)
                    Handler().postDelayed({
                        onBackPressed()
                    },3000)
                }

            })

    }
}


class UserAdapter(private var items : ArrayList<Contacts>, val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_listtile, parent, false),context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items[position].name
        holder.subtitle.text = items[position].number
        holder.badge.text = items[position].name[0].toString()

        if (items[position].name.startsWith("+")) {
            holder.badge.text = items[position].name.subSequence(1, 3)
            holder.badge.textSize = 18F
        }
    }

    fun updateList(updatedList : ArrayList<Contacts>){
         this.items = updatedList
         notifyDataSetChanged()
    }
}

class ViewHolder (view: View,context: Context) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView

    init {
        view.setOnClickListener{
           TransactionContext.selectedUser= Contacts(title.text.toString(),subtitle.text.toString())
           startActivity(context,Intent(context,SingleObjectTransaction::class.java),null)
        }
    }
}


class Contacts(val name: String, val number: String){
    override fun equals(other: Any?): Boolean {
        return (other as Contacts).number == this.number
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + number.hashCode()
        return result
    }
}

