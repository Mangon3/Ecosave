package com.example.ecosave.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecosave.R;
import com.example.ecosave.ui.chat.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    public boolean isLastMessageUser() {
        if (messages.isEmpty()) return false;
        return messages.get(messages.size() - 1).isUser();
    }

    public void updateLastMessage(String newText) {
        if (!messages.isEmpty()) {
            messages.get(messages.size() - 1).setText(newText);
            notifyItemChanged(messages.size() - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_USER) ? R.layout.item_chat_user : R.layout.item_chat_ai;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        MessageViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_message);
        }
        void bind(ChatMessage message) {
            textView.setText(message.getText());
        }
    }
}
