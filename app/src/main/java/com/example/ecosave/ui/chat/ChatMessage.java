package com.example.ecosave.ui.chat;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String text;
    public boolean isUser;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isUser() { return isUser; }
}
