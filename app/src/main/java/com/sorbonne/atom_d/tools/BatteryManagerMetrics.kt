package com.sorbonne.atom_d.tools

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import kotlin.math.abs

class BatteryManagerMetrics(private val context: Context) {
    private val TAG = BatteryManagerMetrics::class.simpleName

    fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getAverageBatteryCurrent(): Int{
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
    }

    fun getBatteryCurrentNow(): Int{
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    }

    fun getRemainingBatteryCapacity(): Long{
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    }

    fun getEstimatedBatteryLifetime(): Double?{
        val batteryCurrent = getAverageBatteryCurrent()
        val batteryCapacity = getRemainingBatteryCapacity()

        return if(batteryCurrent < 0 && batteryCapacity >= abs(batteryCurrent)){
            batteryCapacity.toDouble()/abs(batteryCurrent)
        } else if(batteryCurrent >= 0){
            batteryCapacity.toDouble()
        } else {
            Log.e(TAG, "batteryCurrent: $batteryCurrent - batteryCapacity: $batteryCapacity")
            null
        }
    }
}