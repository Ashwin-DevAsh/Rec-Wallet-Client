package com.DevAsh.recwallet.rBusiness.Home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.DevAsh.recbusiness.Home.Store.MyStore
import com.DevAsh.recwallet.Context.*
import com.DevAsh.recwallet.Database.Credentials
import com.DevAsh.recwallet.Database.ExtraValues
import com.DevAsh.recwallet.Home.QrScanner
import com.DevAsh.recwallet.Home.Transactions.AddMoney
import com.DevAsh.recwallet.Home.Transactions.AllTransactions
import com.DevAsh.recwallet.Home.Transactions.SendMoney
import com.DevAsh.recwallet.Home.Transactions.TransactionDetails
import com.DevAsh.recwallet.Home.Withdraw.AccountDetails
import com.DevAsh.recwallet.Home.Withdraw.AddAccounts
import com.DevAsh.recwallet.Home.Withdraw.WithdrawAmountPrompt
import com.DevAsh.recwallet.Models.BankAccount
import com.DevAsh.recwallet.Models.Transaction
import com.DevAsh.recwallet.R
import com.DevAsh.recwallet.Sync.SocketHelper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.iid.FirebaseInstanceId
import com.opencsv.CSVWriter
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_home_page.balance
import kotlinx.android.synthetic.main.activity_home_page.bankAccounts
import kotlinx.android.synthetic.main.activity_home_page.buyMoney
import kotlinx.android.synthetic.main.activity_home_page.greetings
import kotlinx.android.synthetic.main.activity_home_page.profile
import kotlinx.android.synthetic.main.activity_home_page.scroller
import kotlinx.android.synthetic.main.activity_home_page.sendMoney
import kotlinx.android.synthetic.main.activity_home_page.withdraw
import kotlinx.android.synthetic.main.bank_accounts.view.*

import kotlinx.android.synthetic.main.widget_accounts.view.*
import kotlinx.android.synthetic.main.widget_listtile_transaction.view.*
import kotlinx.android.synthetic.rBusiness.activity_home_page.*
import kotlinx.android.synthetic.rBusiness.payments_bottom_sheet.recentPayments
import kotlinx.android.synthetic.rBusiness.payments_bottom_sheet.view.*
import kotlinx.android.synthetic.rBusiness.set_time_bottomsheet.view.*
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class HomePage : AppCompatActivity() {

    var context: Context = this

    var time = mapOf(
        0 to "Past 1 Hour",
        1 to "Past 3 Hour",
        2 to "Past 5 Hour",
        3 to "Today",
        4 to "Past 3 days",
        5 to "Past 7 days",
        6 to "Past 1 month",
        7 to "Past 6 month"
    )

    lateinit var recentPaymentsAdapter: RecentPaymentsAdapter
    lateinit var extraValues: ExtraValues

    var bottomSheetPeople:BottomSheetPeople?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        extraValues = try{
            Realm.getDefaultInstance().where(ExtraValues::class.java).findFirst()!!
        }catch (e:Throwable){
            println(e)
            ExtraValues()
        }
        StateContext.timeIndex = extraValues.timeIndex
        timeline.text = time[StateContext.timeIndex]

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("Failed . . . .")
                    return@OnCompleteListener
                } else {
                    println("success . . . .")
                    HelperVariables.fcmToken = task.result?.token!!
                    SocketHelper.connect()
                }
            })

        loadProfilePicture()

        recentPaymentsAdapter = RecentPaymentsAdapter(arrayListOf(),this)
        recentPayments.layoutManager=LinearLayoutManager(this)
        recentPayments.adapter = recentPaymentsAdapter
        loadObservers()

        balance.setOnClickListener{
            startActivity(Intent(this,
                AllTransactions::class.java))
        }

        profile.setOnClickListener{
            startActivity(Intent(context, com.DevAsh.recbusiness.Home.Profile::class.java))
        }

        sendMoney.setOnClickListener{
            val permissions = arrayOf(android.Manifest.permission.READ_CONTACTS)
            if(packageManager.checkPermission(android.Manifest.permission.READ_CONTACTS,context.packageName)==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context, SendMoney::class.java))
            }else{
                ActivityCompat.requestPermissions(this, permissions,0)
            }
        }

        buyMoney.setOnClickListener{
            startActivity(Intent(context, AddMoney::class.java))
        }

        setTime.setOnClickListener{
             openTimeSheet()
        }

        viewPayments.setOnClickListener{
            bottomSheetPeople = BottomSheetPeople(this, updatePaymentsListener())
            bottomSheetPeople?.openBottomSheet()
        }

        scanner.setOnClickListener{
            val permissions = arrayOf(android.Manifest.permission.CAMERA)
            if(packageManager.checkPermission(android.Manifest.permission.CAMERA,context.packageName)==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context, QrScanner::class.java))
            }else{
                ActivityCompat.requestPermissions(this, permissions,1)
            }
        }

        qrCode.setOnClickListener{
            startActivity(Intent(context, DisplayQrcode::class.java))
        }

        myStore.setOnClickListener{
            startActivity(Intent(this, MyStore::class.java))
        }

        bankAccounts.setOnClickListener{

            val totalAccounts = StateContext.model.bankAccounts.value?.size
            if(totalAccounts!=null && totalAccounts!=0){
                BottomSheetAccounts(this).openBottomSheet()
            }else{
                startActivity(Intent(context, AddAccounts::class.java))
            }

        }

        withdraw.setOnClickListener{
            val intent = Intent(this, WithdrawAmountPrompt::class.java)
            startActivity(intent)
        }

        hideButton()

    }

    private fun hideButton(){
        val hiddenPanel = findViewById<CardView>(R.id.scanContainer)
        val bottomDown: Animation = AnimationUtils.loadAnimation(
            context,
            R.anim.button_down
        )
        val bottomUp: Animation = AnimationUtils.loadAnimation(
            context,
            R.anim.button_up
        )
        scroller.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY >200) {
                if(hiddenPanel.visibility==View.VISIBLE){
                    hiddenPanel.startAnimation(bottomDown)
                    hiddenPanel.visibility=View.GONE
                }

            }
            if(scrollY < 200){
                if(hiddenPanel.visibility==View.GONE){
                    hiddenPanel.visibility=View.VISIBLE
                    hiddenPanel.startAnimation(bottomUp)
                }
            }
        }
    }

   private fun updatePayments(index:Int = StateContext.timeIndex){
       timeline.text = time[index]
   }

    private fun loadObservers(){
        val balanceObserver = Observer<String> { currentBalance ->
            balance.text = currentBalance
        }

        val paymentsObserver = Observer<ArrayList<Transaction>> { updatedList->
          updatePaymentsListener(updatedList)
        }

        greetings.text=(getText())
        StateContext.model.currentBalance.observe(this,balanceObserver)
        StateContext.model.allTransactions.observe(this,paymentsObserver)
    }

    private fun updatePaymentsListener(updatedList:ArrayList<Transaction> = StateContext.model.allTransactions.value!!):ArrayList<Transaction>{
        val updateListTemp = arrayListOf<Transaction>()
        for(i in updatedList){
            if(i.type=="Received" && !i.isGenerated){
                if( filterWithTimeStamp(i.timeStamp.toString()))
                  updateListTemp.add(i)
                else
                    break
            }

        }
        if(updateListTemp.size>0){
            noPayments.visibility=View.GONE
            recentPaymentsContainer.visibility = View.VISIBLE
        }else{
            noPayments.visibility=View.VISIBLE
            recentPaymentsContainer.visibility = View.GONE
        }


        if(updateListTemp.size<5){
            recentPaymentsAdapter.updateList(updateListTemp)
        }else{
            recentPaymentsAdapter.updateList(ArrayList(updateListTemp.subList(0,5)))

        }

        recentPayments.smoothScrollToPosition(0)
        return updateListTemp
    }

    private fun filterWithTimeStamp(timestampString: String):Boolean{

        val compareTime = mapOf(
            0 to Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)),
            1 to Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)),
            2 to Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5)),
            3 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)),
            4 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)),
            5 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)),
            6 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)),
            7 to Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30*6))
        )

        val time = timestampString.replace("T"," ").substring(0,timestampString.lastIndex)
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val transactionTime: Date = parser.parse(time)
        if(transactionTime.after(compareTime[StateContext.timeIndex])){
            return true
        }
        return false
    }


    private fun loadProfilePicture(){
        UiContext.loadProfileImage(
            context,
            Credentials.credentials.id,
            object : LoadProfileCallBack {
                override fun onSuccess() {
                    profile.background=resources.getDrawable(R.drawable.image_avatar)
                    profile.setPadding(35,35,35,35)
                }

                override fun onFailure() {

                }
            },
            profile,
            R.drawable.profile
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==0){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context, SendMoney::class.java))
            }
        }else if(requestCode==1){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                startActivity(Intent(context, QrScanner::class.java))
            }
        }else if(requestCode==10){
         if(grantResults[0]==PackageManager.PERMISSION_GRANTED ){
             bottomSheetPeople?.exportCsv()
        }
    }
    }

    private fun getText():String{
        val name = Credentials.credentials.accountName!!
        if(name.length>10){
            val splitedText = name.split(" ")
            if(splitedText.size>1){
                val firstName = splitedText[0]
                val secondName = splitedText[1]
                if(firstName.length<3){
                    return secondName
                }
                return firstName
            }else{
                return splitedText[0]
            }
        }else{
            return name
        }
    }

    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    override fun onResume() {
        if(UiContext.isProfilePictureChanged){
            UiContext.UpdateImage(profile)
            UiContext.isProfilePictureChanged=false
            profile.background=( context.resources.getDrawable(R.drawable.image_avatar))
            profile.setColorFilter(context.resources.getColor(R.color.textDark),  android.graphics.PorterDuff.Mode.SRC_IN)
            profile.setPadding(35,35,35,35)

        }
        super.onResume()
    }


    private fun openTimeSheet(){
        val mBottomSheetDialog = BottomSheetDialog(context)
        val sheetView: View = LayoutInflater.from(context).inflate(R.layout.set_time_bottomsheet, null)

        fun updateBackground(view: View, index:Int){
            if(StateContext.timeIndex==index){
                view.background=getDrawable(R.color.colorPrimary)
            }else{
                view.background=getDrawable(R.color.white)
            }
        }

        fun setBackground(){
            updateBackground( sheetView.oneHour,0)
            updateBackground( sheetView.threeHour,1)
            updateBackground( sheetView.fiveHour,2)
            updateBackground( sheetView.today,3)
            updateBackground( sheetView.threeDay,4)
            updateBackground( sheetView.sevenDay,5)
            updateBackground( sheetView.oneMonth,6)
            updateBackground( sheetView.sixMonth,7)
        }

        fun onClick(index: Int){
            StateContext.timeIndex=index
            Realm.getDefaultInstance().executeTransactionAsync{realm->
                val extraValues = realm.where(ExtraValues::class.java).findFirst()
                if(extraValues!=null ){
                  extraValues.timeIndex=index
                }else{
                    val newExtraValues = ExtraValues()
                    newExtraValues.timeIndex = StateContext.timeIndex
                    realm.insert(newExtraValues)
                }
            }
            updatePayments(index)
            mBottomSheetDialog.cancel()
            setBackground()
            updatePaymentsListener()
        }

        setBackground()


        sheetView.oneHour.setOnClickListener{
          onClick(0)
        }
        sheetView.threeHour.setOnClickListener{
            onClick(1)


        }
        sheetView.fiveHour.setOnClickListener{
            onClick(2)

        }
        sheetView.today.setOnClickListener{
            onClick(3)


        }
        sheetView.threeDay.setOnClickListener{
            onClick(4)

        }
        sheetView.sevenDay.setOnClickListener{
            onClick(5)

        }
        sheetView.oneMonth.setOnClickListener{
            onClick(6)

        }
        sheetView.sixMonth.setOnClickListener{
            onClick(7)

        }

        mBottomSheetDialog.setContentView(sheetView)
        mBottomSheetDialog.show()


    }



}

class BottomSheetAccounts(val context:Context):BottomSheet{
    private val mBottomSheetDialog = BottomSheetDialog(context)
    private val sheetView: View = LayoutInflater.from(context).inflate(R.layout.bank_accounts, null)
    private val bankAccounts = if(StateContext.model.bankAccounts.value!=null)
        StateContext.model.bankAccounts.value
    else arrayListOf()
    init {
        val accountsContainer = sheetView.findViewById<RecyclerView>(R.id.accountsContainer)
        sheetView.addAccounts.setOnClickListener{
            closeBottomSheet()
            context.startActivity(Intent(context, AddAccounts::class.java))
        }


        accountsContainer.layoutManager = LinearLayoutManager(context)
        accountsContainer.adapter = AccountsViewAdapter(
            bankAccounts!!,
            context,this)
        mBottomSheetDialog.setContentView(sheetView)
    }
    override fun openBottomSheet(){
        mBottomSheetDialog.show()
    }

    override fun closeBottomSheet() {
        mBottomSheetDialog.cancel()
    }

}

class BottomSheetPeople(val context:Activity,val transactions:ArrayList<Transaction>):BottomSheet{
    private val mBottomSheetDialog = BottomSheetDialog(context)
    private val sheetView: View = LayoutInflater.from(context).inflate(R.layout.payments_bottom_sheet, null)
    init {
        val peopleContainer = sheetView.findViewById<RecyclerView>(R.id.recentPayments)
        peopleContainer.adapter = RecentPaymentsAdapter(transactions,context,this)
        peopleContainer.layoutManager = LinearLayoutManager(context)
        mBottomSheetDialog.setContentView(sheetView)
        sheetView.share.setOnClickListener{
            val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(context.packageManager.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,context.packageName)== PackageManager.PERMISSION_GRANTED ){
                Handler().postDelayed({
                    exportCsv((peopleContainer.adapter as RecentPaymentsAdapter).items)
                },0)
            }else{
                ActivityCompat.requestPermissions(context, permissions,10)
            }

        }
    }
    override fun openBottomSheet(){
        mBottomSheetDialog.show()
    }

    override fun closeBottomSheet() {
        mBottomSheetDialog.cancel()
    }

     fun exportCsv(transaction:List<Transaction> = transactions) {
        try {
            val root = File(Environment.getExternalStorageDirectory(), "rBusiness")
            if (!root.exists()) {
                root.mkdirs()
            }
            val gpxfile = File(root, "Payments.csv")
            val writer =  CSVWriter(FileWriter(gpxfile))
            val data = arrayListOf(
                arrayOf("Name","id","email","amount","time")
            )
            for(i in transaction){
                println(i.time)
                data.add(
                    arrayOf(i.contacts.name,i.contacts.id,i.contacts.email,i.amount+" Rc",
                        i.time)
                )
            }
            writer.writeAll(data)
            writer.close()
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "text/plain"
            val uri: Uri = Uri.fromFile(gpxfile)
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(emailIntent, "Pick a provider"))
        } catch ( e: IOException) {
            e.printStackTrace()
        }


    }

}



class RecentPaymentsAdapter(var items : List<Transaction>, val context: Context, private val openSheet: BottomSheet?=null) : RecyclerView.Adapter<RecentActivityViewHolder>() {

    private var colorIndex = 0

    private var colorMap = HashMap<String,String>()
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentActivityViewHolder {
        return RecentActivityViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_listtile_transactions_home, parent, false),context,openSheet = openSheet)
    }

    override fun onBindViewHolder(holder: RecentActivityViewHolder, position: Int) {
        holder.title.text = items[position].contacts.name
        holder.subtitle.text = items[position].time
        holder.badge.text = items[position].contacts.name[0].toString()

        if (items[position].contacts.name.startsWith("+")) {
            holder.badge.text = items[position].contacts.name.subSequence(1, 3)
            holder.badge.textSize = 18F
        }

        holder.item = items[position]

        try {
            holder.badge.setBackgroundColor(Color.parseColor(colorMap[items[position].contacts.id]))
            holder.color = colorMap[items[position].contacts.id]

        }catch (e:Throwable){
            holder.badge.setBackgroundColor(Color.parseColor(UiContext.colors[colorIndex]))
            colorMap[items[position].contacts.id] = UiContext.colors[colorIndex]
            holder.color = UiContext.colors[colorIndex]
            colorIndex = (colorIndex+1)% UiContext.colors.size
        }

        if(items[position].contacts.id.startsWith("rmart")){
            holder.badge.text = "RC"
            holder.logo.visibility = View.VISIBLE
            holder.title.text="rMart"
            holder.badge.textSize = 14F
            holder.logo.setBackgroundColor(Color.parseColor("#fe724c"))
            holder.color = "#fe724c"
        }

        UiContext.loadProfileImageWithoutPlaceHolder(items[position].contacts.id, object: LoadProfileCallBack {
            override fun onSuccess() {
                holder.badge.visibility=View.GONE
                holder.profile.visibility = View.VISIBLE

            }

            override fun onFailure() {
                holder.badge.visibility= View.VISIBLE
                holder.profile.visibility = View.GONE

            }

        }, holder.profile)


        if(items[position].isGenerated){

            holder.badge.text = "RC"
            holder.logo.visibility = View.VISIBLE
            holder.title.text="Added to wallet"
            holder.badge.textSize = 14F
            holder.additionalInfo.setTextColor(Color.parseColor("#ff9100"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#25ff9100"))
            holder.additionalInfo.text= "+${items[position].amount} ${HelperVariables.currency}"
            holder.badge.setBackgroundColor(context.getColor(R.color.highlightButton))
            holder.color = "#035aa6"

        }else if(items[position].type=="Received"){
            holder.additionalInfo.setTextColor(Color.parseColor("#1b5e20"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#151b5e20"))
            holder.additionalInfo.text= "+${items[position].amount} ${HelperVariables.currency}"
        }else if(items[position].type=="Send"){
            holder.additionalInfo.setTextColor(Color.parseColor("#d50000"))
            holder.additionalInfo.setBackgroundColor(Color.parseColor("#15d50000"))
            holder.additionalInfo.text= "-${items[position].amount} ${HelperVariables.currency}"
        }
    }

    fun updateList(updatedList : ArrayList<Transaction>){
        this.items = updatedList
        notifyDataSetChanged()

    }


}

class RecentActivityViewHolder (view: View,context: Context,var item:Transaction?=null,var color:String?=null,openSheet: BottomSheet?=null) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById(R.id.title) as TextView
    val subtitle = view.findViewById(R.id.subtitle) as TextView
    val badge = view.findViewById(R.id.badge) as TextView
    val additionalInfo = view.findViewById(R.id.additionalInfo) as TextView
    val logo = view.findViewById<ImageView>(R.id.logo)
    val profile = view.profile

    init {
        view.setOnClickListener{
            HelperVariables.selectedTransaction = item
            HelperVariables.avatarColor = color!!
            context.startActivity(Intent(context, TransactionDetails::class.java))
            openSheet?.closeBottomSheet()
        }
    }
}

class AccountsViewAdapter(private var items : ArrayList<BankAccount>, val context: Context, private val openBottomSheetCallback: BottomSheet?) : RecyclerView.Adapter<AccountsViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsViewHolder {
        return AccountsViewHolder(LayoutInflater.from(context).inflate(R.layout.widget_accounts, parent, false),context,openBottomSheetCallback)
    }

    override fun onBindViewHolder(holder: AccountsViewHolder, position: Int) {
        holder.account= items[position]
        holder.accountName.text = items[position].bankName
        holder.accountNumber.text = "XXXX XXXX XXXX "+items[position].accountNumber.substring(
            items[position].accountNumber.length-4,
            items[position].accountNumber.length)
    }
}

class AccountsViewHolder (view: View, context: Context, private val openBottomSheetCallback: BottomSheet?) : RecyclerView.ViewHolder(view){

    var account:BankAccount?=null
    val accountName = view.accountName
    val accountNumber = view.accountNumber

    init {

        view.setOnClickListener{
            openBottomSheetCallback?.closeBottomSheet()
            HelperVariables.selectedAccount = account
            context.startActivity(Intent(context, AccountDetails::class.java))
        }

        view.edit.setOnClickListener{
            openBottomSheetCallback?.closeBottomSheet()
        }
    }
}

interface BottomSheet{
    fun openBottomSheet()
    fun closeBottomSheet()
}