package com.DevAsh.recwallet.Home.Transactions

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.DevAsh.recwallet.Context.HelperVariables
import com.DevAsh.recwallet.Home.HomePage
import com.DevAsh.recwallet.R
import kotlinx.android.synthetic.main.activity_successfull.*
import kotlinx.android.synthetic.main.confirm_sheet.done


class Successful : AppCompatActivity() {
    lateinit var type:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_successfull)

        type = intent.getStringExtra("type")!!
        val amount = intent.getStringExtra("amount")
        when (type) {
            "addMoney" -> {
                messageEditText.text = "The amount $amount ${HelperVariables.currency}s has been successfully added in your wallet"
            }
            "withdraw" -> {
                image.setImageDrawable(getDrawable(R.drawable.withdraw_successfull))
                messageEditText.text = "The amount $amount ${HelperVariables.currency}s has been successfully transfered to your bank"
            }
            else -> {
                image.setImageDrawable(getDrawable(R.drawable.transaction_successful))
                messageEditText.text = "Your transaction of $amount ${HelperVariables.currency}s has been successfully completed"
            }
        }

        val ring: MediaPlayer = MediaPlayer.create(this, R.raw.success)
        ring.start()

        done.setOnClickListener{
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if(type=="addMoney" || type=="withdraw"){
            startActivity(Intent(this,HomePage::class.java))
            finish()
        }else{
            super.onBackPressed()
        }

    }
}