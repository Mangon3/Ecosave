# Project Ecosave: Changes and Deviations from Project Proposal

This document outlines the architectural and technical deviations made during the implementation of the Ecosave mobile application from the original Project Proposal PDF. These changes were necessary to ensure application stability, performance, and resource compatibility with standard Android devices and emulators.

---

## 1. AI Model Selection
* **Proposed in Plan**: Local Llama 2 (7B parameter model, typically 3.9 GB+ for Q4 quantizations).
* **Actual Implementation**: TinyLlama 1.1B Chat (`tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf`, ~638 MB).
* **Rationale for Deviation**: Llama 2 7B is too resource-intensive for standard Android emulators and mobile devices, requiring over 5 GB of RAM for inference and consistently leading to Out-Of-Memory (OOM) crashes. TinyLlama 1.1B runs stable under 850 MB of RAM, preventing crashes while still providing responsive and accurate financial advice.

---

## 2. Inference Engine and Integration Stack
* **Proposed in Plan**: Python integration via the Chaquopy plugin using `llama-cpp-python`.
* **Actual Implementation**: Native C++ `llama.cpp` compilation accessed directly via JNI / Kotlin wrappers (`LlamaHelper`).
* **Rationale for Deviation**: Relying on Chaquopy and a Python runtime on Android introduces significant overhead, slower loading times, and higher memory usage. Compiling the inference engine natively via C++ JNI bindings allows direct memory mapping (mmap) of the GGUF model, enabling faster processing and lower memory footprints.

---

## 3. ASX 200 Index API Strategy
* **Proposed in Plan**: Real-time S&P/ASX 200 Index API integration.
* **Actual Implementation**: Fetching NYSE dual-listed stock data for top ASX companies (BHP Group, Rio Tinto, Woodside Energy) via the Finnhub API.
* **Rationale for Deviation**: Finnhub's free tier does not support direct ASX index queries (such as `^AXJO`), which are restricted to premium subscriptions. By fetching dual-listed top constituents on the NYSE, the app provides real-time indicators of ASX market trends without requiring a paid API key.

---

## 4. UI/UX and Stability Features
Several features were introduced during the implementation phase that were not detailed in the original proposal but were necessary for a polished user experience:
* **Stop Generation Button**: Added a pause/stop button in the Chat UI that replaces the send button during generation. This lets the user stop run-on text generation instantly, releasing thread resources and saving battery.
* **Response Word/Character Limits**: A strict 120-word restriction was implemented via prompt engineering and parameter limits (`MAX_TOKENS = 160`) to keep the conversational buddy concise and within memory limits. Additionally, a strict 200-character input limit was enforced on the user's chat input interface to prevent token overflow.
* **Prompt Configuration File**: Created a dedicated, centralized configuration class `PromptConfig.kt` to make changes to the prompt template easily manageable.
* **Live Dashboard Synchronization**: Integrated Room Database checks in `onResume()` of the DashboardFragment so that current balance card updates automatically when the user adds/modifies entries in the Budget tab.
* **Persistent Chat History & Reset (Room DB)**: Migrated `ChatMessage` to a Room Entity under `AppDatabase` (version 2 with safe schema migration) to save user and AI chats locally. Added a "Reset" feature to wipe the chat history and trigger a fresh budget-informed greeting.
* **Parallel Stock Loading**: Refactored stock price retrieval in `fetchMarketData()` to run parallel network queries via coroutine `async`/`awaitAll`, preventing blocking UI delays when the chat launches.

