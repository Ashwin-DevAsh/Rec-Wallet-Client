package com.DevAsh.recwallet.Home.Withdraw

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.DevAsh.recwallet.Context.ApiContext
import com.DevAsh.recwallet.Context.DetailsContext
import com.DevAsh.recwallet.Context.HelperVariables
import com.DevAsh.recwallet.Context.StateContext
import com.DevAsh.recwallet.Database.BankAccount
import com.DevAsh.recwallet.Database.RealmHelper
import com.DevAsh.recwallet.Helper.AlertHelper
import com.DevAsh.recwallet.R
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_account_details.*
import kotlinx.android.synthetic.main.activity_account_details.accountNumber
import kotlinx.android.synthetic.main.activity_account_details.bankName
import kotlinx.android.synthetic.main.activity_account_details.cancel
import kotlinx.android.synthetic.main.activity_account_details.holderName
import kotlinx.android.synthetic.main.activity_account_details.ifsc
import kotlinx.android.synthetic.main.activity_account_details.mainContent
import kotlinx.android.synthetic.main.activity_add_accounts.*
import kotlinx.android.synthetic.main.activity_change_password.*
import kotlinx.android.synthetic.main.confirm_sheet.view.*
import org.json.JSONObject

class AccountDetails : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RealmHelper.init(this)
        setContentView(R.layout.activity_account_details)

        bankName.text = HelperVariables.selectedAccount?.bankName
        accountNumber.text = "A/c No: "+HelperVariables.selectedAccount?.accountNumber
        ifsc.text = "IFSC: "+HelperVariables.selectedAccount?.IFSC
        holderName.text = HelperVariables.selectedAccount?.holderName

        back.setOnClickListener{
            onBackPressed()
        }

        cancel.setOnClickListener {
            onBackPressed()
        }

        delete.setOnClickListener {
            showAlert()
        }

    }

    private fun showAlert(){
        val mBottomSheetDialog = AlertDialog.Builder(this)
        val sheetView: View = layoutInflater.inflate(R.layout.confirm_sheet, null)
        val done = sheetView.findViewById<TextView>(R.id.done)
        val cancel = sheetView.findViewById<TextView>(R.id.cancel)
        mBottomSheetDialog.setView(sheetView)
        sheetView.title.text = "Remove Bank"
        sheetView.subTitle.text = "Are you sure want to delete this bank account ?"
        val dialog = mBottomSheetDialog.show()

        cancel.setOnClickListener{

            onBackPressed()
        }
        done.setOnClickListener{
            mainContent.visibility=View.INVISIBLE
            deleteFromServer()
            dialog.dismiss()
        }
    }

   private fun removeAccountFromDataBase(){
       StateContext.model.bankAccounts.value?.remove(HelperVariables.selectedAccount)
       Realm.getDefaultInstance().executeTransactionAsync{
           it.where(BankAccount::class.java)
               .equalTo("accountNumber",HelperVariables.selectedAccount?.accountNumber)
               .equalTo("IFSC",HelperVariables.selectedAccount?.IFSC).findFirst()?.deleteFromRealm()
       }

       mainContent.visibility=View.VISIBLE


       AlertHelper.showAlertDialog(
           this@AccountDetails,
           "Successful !",
           "Your password has been changed successfully",
           object : AlertHelper.AlertDialogCallback {
               override fun onDismiss() {
                   mainContent.visibility = View.VISIBLE
                   onBackPressed()
               }

               override fun onDone() {
                   mainContent.visibility = View.VISIBLE
                   onBackPressed()
               }
           }
       )
   }

    private fun deleteFromServer(){

        AndroidNetworking.post(ApiContext.apiUrl+ ApiContext.registrationPort+"/deleteBankAccount")
            .addHeaders("token", DetailsContext.token)
            .addBodyParameter( object{
                var id = DetailsContext.id
                var accountNumber = HelperVariables.selectedAccount?.accountNumber
                var ifsc = HelperVariables.selectedAccount?.IFSC
                var holderName = HelperVariables.selectedAccount?.holderName
                var bankName = HelperVariables.selectedAccount?.bankName
            })
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object: JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    mainContent.visibility= View.VISIBLE
                    if(response?.getString("message")=="done"){
                        removeAccountFromDataBase()
                    }else{
                        mainContent.visibility= View.VISIBLE
                        AlertHelper.showAlertDialog(
                            this@AccountDetails,
                            "Failed !", "There is some issue with our server",
                            object : AlertHelper.AlertDialogCallback {
                                override fun onDismiss() {
                                    mainContent.visibility = View.VISIBLE
                                }

                                override fun onDone() {
                                    mainContent.visibility = View.VISIBLE
                                }

                            }
                        )
                    }
                }

                override fun onError(anError: ANError?) {
                    mainContent.visibility= View.VISIBLE
                    AlertHelper.showAlertDialog(
                        this@AccountDetails,
                        "Failed !",
                        "There is some issue with our server",
                        object : AlertHelper.AlertDialogCallback {
                            override fun onDismiss() {
                                mainContent.visibility = View.VISIBLE
                            }

                            override fun onDone() {
                                mainContent.visibility = View.VISIBLE
                            }
                        }
                    )
                }
            })

    }
}