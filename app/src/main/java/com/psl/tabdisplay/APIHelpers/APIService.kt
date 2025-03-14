package com.psl.tabdisplay.APIHelpers

import com.google.gson.JsonObject
import com.psl.tabdisplay.database.AssetMaster
import com.psl.tabdisplay.models.dataModels
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST(APIConstants.M_USER_LOGIN)
    fun userLogin(@Body request: LoginRequest): Call<APIResponse<dataModels.LoginResponse>>

    @POST(APIConstants.M_SYNC)
    fun syncAssets(@Body request: RequestBody): Call<APIResponse<dataModels.Assets>>

    @POST(APIConstants.M_GET_WORK_ORDER_DETAILS)
    fun getWorkorder(@Body request: RequestBody): Call<JsonObject>

    @POST(APIConstants.M_POST_INVENTORY)
    fun postInventory(@Body request: dataModels.InventoryRequest): Call<JsonObject>
}