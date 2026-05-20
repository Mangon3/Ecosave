# Ecosave - Offline Financial AI Buddy

Ecosave is a privacy-first personal budgeting and financial assistant Android application. It utilizes a local SQLite database for expense tracking, Retrofit for real-time market trends (Finnhub API), and a **fully native local TinyLlama 1.1B LLM engine** (running offline on-device) to answer budgeting queries without compromising user privacy.

---

## Technical Architecture Overview
- **UI Architecture:** MVVM (Model-View-ViewModel) with native Fragments, premium dark theme, and a Bottom Navigation bar.
- **Local Database:** Room persistence layer storing budget/expense records and persisting AI chat history offline (with safe v1->v2 migrations).
- **Networking:** Retrofit REST client integrating the Finnhub API for real-time stock market data (BHP, RIO, WDS).
- **On-Device LLM:** Powered by native C++ `llama.cpp` JNI bindings (`llamacpp-kotlin`) loading a 4-bit quantized GGUF TinyLlama model directly from the emulator's local storage.

---

## Prerequisites
1. **Android Studio** (Koala or newer).
2. **Android SDK Platform 36** (API 36).
3. **Android NDK & CMake** installed via the SDK Manager.
4. **Finnhub API Key** (Get a free one from [finnhub.io](https://finnhub.io)).

---

## 1. Initial Project Setup

### Environment Variables
Create a `.env` file in the root directory of the project (this file is gitignored to secure API keys) and add your Finnhub key:
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
Because the raw weights are too large for standard mobile devices, we use a 4-bit quantized GGUF format version of **TinyLlama 1.1B Chat** (~638 MB), which operates efficiently on mobile resources.

Run the following command to download the optimized model directly into the project's models directory:
```bash
wget -O local_models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

---

## 3. Running the App

### Option A: Using the Terminal (Recommended)

#### 1. Setup the Android Emulator Storage
Ensure your running emulator has at least **2GB - 4GB of internal storage space** configured in its AVD settings.
Start the emulator:
```bash
# Start your AVD
emulator -avd Your_AVD_Name -wipe-data
```

#### 2. Push the TinyLlama Model to the Emulator
The model must reside on the emulator's private external files storage directory, and permissions must be set to allow the app to read it:
```bash
adb shell mkdir -p /sdcard/Android/data/com.example.ecosave/files/
adb push local_models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
# Change permissions so the app UID can read the pushed file
adb shell chmod 666 /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

#### 3. Build & Launch the Application
```bash
./gradlew installDebug
adb shell am start -n com.example.ecosave/com.example.ecosave.MainActivity
```

---

### Option B: Using Android Studio (GUI)

#### 1. Configure Emulator Storage
1. Open the **Device Manager** (`Tools -> Device Manager`).
2. Click the Pencil icon (Edit) next to your emulator.
3. Click **Show Advanced Settings**.
4. Scroll down to **Internal Storage** and set it to **4096 MB** (4GB) or higher.
5. Click **Finish**.
6. Wipe existing data by clicking the three dots (`⋮`) next to the device and selecting **Wipe Data**.

#### 2. Push the Model
Launch the emulator first, then use the terminal to push the model file to the app's private storage path and make it readable:
```bash
adb shell mkdir -p /sdcard/Android/data/com.example.ecosave/files/
adb push local_models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
# Change permissions so the app UID can read the pushed file
adb shell chmod 666 /sdcard/Android/data/com.example.ecosave/files/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf
```

#### 3. Build and Run
1. Open the project folder in Android Studio.
2. Allow Gradle sync to complete.
3. Click the green **Run** button (or press `Shift + F10`) on the toolbar.
4. Select your active emulator to install and launch.
