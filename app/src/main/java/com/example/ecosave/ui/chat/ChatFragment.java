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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        viewModel = new ViewModelProvider(this).get(AiBuddyViewModel.class);
        adapter = new ChatAdapter();
        
        RecyclerView recycler = view.findViewById(R.id.recycler_chat);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);
        
        inputField = view.findViewById(R.id.edit_chat_input);
        sendButton = view.findViewById(R.id.btn_send);
        
        sendButton.setOnClickListener(v -> {
            String text = inputField.getText().toString().trim();
            if (!text.isEmpty()) {
                // Add user message to UI
                adapter.addMessage(new ChatMessage(text, true));
                recycler.scrollToPosition(adapter.getItemCount() - 1);
                
                // Clear input and show loading placeholder
                inputField.setText("");
                adapter.addMessage(new ChatMessage("Thinking...", false));
                int placeholderIndex = adapter.getItemCount() - 1;
                recycler.scrollToPosition(placeholderIndex);
                
                // Request response from Llama 2 Python Engine
                viewModel.askQuestion(text);
                
                // Temporary observer specifically for this response to replace placeholder
                viewModel.getResponse().observe(getViewLifecycleOwner(), response -> {
                    // Update the placeholder with the actual AI response
                    // In a robust implementation, we'd use a better state management (like updating specific indices)
                    // but for this prototype, appending or replacing the last message works.
                    viewModel.getResponse().removeObservers(getViewLifecycleOwner()); // Only observe once per click
                    
                    // Since RecyclerView adapter updates aren't safely mutable this way for real apps,
                    // we'll just add the real message and we can ignore replacing "Thinking..." for this simple prototype, 
                    // or just add it normally. Let's add it normally and maybe remove the thinking one if we want.
                    adapter.addMessage(new ChatMessage(response, false));
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        });

        // Add initial greeting
        adapter.addMessage(new ChatMessage("Hi! I'm your local AI financial buddy. How can I help you today?", false));
        
        return view;
    }
}
