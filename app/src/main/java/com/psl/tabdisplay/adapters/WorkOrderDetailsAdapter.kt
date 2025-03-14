package com.psl.tabdisplay.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.psl.tabdisplay.R
import com.psl.tabdisplay.models.OrderDetails

class WorkOrderDetailsAdapter(
    private val context: Context,
    private val orderDetailList: List<OrderDetails>,
    private val orderType: String
) : RecyclerView.Adapter<WorkOrderDetailsAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemSerialNo: TextView = itemView.findViewById(R.id.itemSerialNo)
        val itemPalletNo: TextView = itemView.findViewById(R.id.itemPalletNo)
        val itemPickup: TextView = itemView.findViewById(R.id.itemPickup)
        val itemWOType: TextView = itemView.findViewById(R.id.itemWOtype)
        val itemStatus: TextView = itemView.findViewById(R.id.itemStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_loading_unloading, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val order = orderDetailList[position]
        val pos = position + 1

        holder.itemSerialNo.text = pos.toString()
        holder.itemPalletNo.text = order.palletNumber
        holder.itemPalletNo.isSelected = true
        holder.itemPickup.text = order.pickupLocation
        holder.itemPickup.isSelected = true
        order.pickupLocation?.let { Log.e("Pickup Location", it) }
        holder.itemWOType.text = order.workorderType
        holder.itemWOType.isSelected = true
        holder.itemStatus.text = order.listItemStatus
        holder.itemStatus.isSelected = true

        if (order.listItemStatus == "Completed") {
            holder.itemStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
        }

        when (orderType) {
            "U0", "U1", "L0", "L1", "I0" -> {
                holder.itemSerialNo.visibility = View.VISIBLE
                holder.itemPalletNo.visibility = View.VISIBLE
                holder.itemPickup.visibility = View.VISIBLE
                holder.itemWOType.visibility = View.VISIBLE
                holder.itemStatus.visibility = View.VISIBLE
            }
        }

        // Row color alternation
        if (order.workorderType.equals("I0")) {
            holder.itemSerialNo.setTextColor(context.resources.getColor(R.color.white))
            holder.itemPalletNo.setTextColor(context.resources.getColor(R.color.white))
            holder.itemPickup.setTextColor(context.resources.getColor(R.color.white))
            holder.itemWOType.setTextColor(context.resources.getColor(R.color.white))
            holder.itemStatus.setTextColor(context.resources.getColor(R.color.white))
            holder.itemView.setBackgroundColor(context.resources.getColor(R.color.orange))
        } else if (position % 2 == 0) {
            holder.itemSerialNo.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemPalletNo.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemPickup.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemWOType.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.cyan1))
        } else {
            holder.itemSerialNo.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemPalletNo.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemPickup.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemWOType.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.lemonyellow))
        }
    }

    override fun getItemCount(): Int = orderDetailList.size

    override fun getItemViewType(position: Int): Int = 1
}
