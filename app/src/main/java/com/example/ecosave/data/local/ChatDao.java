package com.example.ecosave.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.ecosave.ui.chat.ChatMessage;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insert(ChatMessage message);

    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    List<ChatMessage> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void deleteAllMessages();
}
