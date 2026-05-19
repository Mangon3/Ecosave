import os
import time
import logging
from llama_cpp import Llama
import tiktoken

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger("LlamaEngine")

class LlamaEngine:
    def __init__(self):
        self.model_path = "/sdcard/Download/llama-2-7b-chat.gguf"
        self.llm = None
        self.tokenizer = tiktoken.get_encoding("cl100k_base")
        logger.info("Initializing LlamaEngine...")

        if not os.path.exists(self.model_path):
            logger.error(f"Model file not found at {self.model_path}")
        else:
            try:
                # load model w' context size 2048 & 4 threads
                self.llm = Llama(model_path=self.model_path, n_ctx=2048, n_threads=4)
                logger.info("Llama model loaded successfully.")
            except Exception as e:
                logger.error(f"Failed to load model: {str(e)}")

    def generate_response(self, prompt: str, system_prompt: str = "") -> str:
        if self.llm is None:
            return "Error: Llama 2 model is not loaded. Please ensure llama-2-7b-chat.gguf is in your device's Download folder."

        # format prompt
        formatted_prompt = f"[INST] <<SYS>>\n{system_prompt}\n<</SYS>>\n\n{prompt} [/INST]"
        
        # tokenizer
        input_tokens = len(self.tokenizer.encode(formatted_prompt))
        logger.info(f"Generating response for prompt with {input_tokens} tokens...")
        
        start_time = time.time()
        
        try:
            output = self.llm(
                formatted_prompt,
                max_tokens=256,
                stop=["[/INST]"],
                echo=False
            )
            
            elapsed_time = time.time() - start_time
            response_text = output['choices'][0]['text'].strip()
            
            output_tokens = len(self.tokenizer.encode(response_text))
            tokens_per_second = output_tokens / elapsed_time if elapsed_time > 0 else 0
            
            logger.info(f"Generation complete: {output_tokens} tokens in {elapsed_time:.2f}s ({tokens_per_second:.2f} t/s)")
            
            return response_text
        except Exception as e:
            logger.error(f"Inference error: {str(e)}")
            return f"Error generating response: {str(e)}"

engine_instance = None

def get_response(prompt, system_prompt="You are a helpful financial assistant."):
    global engine_instance
    if engine_instance is None:
        engine_instance = LlamaEngine()
    
    return engine_instance.generate_response(prompt, system_prompt)
