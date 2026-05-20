# Ecosave - Offline Financial AI Buddy

Ecosave is a personal budgeting and financial assistant Android app. It uses a local SQLite database for expense tracking, Retrofit for real-time market trends (Finnhub API), and a fully native local TinyLlama 1.1B LLM engine to answer budgeting queries.

---

## Technical Architecture Overview
- **UI Architecture:** Model-View-ViewModel with native fragments, dark theme, bottom nav bar.
- **Local Database:** Room persistence layer storing budget/expense records & persisting AI chat history offline.
- **Networking:** Retrofit REST client with Finnhub API for real-time stock market data (BHP, RIO, WDS).
- **On-Device LLM:** Native C++ `llama.cpp` JNI bindings (`llamacpp-kotlin`) loading a 4-bit quantized GGUF TinyLlama model from the emulator's local storage.

---

## Prerequisites
1. **Android Studio** (Koala or newer)
2. **Android SDK Platform 36** (API 36)
3. **Android NDK & CMake** installed via the SDK Manager
4. **Finnhub API Key** (get a free one from [finnhub.io](https://finnhub.io))

---

## 1. Initial Project Setup

### Environment Variables
Create a `.env` file in the root directory of the project based on .env.example and add your Finnhub key:
```env
FINNHUB_API_KEY=your_api_key_here
```

### Setup Local Model Directories
Create a folder named `local_models` in the project root directory:
```bash
mkdir -p local_models
```

---

## 2. Acquiring the AI Model
Because the raw weights are too large for standard mobile devices, we use a 4-bit quantized GGUF format version of TinyLlama 1.1B Chat (~638 MB).

Run the following command to download the optimized model directly into the project's models directory:
```bash
wget -O local_models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

---

## 3. Running the App

### Option A: Using the Terminal (recommended)

#### 1. Setup the Android emulator storage
Ensure your running emulator has at least 2GB - 4GB of internal storage space configured in its AVD settings.
Start the emulator:
```bash
emulator -avd Your_AVD_Name -wipe-data
```

#### 2. Push the TinyLlama model to the emulator
The model must reside on the emulator's private external files storage directory, and permissions must be set to allow the app to read it:
```bash
adb shell mkdir -p /sdcard/Android/data/com.example.ecosave/files/
adb push local_models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
adb shell chmod 666 /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

#### 3. Build & launch the app
```bash
./gradlew installDebug
adb shell am start -n com.example.ecosave/com.example.ecosave.MainActivity
```

---

### Option B: Using Android Studio (GUI)

#### 1. Configure emulator RAM to 4GB or higher

#### 2. Push the model
Launch the emulator first, then use the terminal to push the model:
```bash
adb shell mkdir -p /sdcard/Android/data/com.example.ecosave/files/
adb push local_models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
# Change permissions so the app UID can read the pushed file
adb shell chmod 666 /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

#### 3. Build and Run
1. Open the project folder in Android Studio
3. Click Run
4. Select your active emulator to install and launch
