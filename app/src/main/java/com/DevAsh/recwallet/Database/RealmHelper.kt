package com.DevAsh.recwallet.Database

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration

object RealmHelper {
    fun init(context:Context){
        Realm.init(context)

        val mConfiguration = RealmConfiguration.Builder()
            .name("RealmData.realm")
            .schemaVersion(9)
            .migration(Migrations())
            .build()
        Realm.setDefaultConfiguration(mConfiguration)
    }


}