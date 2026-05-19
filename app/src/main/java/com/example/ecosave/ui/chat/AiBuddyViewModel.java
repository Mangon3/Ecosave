package com.example.ecosave.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewModelScope;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiBuddyViewModel extends ViewModel {
    private final MutableLiveData<String> _response = new MutableLiveData<>();

    public LiveData<String> getResponse() {
        return _response;
    }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsLoading() {
        return _isLoading;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void askQuestion(String prompt) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            try {
                Python py = Python.getInstance();
                PyObject pyModule = py.getModule("llm_engine");
                PyObject result = pyModule.callAttr("get_response", prompt,
                        "You are a helpful and educational financial assistant for beginners.");
                _response.postValue(result.toString());
            } catch (Exception e) {
                _response.postValue("Error: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }
}
