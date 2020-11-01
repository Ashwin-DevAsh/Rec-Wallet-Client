package com.DevAsh.recwallet.Home.Transactions

import android.Manifest.permission.USE_FINGERPRINT
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.fingerprint.FingerprintManager
import android.os.*
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.DevAsh.recwallet.Context.*
import com.DevAsh.recwallet.Context.HelperVariables.needToPay
import com.DevAsh.recwallet.Database.Credentials
import com.DevAsh.recwallet.Database.ExtraValues
import com.DevAsh.recwallet.Database.RealmHelper
import com.DevAsh.recwallet.Helper.AlertHelper
import com.DevAsh.recwallet.Helper.PasswordHashing
import com.DevAsh.recwallet.Helper.TransactionsHelper
import com.DevAsh.recwallet.Home.Recovery.RecoveryOptions
import com.DevAsh.recwallet.Models.Contacts
import com.DevAsh.recwallet.Models.Transaction
import com.DevAsh.recwallet.R
import com.DevAsh.recwallet.Registration.Login
import com.DevAsh.recwallet.SplashScreen
import com.DevAsh.recwallet.Sync.SocketHelper
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_amount_prompt.back
import kotlinx.android.synthetic.main.activity_amount_prompt.done
import kotlinx.android.synthetic.main.activity_password_prompt.*
import org.json.JSONObject
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.text.DecimalFormat
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class PasswordPrompt : AppCompatActivity() {

    var context: Context = this
    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private val KEY_NAME = ApiContext.qrKey
    lateinit var cipher:Cipher
    lateinit var  fingerprintManager:FingerprintManager
    lateinit var keyguardManager:KeyguardManager
    var isTooManyAttempts = false
    var helper:FingerprintHelper?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_prompt)
        RealmHelper.init(context)

        val extraValues = Realm.getDefaultInstance().where(ExtraValues::class.java).findFirst()

        if(intent.getBooleanExtra("isExternalApp",false)){
            needToPay=true
            loadingScreen.visibility=View.VISIBLE
            loadFromData()
            loadToData()
            Handler().postDelayed({
                loadingScreen.visibility=View.GONE
            },1000)

        }


        if (checkLockScreen()) {
            generateKey()
            if (initCipher()) {
                var cryptoObject:FingerprintManager.CryptoObject
                cipher.let {
                    cryptoObject = FingerprintManager.CryptoObject(it)
                }
                 helper = FingerprintHelper(this,object :CallBack{
                    override fun onSuccess(){
                        if(extraValues==null || !extraValues.isEnteredPasswordOnce!!){
                            animateBell("Enter password to enable fingerprint")
                        }else{
                            startVibrate(100)
                            fingerPrint.setColorFilter(Color.parseColor("#4BB543"))
                            errorMessage.setTextColor(Color.parseColor("#4BB543"))
                            errorMessage.text = "Authentication success !"
                            Handler().postDelayed({
                                transaction()
                            },200)
                        }
                    }

                    override fun onFailed(){
                        startVibrate(200)
                        animateBell()
                    }

                    override fun onDirtyRead(){
                        startVibrate(200)
                        animateBell("Could'nt process fingerprint")
                    }

                    override fun onTooManyAttempt() {
                        isTooManyAttempts=true
                        handler.removeCallbacksAndMessages(1)
                        fingerPrint.setColorFilter(Color.GRAY)
                        errorMessage.setTextColor(Color.GRAY)
                        errorMessage.text = "Fingerprint disabled"
                    }
                })

                if (fingerprintManager != null && cryptoObject != null) {
                    helper?.startAuth(fingerprintManager, cryptoObject)
                }
            }
        }



        badge.setBackgroundColor(Color.parseColor(HelperVariables.avatarColor))
        badge.text = HelperVariables.selectedUser?.name?.substring(0,1)
        name.text = "${HelperVariables.selectedUser?.name}"
        number.text = HelperVariables.selectedUser?.number
        amount.text = HelperVariables.amount

        loadAvatar()

        back.setOnClickListener{
            super.onBackPressed()
        }

        forget.setOnClickListener{

            startActivity(Intent(this, RecoveryOptions::class.java))
        }



        done.setOnClickListener{v->
            if(PasswordHashing.decryptMsg(Credentials.credentials.password!!)==password.text.toString()){
                hideKeyboardFrom(context,v)
                Handler().postDelayed({
                    transaction()
                },500)

                if(extraValues==null || extraValues.isEnteredPasswordOnce!!){
                    Realm.getDefaultInstance().executeTransactionAsync{
                        val extraValues = ExtraValues(true)
                        it.insert(extraValues)
                    }
                }
            }else{
                println(PasswordHashing.decryptMsg(Credentials.credentials.password!!)+" actual password")
                println(password.text.toString()+" actual password")
                AlertHelper.showError("Invalid Password",this@PasswordPrompt)
            }

        }


    }

    private fun loadToData(){
        HelperVariables.amount = intent.getStringExtra("amount")
        HelperVariables.selectedUser= Contacts(
            intent.getStringExtra("name")!!,
            intent.getStringExtra("number")!!,
            intent.getStringExtra("id")!!,
            intent.getStringExtra("email")!!,
            null
        )
    }

    override fun onDestroy() {
        helper?.stopAuth()
        super.onDestroy()
    }

    override fun onPause() {
        helper?.stopAuth()
        super.onPause()
    }


    private fun loadFromData(){
        val credentials: Credentials? =  Realm.getDefaultInstance().where(Credentials::class.java).findFirst()
        try {
            if(credentials==null){
                throw Exception()
            }
            Credentials.credentials = credentials
        }catch (e:Throwable){
            if(intent.getBooleanExtra("isExternalApp",false)){
                sendResult(false)
                return
            }
        }
    }



    private fun loadAvatar(){
        UiContext.loadProfileImage(context,HelperVariables.selectedUser?.id!!,object:
            LoadProfileCallBack {
            override fun onSuccess() {
                if(!HelperVariables.selectedUser?.id!!.contains("rpay")){
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


    fun transaction(){
        if(needToPay){
            loadingScreen.visibility= View.VISIBLE
            needToPay=false

            AndroidNetworking.post(ApiContext.apiUrl + ApiContext.paymentPort + "/pay")
                .setContentType("application/json; charset=utf-8")
                .addHeaders("token", Credentials.credentials.token)
                .addApplicationJsonBody(object{
                    var from = object {
                        var id = Credentials.credentials.id
                        var name = Credentials.credentials.accountName
                        var number = Credentials.credentials.phoneNumber
                        var email = Credentials.credentials.email
                    }
                    var to = object {
                        var id =  HelperVariables.selectedUser?.id
                        var name =  HelperVariables.selectedUser?.name
                        var number =  HelperVariables.selectedUser?.number
                        var email =  HelperVariables.selectedUser?.email
                    }
                    var amount = HelperVariables.amount
                })
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject?) {
                        println(response)
                        if(response?.get("message")=="done"){
                            if(intent.getBooleanExtra("isExternalApp",false)){
                                sendResult(true)
                                return
                            }

                            TransactionsHelper.paymentObserver?.update(
                                Transaction(
                                    contacts = HelperVariables.selectedUser!!,
                                    amount = HelperVariables.amount!!,
                                    time = "Paid  "+ SplashScreen.dateToString(
                                        response.getString("transactiontime")) ,
                                    transactionId =  response.getString("transactionid"),
                                    isWithdraw =  false,isGenerated = false,
                                    type = "Send",
                                    timeStamp = response.getString("transactiontime")

                                )
                            )

                            SocketHelper.socket?.connect()
                            SocketHelper.socket?.emit("notifySingleObjectTransaction",JSONObject(
                                mapOf(
                                    "from"  to JSONObject(
                                        mapOf(
                                            "id" to Credentials.credentials.id ,
                                            "name" to Credentials.credentials.accountName,
                                            "number" to Credentials.credentials.phoneNumber,
                                            "email" to Credentials.credentials.email
                                        )
                                    ),
                                    "to" to JSONObject(
                                        mapOf(
                                            "id" to HelperVariables.selectedUser?.id ,
                                            "name" to HelperVariables.selectedUser?.name,
                                            "number" to  HelperVariables.selectedUser?.number,
                                            "email" to HelperVariables.selectedUser?.email
                                        )
                                    ),
                                    "amount" to HelperVariables.amount,
                                    "transactionID" to response.get("transactionid"),
                                    "transactionTime" to response.get("transactiontime")
                                )
                            ))
                            transactionSuccessful()
                            AndroidNetworking.get(ApiContext.apiUrl + ApiContext.profilePort + "/init/${Credentials.credentials.id}")
                                .addHeaders("token", Credentials.credentials.token)
                                .setPriority(Priority.IMMEDIATE)
                                .build()
                                .getAsJSONObject(object: JSONObjectRequestListener {
                                    override fun onResponse(response: JSONObject?) {
                                        val jsonData = JSONObject()
                                        jsonData.put("to",
                                            HelperVariables.selectedUser?.id.toString().replace("+",""))
                                        SocketHelper.socket?.emit("notifyPayment",jsonData)
                                        val balance = response?.getInt("balance")
                                        StateContext.currentBalance = balance!!
                                        val formatter = DecimalFormat("##,##,##,##,##,##,##,###")
                                        StateContext.setBalanceToModel(formatter.format(balance))
                                        val transactionObjectArray = response?.getJSONArray("transactions")
                                        StateContext.initAllTransaction(TransactionsHelper.addTransaction(transactionObjectArray))
                                    }
                                    override fun onError(anError: ANError?) {
                                        AlertHelper.showServerError(this@PasswordPrompt)
                                    }
                                })
                        }else{
                            if(intent.getBooleanExtra("isExternalApp",false)){
                                sendResult(false)
                                return
                            }
                            AlertHelper.showAlertDialog(this@PasswordPrompt,
                                "Failed !",
                                "your transaction of ${HelperVariables.amount} ${HelperVariables.currency} is failed. if any amount debited it will refund soon",
                                object: AlertHelper.AlertDialogCallback {
                                    override fun onDismiss() {
                                        loadingScreen.visibility=View.INVISIBLE
                                        onBackPressed()
                                    }

                                    override fun onDone() {
                                        loadingScreen.visibility=View.INVISIBLE
                                        onBackPressed()
                                    }
                                }
                            )
                        }
                    }

                    override fun onError(anError: ANError?) {
                        if(intent.getBooleanExtra("isExternalApp",false)){
                            sendResult(false)
                            return
                        }
                        loadingScreen.visibility= View.VISIBLE
                        AlertHelper.showAlertDialog(this@PasswordPrompt,
                            "Failed !",
                            "your transaction of ${HelperVariables.amount} ${HelperVariables.currency} is failed. if any amount debited it will refund soon",
                            object: AlertHelper.AlertDialogCallback {
                                override fun onDismiss() {
                                    loadingScreen.visibility=View.INVISIBLE
                                    onBackPressed()
                                }

                                override fun onDone() {
                                    loadingScreen.visibility=View.INVISIBLE
                                    onBackPressed()
                                }
                            }
                        )
                    }

                })
        }
    }


    fun transactionSuccessful(){
        val intent = Intent(this,Successful::class.java)
        intent.putExtra("type","transaction")
        intent.putExtra("amount",HelperVariables.amount)
        startActivity(intent)
        finish()
    }

    fun sendResult(result: Boolean){
        val resultIntent = Intent()
        resultIntent.putExtra("isTransactionSuccess",result )
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }


    override fun onBackPressed() {
        if(loadingScreen.visibility==View.GONE){
            super.onBackPressed()
        }
    }


    private fun hideKeyboardFrom(context: Context, view: View) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun checkLockScreen(): Boolean {
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE)
                as KeyguardManager
         fingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE)
                as FingerprintManager
        if (!keyguardManager.isKeyguardSecure) {
           AlertHelper.showError(
                "Lock screen security not enabled",
               this)
            return false
        }

        if (ActivityCompat.checkSelfPermission(this,
                USE_FINGERPRINT) !=
            PackageManager.PERMISSION_GRANTED) {
          AlertHelper.showError(
                "Permission not enabled (Fingerprint)",
              this)

            return false
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            AlertHelper.showError(
                "No fingerprint registered, please register",
                this)
            return false
        }
        return true
    }

    private fun generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(
                "Failed to get KeyGenerator instance", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get KeyGenerator instance", e)
        }

        try {
            keyStore.load(null)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            keyGenerator.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun initCipher(): Boolean {

        try {
            cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }

        try {
            keyStore.load(null)
            val key = keyStore.getKey(KEY_NAME, null) as SecretKey
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }
    }

    private fun startVibrate(time:Long){
        val v: Vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            //deprecated in API 26
            v.vibrate(time)
        }
    }

    val handler= Handler()

    fun animateBell(message:String="Authentication Failed") {

        val imgBell: ImageView = findViewById<View>(R.id.fingerPrint) as ImageView
        imgBell.setColorFilter(Color.RED)
        errorMessage.text=message
        errorMessage.visibility=View.VISIBLE
        errorMessage.setTextColor(Color.RED)

        val shake: Animation = AnimationUtils.loadAnimation(this, R.anim.shakeanimation)
        imgBell.animation = shake
        imgBell.startAnimation(shake)

        handler.postDelayed({
            if(!isTooManyAttempts && !message.startsWith("Enter")){
                imgBell.setColorFilter(resources.getColor(R.color.textDark))
                errorMessage.text="Touch the fingerprint sensor"
                errorMessage.setTextColor(resources.getColor(R.color.textDark))
             }
        },1500)
    }
}

class FingerprintHelper(private val appContext: Activity, private val callBack: CallBack) : FingerprintManager.AuthenticationCallback() {

    private  var cancellationSignal: CancellationSignal?=null

    fun stopAuth(){
        try {
            cancellationSignal?.cancel()
        }catch (E:Throwable){}
    }

    fun startAuth(manager: FingerprintManager,
                  cryptoObject: FingerprintManager.CryptoObject) {

        cancellationSignal = CancellationSignal()

        if (ActivityCompat.checkSelfPermission(appContext,
                USE_FINGERPRINT) !=
            PackageManager.PERMISSION_GRANTED) {
            return
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
    }

    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
        callBack.onTooManyAttempt()
    }

    override fun onAuthenticationHelp(helpMsgId: Int,helpString: CharSequence) {
        callBack.onDirtyRead()

    }

    override fun onAuthenticationFailed() {
        callBack.onFailed()
    }

    override fun onAuthenticationSucceeded(
        result: FingerprintManager.AuthenticationResult) {
        callBack.onSuccess()
    }

}

interface CallBack{
    fun onSuccess()
    fun onFailed()
    fun onDirtyRead()
    fun onTooManyAttempt()
}