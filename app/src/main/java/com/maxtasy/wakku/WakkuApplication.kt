package com.maxtasy.wakku

import android.app.Application
import com.maxtasy.wakku.data.WakkuDatabase

class WakkuApplication : Application() {
    val database: WakkuDatabase by lazy { WakkuDatabase.getInstance(this) }
}
