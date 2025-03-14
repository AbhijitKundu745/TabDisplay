package com.psl.tabdisplay.models

import com.google.gson.JsonObject
import com.psl.tabdisplay.database.AssetMaster
import java.util.Objects

class dataModels {
    data class LoginResponse(
        val PairedDeviceID: String,
        val Topic: List<TopicsList>
    )

    data class TopicsList(
        var Title: String,
        var TopicName: String
    )
    data class InventoryRequest(
        val TransID : String,
        val ClientDeviceID : String,
        val AntennaID : Int,
        val RSSI : Int,
        val TransDatetime : String,
        val TouchPointType : String,
        val Count : String,
        val PalletTagID : String,
        val ListItemStatus : String,
        val WorkorderNumber : String,
        val WorkorderType : String,
        val CategoryID : String,
        val SubTagDetails : List<SubTagData>
    )
    data class SubTagData(
        val TagID : String,
        val Count : Int,
        val RSSI : String,
        val CategoryID : Int,
        val TagType : String,
        val TransDatetime : String
    )
    data class Assets(
        val assestsList : List<AssetMaster>
    )
}