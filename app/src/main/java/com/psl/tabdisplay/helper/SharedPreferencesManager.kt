package com.psl.tabdisplay.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class SharedPreferencesManager (context: Context) {

    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor: SharedPreferences.Editor = sp.edit()

    private val SHARED_PREF_NAME: String = "PALLET_TRACKING_SHARED_PREF"

    private val IS_HOST_CONFIG: String = "IS_HOST_CONFIG"
    private val HOST_URL: String = "HOST_URL"
    private val ASSET_MASTER_LAST_SYNC_DATE: String = "ASSET_MASTER_LAST_SYNC_DATE"


    private val IS_LOGIN_SAVED: String = "IS_LOGIN_SAVED"
    private val LOGIN_USER: String = "LOGIN_USER"
    private val LOGIN_USER_ID: String = "LOGIN_USER_ID"
    private val LOGIN_CUSTOMER_ID: String = "LOGIN_CUSTOMER_ID"
    private val LOGIN_PASSWORD: String = "LOGIN_PASSWORD"
    private val COMPANY_CODE: String = "COMPANY_CODE"
    private val COMPANY_ID: String = "COMPANY_ID"
    private val CURRENT_ACCESS_PASSWORD: String = "CURRENT_ACCESS_PASSWORD"
    private val DEVICE_ID: String = "DEVICE_ID"
    private val SAVED_POWER: String = "POWER"
    private val POLLING_TIMER: String = "POLLING_TIMER"
    private val PAIRED_DEVICE_ID = "PAIRED_DEVICE_ID"

    fun getIsLoginSaved(): Boolean {
        return sp.getBoolean(IS_LOGIN_SAVED, false)
    }

    fun getPower(): Int {
        return sp.getInt(SAVED_POWER, 30)
    }
    
    fun getPollingTimer(): Int {
        return sp.getInt(POLLING_TIMER, 5000)
    }

    fun getHostUrl(): String? {
        return sp.getString(HOST_URL, "http://192.168.0.172/WMS31/")
    }
    
    fun getIsHostConfig(): Boolean {
        return sp.getBoolean(IS_HOST_CONFIG, false)
    }
    
    fun getCurrentAccessPassword(): String? {
        return sp.getString(CURRENT_ACCESS_PASSWORD, "00000000")
    }

    fun getDeviceId(): String? {
        return sp.getString(DEVICE_ID, "")
    }

    fun getSavedUser(): String? {
        return sp.getString(LOGIN_USER, "")
    }

    fun getSavedUserId(): String? {
        return sp.getString(LOGIN_USER_ID, "")
    }
    
    fun getSavedPassword(): String? {
        return sp.getString(LOGIN_PASSWORD, "")
    }

    fun getCompanyCode(): String? {
        return sp.getString(COMPANY_CODE, "15")
    }

    fun getCompanyId(): String? {
        return sp.getString(COMPANY_ID, "")
    }

    fun getCustomerId(): String? {
        return sp.getString(LOGIN_CUSTOMER_ID, "")
    }

    fun getAssetMasterLastSyncDate(): String? {
        return sp.getString(ASSET_MASTER_LAST_SYNC_DATE, "01-01-1970")
    }

    fun getPairedDeviceID(): String? {
        return sp.getString(PAIRED_DEVICE_ID, "123ABC")
    }

    fun setPower( newValue: Int): SharedPreferencesManager {
        val editor = sp.edit()
        editor.putInt(SAVED_POWER, newValue).apply()
        return this
    }
    
    fun setPollingTimer(newValue: Int): SharedPreferencesManager {
        editor.putInt(POLLING_TIMER, newValue).apply()
        return this
    }

    fun setHostUrl(newValue: String?): SharedPreferencesManager {
        editor.putString(HOST_URL, newValue).apply()
        return this
    }
    
    fun setIsHostConfig(newValue: Boolean): SharedPreferencesManager {
        editor.putBoolean(IS_HOST_CONFIG, newValue).apply()
        return this
    }

    fun setIsLoginSaved(newValue: Boolean): SharedPreferencesManager {
        editor.putBoolean(IS_LOGIN_SAVED, newValue).apply()
        return this
    }
    
    fun setCurrentAccessPassword(newValue: String?): SharedPreferencesManager {
        editor.putString(CURRENT_ACCESS_PASSWORD, newValue).apply()
        return this
    }
    
    fun setDeviceId(newValue: String?): SharedPreferencesManager {
        editor.putString(DEVICE_ID, newValue).apply()
        return this
    }

   

    fun setSavedUser(newValue: String?): SharedPreferencesManager {
        editor.putString(LOGIN_USER, newValue).apply()
        return this
    }

    fun setSavedUserId(newValue: String?): SharedPreferencesManager {
        editor.putString(LOGIN_USER_ID, newValue).apply()
        return this
    }

   

    fun setSavedPassword(newValue: String?): SharedPreferencesManager {
        editor.putString(LOGIN_PASSWORD, newValue).apply()
        return this
    }

    fun setCompanyCode(newValue: String?): SharedPreferencesManager {
        editor.putString(COMPANY_CODE, newValue).apply()
        return this
    }

    fun setCompanyId(newValue: String?): SharedPreferencesManager {
        editor.putString(COMPANY_ID, newValue).apply()
        return this
    }

    fun setCustomerId(newValue: String?): SharedPreferencesManager {
        editor.putString(LOGIN_CUSTOMER_ID, newValue).apply()
        return this
    }



    fun setAssetMasterLastSyncDate(newValue: String?): SharedPreferencesManager {
        editor.putString(ASSET_MASTER_LAST_SYNC_DATE, newValue).apply()
        return this
    }
    fun setPairedDeviceID(newValue: String): SharedPreferencesManager {
        editor.putString(PAIRED_DEVICE_ID, newValue).apply()
        return this
    }
}