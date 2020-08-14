package com.DevAsh.recwallet.Home.Withdraw

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.DevAsh.recwallet.Context.HelperVariables
import com.DevAsh.recwallet.Context.StateContext
import com.DevAsh.recwallet.Helper.AlertHelper
import com.DevAsh.recwallet.Home.Transactions.PasswordPrompt
import com.DevAsh.recwallet.R
import kotlinx.android.synthetic.main.activity_amount_prompt.*

class WithdrawAmountPrompt : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_withdraw_amount_prompt)


        back.setOnClickListener{
            super.onBackPressed()
        }

        cancel.setOnClickListener{
            super.onBackPressed()
        }

        done.setOnClickListener{
            HelperVariables.withdrawAmount = amount.text.toString()
            if(HelperVariables.withdrawAmount==""){
                return@setOnClickListener
            }
            if(HelperVariables.withdrawAmount.toInt()> StateContext.currentBalance){
                AlertHelper.showError("Insufficient Balance !", this)
                return@setOnClickListener
            }

            try {
                if(HelperVariables.withdrawAmount.toInt()>0){
                    HelperVariables.needToPay = true
                    startActivity(Intent(this, WithdrawOptions::class.java))
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