#!/bin/bash

echo "🤖 AI Chatbot - Quick Setup for Gemini"
echo "======================================="
echo ""

# Check if Gemini credentials are set
if [ -n "$GEMINI_API_KEY" ] && [ -n "$GEMINI_PROJECT_ID" ]; then
    echo "✅ Gemini credentials are already set!"
    echo "   API Key: ${GEMINI_API_KEY:0:20}..."
    echo "   Project ID: $GEMINI_PROJECT_ID"
    echo ""
    echo "Starting Quarkus with Gemini..."
    ./mvnw quarkus:dev
else
    echo "❌ Gemini credentials NOT set"
    echo ""
    echo "To use Gemini (FAST - 2-5 second responses):"
    echo ""
    echo "1. Visit: https://console.cloud.google.com/"
    echo "2. Create a project or select existing one"
    echo "3. Enable 'Vertex AI API'"
    echo "4. Create API credentials"
    echo ""
    echo "Then run these commands:"
    echo ""
    echo "  export GEMINI_API_KEY=\"your-api-key-here\""
    echo "  export GEMINI_PROJECT_ID=\"your-project-id\""
    echo "  ./mvnw quarkus:dev"
    echo ""
    echo "OR add to ~/.bashrc for permanent setup:"
    echo ""
    echo "  echo 'export GEMINI_API_KEY=\"your-key\"' >> ~/.bashrc"
    echo "  echo 'export GEMINI_PROJECT_ID=\"your-project\"' >> ~/.bashrc"
    echo "  source ~/.bashrc"
    echo ""
    echo "=========================================="
    echo ""
    echo "Alternative: Use a smaller local model (slower but free)"
    echo ""
    echo "1. Download TinyLlama (fast, lower quality):"
    echo "   cd models/"
    echo "   wget https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
    echo ""
    echo "2. Edit docker-compose.yml to use TinyLlama"
    echo ""
    echo "3. Edit application.properties:"
    echo "   Change: quarkus.langchain4j.chat-model.provider=openai"
    echo ""
    echo "4. Restart: docker compose restart granite"
    echo ""
    echo "See FINAL-SOLUTION.md for detailed instructions"
    echo ""

    # Prompt for interactive setup
    read -p "Do you want to enter Gemini credentials now? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -p "Enter GEMINI_API_KEY: " api_key
        read -p "Enter GEMINI_PROJECT_ID: " project_id

        export GEMINI_API_KEY="$api_key"
        export GEMINI_PROJECT_ID="$project_id"

        echo ""
        echo "✅ Credentials set for this session!"
        echo "⚠️  Note: These will be lost when you close the terminal"
        echo "   To make permanent, add to ~/.bashrc (see instructions above)"
        echo ""
        echo "Starting Quarkus with Gemini..."
        ./mvnw quarkus:dev
    else
        echo ""
        echo "Setup cancelled. See FINAL-SOLUTION.md for more options."
    fi
fi

