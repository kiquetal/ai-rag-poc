# Use the official llama.cpp server image as a base
FROM ghcr.io/ggerganov/llama.cpp:server

# Switch to root user to install necessary tools
USER root

# Install wget for downloading the model
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create a directory for the models
RUN mkdir -p /models && chown -R 1001:1001 /models

# Switch back to the default non-root user
USER 1001
WORKDIR /models

# This command will run when the container starts.
# 1. It checks if the model file exists in the /models volume.
# 2. If it does NOT exist, it downloads the model.
# 3. It then starts the llama.cpp server, pointing to the model file.
CMD ["/bin/sh", "-c", "if [ ! -f /models/granite-3.3-8b-instruct.Q5_K_M.gguf ]; then echo '--- Model not found, downloading... ---'; wget -O /models/granite-3.3-8b-instruct.Q5_K_M.gguf https://huggingface.co/ibm-granite/granite-3B-8B-instruct-GGUF/resolve/main/granite-3.3-8b-instruct.Q5_K_M.gguf; echo '--- Download complete. ---'; fi && echo '--- Starting server... ---' && /usr/local/bin/server -m /models/granite-3.3-8b-instruct.Q5_K_M.gguf --host 0.0.0.0 --port 8080 -c 4096"]
