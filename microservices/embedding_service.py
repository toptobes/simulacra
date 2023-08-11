import os

from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModel
import torch
import torch.nn.functional as f

app = Flask(__name__)

device = os.getenv("ASTRA_DEMO_EMBEDDING_SERVICE_DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
device = torch.device(device)
print(f"Using device: {device}")

# all-MiniLM-L6-v2
tokenizer = AutoTokenizer.from_pretrained('sentence-transformers/all-mpnet-base-v2')
model = AutoModel.from_pretrained('sentence-transformers/all-mpnet-base-v2').to(device)

def mean_pooling(model_output, attention_mask):
    token_embeddings = model_output[0]
    input_mask_expanded = attention_mask.unsqueeze(-1).expand(token_embeddings.size()).float()
    return torch.sum(token_embeddings * input_mask_expanded, 1) / torch.clamp(input_mask_expanded.sum(1), min=1e-9)

@app.route('/embed', methods=['POST'])
def embed():
    sentences = request.json['sentences']

    encoded_input = tokenizer(sentences, padding=True, truncation=True, return_tensors='pt')

    encoded_input = { key: tensor.to(device) for key, tensor in encoded_input.items() }

    with torch.no_grad():
        model_output = model(**encoded_input)

    sentence_embeddings = mean_pooling(model_output, encoded_input['attention_mask'])
    sentence_embeddings = f.normalize(sentence_embeddings)

    return jsonify(sentence_embeddings.tolist())

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
