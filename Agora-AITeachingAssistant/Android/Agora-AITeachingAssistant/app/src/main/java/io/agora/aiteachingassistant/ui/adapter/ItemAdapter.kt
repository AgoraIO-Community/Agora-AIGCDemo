package io.agora.aiteachingassistant.ui.adapter

import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.agora.aiteachingassistant.R

class ItemAdapter(private val itemList: Array<String>) :
    RecyclerView.Adapter<ItemAdapter.MyViewHolder>() {
    private var selectedItem = -1
    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = itemList[position]
        holder.content.text = item
        holder.itemView.isSelected = position == selectedItem
        if (position == selectedItem) {
            holder.layout.setBackgroundColor(Color.GRAY)
        } else {
            holder.layout.setBackgroundColor(Color.WHITE)
        }
        holder.itemView.setOnClickListener {
            selectedItem = position
            notifyDataSetChanged()   // 更新列表
            listener?.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val content: TextView = itemView.findViewById(R.id.content_tv)
        val layout = itemView.findViewById<View>(R.id.item_layout)
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