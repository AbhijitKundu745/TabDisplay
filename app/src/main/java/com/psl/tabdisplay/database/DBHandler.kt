package com.psl.tabdisplay.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DBHandler(context : Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_ASSET_MASTER_TABLE = """
        CREATE TABLE $TABLE_ASSETS (
            $C_ASSET_ID TEXT UNIQUE, 
            $C_ASSET_NAME TEXT, 
            $C_ASSET_TYPE_ID TEXT, 
            $C_ASSET_TAG_ID TEXT
        )
            """.trimIndent()
            db?.execSQL(CREATE_ASSET_MASTER_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_ASSETS")
        onCreate(db)
    }
    fun insertAssets(lst : List<AssetMaster>){
        val db = this.writableDatabase
        val sql = "INSERT OR REPLACE INTO $TABLE_ASSETS ($C_ASSET_ID, $C_ASSET_TYPE_ID, $C_ASSET_NAME, $C_ASSET_TAG_ID) VALUES (?, ?, ?, ?)"
        db.beginTransactionNonExclusive()
        val stmt = db.compileStatement(sql)
        try{
            for (product in lst) {
                stmt.bindString(1, product.assetID)
                stmt.bindString(2, product.aTypeID)
                stmt.bindString(3, product.aName)
                stmt.bindString(4, product.aTagID)
                stmt.execute()
                stmt.clearBindings()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("PRODUCTMASTEREXC", e.message.toString())
        } finally {
            db.endTransaction()
            db.close()
        }
    }
    fun deleteAssetMaster(){
        writableDatabase.use { db ->
            db.delete(TABLE_ASSETS, null, null)
        }
    }
    fun getAssetMasterCount() : Int {
        val db = this.readableDatabase
        val selectQuery = "SELECT COUNT(*) FROM $TABLE_ASSETS"
        db.rawQuery(selectQuery, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }
    fun getAssetNameByAssetTagId(tagId: String): String {
        readableDatabase.use { db ->
            db.query(
                TABLE_ASSETS,
                arrayOf(C_ASSET_NAME),
                "$C_ASSET_TAG_ID=?",
                arrayOf(tagId),
                null, null, null
            ).use { cursor ->
                return if (cursor.moveToFirst()) cursor.getString(0) else UNKNOWN_ASSET
            }
        }
    }
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PSL_TAB_DISPLAY.db"
        private const val TABLE_ASSETS = "Asset_Master_Table"

        private const val C_ASSET_ID = "AssetID"
        private const val C_ASSET_NAME = "AName"
        private const val C_ASSET_TYPE_ID = "ATypeID"
        private const val C_ASSET_TAG_ID = "ATagID"

        private const val UNKNOWN_ASSET = "Unknown"
    }
}