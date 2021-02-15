package com.DevAsh.recwallet.Helper

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.DevAsh.recwallet.R
import com.nispok.snackbar.Snackbar
import com.nispok.snackbar.SnackbarManager
import kotlinx.android.synthetic.main.password_alert_sheet.view.*


object AlertHelper {
    fun showError(text: String, context: Activity){
        SnackbarManager.show(
            Snackbar.with(context) // context
                .text(text) // text to be displayed
                .textTypeface(Typeface.DEFAULT_BOLD)
                .duration(2000)
                .textColor(Color.WHITE) // change the text color
                .color(Color.parseColor("#b71c1c")) // change the background color
            , context
        )

    }

    fun showToast(text: String, context: Activity){
        SnackbarManager.show(
            Snackbar.with(context) // context
                .text(text) // text to be displayed
                .textTypeface(Typeface.DEFAULT_BOLD)
                .duration(2000)
                .textColor(Color.WHITE) // change the text color
                .color(Color.parseColor("#4caf50")) // change the background color
            , context
        )

    }

    fun showAlertDialog(
        context:Activity,
        title:String,
        subTitle:String,
        alertDialogCallback: AlertDialogCallback? =null
    ){
        val mBottomSheetDialog = AlertDialog.Builder(context)
        val sheetView: View = LayoutInflater.from(context).inflate(R.layout.password_alert_sheet, null)
        sheetView.title.text = title
        sheetView.subTitle.text = subTitle
        val done = sheetView.findViewById<TextView>(R.id.done)
        mBottomSheetDialog.setView(sheetView)
        val dialog = mBottomSheetDialog.show()
        done.setOnClickListener{
            dialog.dismiss()
            alertDialogCallback?.onDone()
        }

        dialog.setOnDismissListener{
           alertDialogCallback?.onDismiss()
        }
    }

    fun showNativeAlertDialog(
        context:Activity,
        title:String,
        subTitle:String,
        alertDialogCallback: AlertDialogCallback? =null
    ){
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context,R.style.AlertDialogCustom)
        alertDialogBuilder.setMessage(subTitle)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setCancelable(false)

        alertDialogBuilder.setPositiveButton(
           "OK"
        ) { dialog, _ ->
            dialog.cancel()
            alertDialogCallback?.onDismiss()
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()


//        val mBottomSheetDialog = AlertDialog.Builder(context)
//        val sheetView: View = LayoutInflater.from(context).inflate(R.layout.password_alert_sheet, null)
//        sheetView.title.text = title
//        sheetView.subTitle.text = subTitle
//        val done = sheetView.findViewById<TextView>(R.id.done)
//        mBottomSheetDialog.setView(sheetView)
//        val dialog = mBottomSheetDialog.show()
//        done.setOnClickListener{
//            dialog.dismiss()
//            alertDialogCallback?.onDone()
//        }
//
//        dialog.setOnDismissListener{
//            alertDialogCallback?.onDismiss()
//        }
    }




    interface AlertDialogCallback{
        fun onDismiss()
        fun onDone()
    }

    fun showServerError(context: Activity){
        showAlertDialog(
            context,
            "Server Error !", "unable to reach server kindly check your internet connection or try again",
            object :AlertDialogCallback {
                override fun onDismiss() {
                   context.finish()
                }

                override fun onDone() {
                  context.finish()
                }

            }
        )
    }

}

