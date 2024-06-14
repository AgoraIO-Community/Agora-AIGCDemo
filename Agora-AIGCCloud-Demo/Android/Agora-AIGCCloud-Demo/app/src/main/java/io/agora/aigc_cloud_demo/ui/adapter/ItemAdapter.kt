package io.agora.aigc_cloud_demo.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import io.agora.aigc_cloud_demo.R
import io.agora.aigc_cloud_demo.model.ItemData

class ItemAdapter(private val itemList: MutableList<ItemData>) :
    RecyclerView.Adapter<ItemAdapter.MyViewHolder>() {
    private var selectedItem = -1
    private var listener: OnItemClickListener? = null
    private var isClickable = false

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setClickable(clickable: Boolean) {
        isClickable = clickable
    }

    fun updateDataChanged() {
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(
        holder: MyViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val item = itemList[position]
        holder.content.text = item.content
        holder.itemView.isSelected = item.bgResId != 0


        var resId = R.color.white
        if (item.bgResId != 0) {
            resId = item.bgResId
        }
        holder.itemLayout.setBackgroundColor(
            holder.itemView.resources.getColor(
                resId,
                null
            )
        )

        holder.itemView.setOnClickListener {
            if (isClickable) {
                selectedItem = position
                notifyDataSetChanged()   // 更新列表
                listener?.onItemClick(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: View = itemView.findViewById(R.id.item_layout)
        val content: TextView = itemView.findViewById(R.id.content_tv)
    }

    class MyItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = 0
            outRect.right = 0
            outRect.bottom = 0
            outRect.top = space

            // 如果是第一个item，设置top间隔
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = 0
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}