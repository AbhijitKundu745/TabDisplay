package com.psl.tabdisplay.models

data class OrderDetails(
    var palletNumber: String? = null,
    var palletTagID: String? = null,
     var currentPalletName: String? = null,
     var currentPalletTagID: String? = null,
     var lastUpdatedDateTime: String? = null,
    var pickupLocation: String? = null,
     var pickupLocationTagID: String? = null,
     var binLocation: String? = null,
     var binLocationTagID: String? = null,
    var listItemStatus: String? = null,
    var workorderType: String? = null,
    var workorderNo: String? = null,
     var ordertype: String? = null,
     var serialNo: String? = null,
     var loadingAreaName: String? = null,
     var tempStorage: String? = null
)