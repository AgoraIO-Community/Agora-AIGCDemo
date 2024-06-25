package io.agora.aigc_cloud_demo.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.agora.aigc_cloud_demo.R
import io.agora.aigc_cloud_demo.model.HistoryModel
import io.agora.aigc_cloud_demo.ui.adapter.HistoryListAdapter.MyViewHolder

class HistoryListAdapter(private val mContext: Context, private val dataList: List<HistoryModel>?) :
    RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_history_list, parent, false)
        return MyViewHolder(view)
    }

    fun getDataList(): List<HistoryModel>? {
        return dataList
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        if (null != dataList) {
            holder.layoutView.setBackgroundColor(
                if (position % 2 == 0) ContextCompat.getColor(
                    mContext,
                    R.color.history_bg_color
                ) else Color.WHITE
            )
            val aiHistoryModel = dataList[position]
            holder.message.text = String.format(
                "[%s]%s%s",
                aiHistoryModel.date,
                aiHistoryModel.title,
                aiHistoryModel.message
            )
        }
    }

    override fun getItemCount(): Int {
        if (null == dataList) {
            return 0
        }
        return dataList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val layoutView: View = itemView.findViewById(R.id.item_layout)
        val message: TextView = itemView.findViewById<View>(R.id.tv_message) as TextView
    }


    class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.left = 0
            outRect.right = 0
            outRect.bottom = space

            // Add top margin only for the first item to avoid double space between items
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = space
            }
        }
    }
}
