package de.eloc.eloc_control_panel.data.helpers

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.AssociatedDeviceInfo
import java.util.concurrent.Executors

class DatabaseHelper : SQLiteOpenHelper(App.instance, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "eloc_app.sqlite"
        private const val DATABASE_VERSION = 1

    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create database to keep information about BT pairs
        val sql =
            "create table ${DataManager.TBL_ASSOCIATIONS} (${DataManager.COL_DEVICE_MAC} text primary key, ${DataManager.COL_DEVICE_NAME} text)"
        db?.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
}

object DataManager {
    const val TBL_ASSOCIATIONS = "tbl_associations"

    const val COL_ID = "col_id"
    const val COL_DEVICE_NAME = "col_device_name"
    const val COL_DEVICE_MAC = "col_device_mac"

    private val helper = DatabaseHelper()

    fun findAssociation(rawMac: String, callback: (AssociatedDeviceInfo?) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            val mac = rawMac.uppercase()
            val whereClause = "$COL_DEVICE_MAC = ?"
            val whereArgs = arrayOf(mac)
            val cursor =
                helper.readableDatabase.query(
                    TBL_ASSOCIATIONS,
                    null,
                    whereClause,
                    whereArgs,
                    null,
                    null,
                    null
                )
            var info: AssociatedDeviceInfo? = null
            cursor.use {
                if (cursor.moveToFirst()) {
                    val macIndex = cursor.getColumnIndex(COL_DEVICE_MAC)
                    val nameIndex = cursor.getColumnIndex(COL_DEVICE_NAME)
                    val deviceMac = cursor.getString(macIndex)
                    val deviceName = cursor.getString(nameIndex)
                    info = AssociatedDeviceInfo(deviceMac, deviceName, null)

                }
            }
            callback(info)
        }
    }

    fun removeAssociation(rawMac: String, callback: ((Boolean) -> Unit)? = null) {
        Executors.newSingleThreadExecutor().execute {
            val mac = rawMac.uppercase()
            val db = helper.writableDatabase
            val whereClause = "$COL_DEVICE_MAC = ?"
            val whereArgs = arrayOf(mac)
            var existingRecordCount: Int
            val cursor = db.query(TBL_ASSOCIATIONS, null, whereClause, whereArgs, null, null, null)
            cursor.use {
                existingRecordCount = cursor.count
            }

            val deletedRowCount = db.delete(TBL_ASSOCIATIONS, whereClause, whereArgs)
            val success = (deletedRowCount == existingRecordCount)
            if (callback != null) {
                callback(success)
            }
        }
    }

    fun addAssociation(name: String, rawMac: String, callback: ((Boolean) -> Unit)? = null) {
        Executors.newSingleThreadExecutor().execute {
            val mac = rawMac.uppercase()
            val db = helper.writableDatabase
            val selection = "$COL_DEVICE_MAC = ?"
            val selectionArgs = arrayOf(mac)
            val cursor =
                db.query(TBL_ASSOCIATIONS, null, selection, selectionArgs, null, null, null)
            var existingRecords: Int
            cursor.use {
                existingRecords = cursor.count
            }
            var added = false
            val data = ContentValues()
            data.put(COL_DEVICE_MAC, mac)
            data.put(COL_DEVICE_NAME, name)
            if (existingRecords > 0) {
                val updated = db.update(TBL_ASSOCIATIONS, data, selection, selectionArgs)
                if (updated >= 1) {
                    added = true
                }
            } else {
                val rowId = db.insert(TBL_ASSOCIATIONS, null, data)
                if (rowId >= 0) {
                    added = true
                }
            }
            if (callback != null) {
                callback(added)
            }
        }
    }
}