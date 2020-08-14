package com.DevAsh.recwallet.Home.Transactions

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.DevAsh.recwallet.Context.StateContext
import com.DevAsh.recwallet.Context.HelperVariables
import com.DevAsh.recwallet.Helper.AlertHelper
import com.DevAsh.recwallet.R
import kotlinx.android.synthetic.main.activity_amount_prompt.*
import kotlinx.android.synthetic.main.activity_amount_prompt.amount
import kotlinx.android.synthetic.main.activity_amount_prompt.back
import kotlinx.android.synthetic.main.activity_amount_prompt.cancel
import kotlinx.android.synthetic.main.activity_amount_prompt.done

class AmountPrompt : AppCompatActivity() {

    lateinit var context: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amount_prompt)

        context=this

        paymentText.text = "Paying ${HelperVariables.selectedUser?.name}"
        paymentBrief.text = "Your transaction to ${HelperVariables.selectedUser?.number} will be verify by our server"



        back.setOnClickListener{
            super.onBackPressed()
        }

        cancel.setOnClickListener{
            super.onBackPressed()
        }

        done.setOnClickListener{v->

            HelperVariables.amount = amount.text.toString()
            if(HelperVariables.amount==""){
                return@setOnClickListener
            }
            if(HelperVariables.amount!!.toInt()>StateContext.currentBalance){
                AlertHelper.showError("Insufficient Balance !", this)
                return@setOnClickListener
            }

            try {
               if(HelperVariables.amount!!.toInt()>0){
                   HelperVariables.needToPay = true

                   startActivity(Intent(context,PasswordPrompt::class.java))
                    finish()
                }else{
                   AlertHelper.showError("Invalid Amount", this)
                }
            }catch (e:Throwable){
                AlertHelper.showError("Invalid Amount", this)
            }

        }
    }
}
