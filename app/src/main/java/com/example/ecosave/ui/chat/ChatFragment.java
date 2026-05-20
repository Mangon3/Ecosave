package com.example.ecosave.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecosave.R;
import com.example.ecosave.ui.chat.adapter.ChatAdapter;

public class ChatFragment extends Fragment {
    
    private AiBuddyViewModel viewModel;
    private ChatAdapter adapter;
    private EditText inputField;
    private ImageButton sendButton;
    private ImageButton stopButton;
    private RecyclerView recycler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        viewModel = new ViewModelProvider(requireActivity()).get(AiBuddyViewModel.class);
        adapter = new ChatAdapter();
        
        recycler = view.findViewById(R.id.recycler_chat);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);
        
        inputField = view.findViewById(R.id.edit_chat_input);
        sendButton = view.findViewById(R.id.btn_send);
        stopButton = view.findViewById(R.id.btn_stop);
        ImageButton resetButton = view.findViewById(R.id.btn_reset);

        // Load chat history from DB
        viewModel.getHistory().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null && !messages.isEmpty()) {
                adapter.setMessages(messages);
                recycler.scrollToPosition(adapter.getItemCount() - 1);
            }
        });

        // Listen for reset events
        viewModel.getChatReset().observe(getViewLifecycleOwner(), reset -> {
            if (Boolean.TRUE.equals(reset)) {
                adapter.clearMessages();
            }
        });

        // Single persistent response observer - always updates the last AI bubble
        viewModel.getResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null && !response.isEmpty()) {
                // If it's a new message, we add it, otherwise we update the last one
                if (adapter.getItemCount() == 0 || adapter.isLastMessageUser()) {
                    adapter.addMessage(new ChatMessage(response, false));
                } else {
                    adapter.updateLastMessage(response);
                }
                recycler.scrollToPosition(adapter.getItemCount() - 1);
            }
        });

        // Single persistent loading observer - toggles send/stop buttons
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                sendButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                inputField.setEnabled(false);
            } else {
                stopButton.setVisibility(View.GONE);
                sendButton.setVisibility(View.VISIBLE);
                inputField.setEnabled(true);
            }
        });

        resetButton.setOnClickListener(v -> {
            viewModel.resetChat();
        });

        sendButton.setOnClickListener(v -> {
            String text = inputField.getText().toString().trim();
            if (!text.isEmpty()) {
                adapter.addMessage(new ChatMessage(text, true));
                recycler.scrollToPosition(adapter.getItemCount() - 1);
                
                inputField.setText("");
                adapter.addMessage(new ChatMessage("Thinking...", false));
                recycler.scrollToPosition(adapter.getItemCount() - 1);
                
                viewModel.askQuestion(text);
            }
        });

        stopButton.setOnClickListener(v -> {
            viewModel.stopGeneration();
        });

        return view;
    }
}
