package io.agora.aiteachingassistant.ui.adapter

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.agora.aiteachingassistant.R
import io.agora.aiteachingassistant.constants.Constants
import io.agora.aiteachingassistant.model.ChatMessage
import io.agora.aiteachingassistant.utils.Utils

class ChatMessageAdapter(private val chatMessages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listener: ChatMessageAdapter.OnItemClickListener? = null
    private val LEFT_MESSAGE = 0
    private val RIGHT_MESSAGE = 1

    fun setOnItemClickListener(listener: ChatMessageAdapter.OnItemClickListener) {
        this.listener = listener
    }

    inner class LeftMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: ConstraintLayout = itemView.findViewById(R.id.item_layout)
        val messageLayout: ConstraintLayout = itemView.findViewById(R.id.message_layout)
        val messageTextView: TextView = itemView.findViewById(R.id.message_tv)
        val translateTextView: TextView = itemView.findViewById(R.id.translate_tv)
        val tipBtn: TextView = itemView.findViewById(R.id.tip_btn)
        val translateBtn: TextView = itemView.findViewById(R.id.translate_btn)
    }

    inner class RightMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: ConstraintLayout = itemView.findViewById(R.id.item_layout)
        val messageLayout: ConstraintLayout = itemView.findViewById(R.id.message_layout)
        val messageTextView: TextView = itemView.findViewById(R.id.message_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == LEFT_MESSAGE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_left, parent, false)
            LeftMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_right, parent, false)
            RightMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = chatMessages[position]

        if (holder.itemViewType == LEFT_MESSAGE) {
            val leftMessageHolder = holder as LeftMessageViewHolder
            leftMessageHolder.messageTextView.text = Utils.removeChineseQuotes(chatMessage.content)
            if (chatMessage.translateContent.isNotEmpty()) {
                leftMessageHolder.translateTextView.visibility = View.VISIBLE
                leftMessageHolder.translateTextView.text = chatMessage.translateContent
            } else {
                leftMessageHolder.translateTextView.visibility = View.GONE
            }
            leftMessageHolder.messageLayout.post {
                val layoutParams =
                    leftMessageHolder.messageLayout.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.matchConstraintMaxWidth =
                    leftMessageHolder.itemLayout.width - leftMessageHolder.tipBtn.width - Utils.toDp(
                        10
                    )
                leftMessageHolder.messageLayout.layoutParams = layoutParams
            }

            leftMessageHolder.tipBtn.setOnClickListener {
                listener?.onTipClick(chatMessage)
            }

            leftMessageHolder.translateBtn.setOnClickListener {
                listener?.onTranslateClick(chatMessage)
            }
        } else {
            val rightMessageHolder = holder as RightMessageViewHolder
            rightMessageHolder.messageTextView.text = Utils.removeChineseQuotes(chatMessage.content)

            rightMessageHolder.messageLayout.post {
                val layoutParams =
                    rightMessageHolder.messageLayout.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.matchConstraintMaxWidth =
                    rightMessageHolder.itemLayout.width - Utils.toDp(
                        10
                    )
                rightMessageHolder.messageLayout.layoutParams = layoutParams
            }
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].role == Constants.LLM_ROLE_ASSISTANT) LEFT_MESSAGE else RIGHT_MESSAGE
    }

    class ItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

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
        fun onTipClick(chatMessage: ChatMessage)
        fun onTranslateClick(chatMessage: ChatMessage)
    }
}