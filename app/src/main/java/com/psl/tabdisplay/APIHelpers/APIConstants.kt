package com.psl.tabdisplay.APIHelpers

class APIConstants {
    companion object {
        const val M_USER_LOGIN: String = "WMS/TabLogin"
        const val M_SYNC: String = "WMS/Sync"
        const val M_LOG_OUT: String = "WMS/UserLogOut?userID="
        const val M_GET_WORK_ORDER_DETAILS: String = "WMS/GetWorkorderListItemsV1"
        const val M_POST_INVENTORY: String = "WMS/InsertTransactionDetails"

        const val API_TIMEOUT: Int = 60
        const val K_STATUS: String = "status"
        const val K_MESSAGE: String = "message"
        const val K_DATA: String = "data"

        const val K_DEVICE_ID: String = "ClientDeviceID"
        const val K_POLLING_TIMER: String = "PollingTimer"
        const val K_WORK_ORDER_DETAILS_ARRAY: String = "OrderDetails"
        const val K_WORK_ORDER_NUMBER: String = "WorkorderNumber"
        const val K_WORK_ORDER_TYPE: String = "WorkorderType"
        const val K_PALLET_NUMBER: String = "PalletName"
        const val K_PALLET_TAG_ID: String = "PalletTagID"
        const val K_LAST_UPDATED_DATE_TIME: String = "LastUpdatedDateTime"
        const val K_LIST_ITEM_STATUS: String = "ListItemStatus"
        const val K_LOCATION_NAME: String = "LocationName"
        const val K_BIN_LOCATION: String = "BinLocation"
        const val K_LOADING_AREA: String = "LoadingAreaName"
        const val K_TEMP_STORAGE: String = "TemporaryStorageName"
    }
}