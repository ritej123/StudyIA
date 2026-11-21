FROM python:3.11-slim

WORKDIR /app

# OCR pour les PDF scannés (français, anglais et arabe)
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr tesseract-ocr-fra tesseract-ocr-eng tesseract-ocr-ara \
    && tesseract --list-langs | grep -Eq '(^|[[:space:]])fra([[:space:]]|$)' \
    && tesseract --list-langs | grep -Eq '(^|[[:space:]])eng([[:space:]]|$)' \
    && tesseract --list-langs | grep -Eq '(^|[[:space:]])ara([[:space:]]|$)' \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Installation des dépendances
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copie du code source
COPY . .

RUN groupadd --gid 10001 ai-service \
    && useradd --uid 10001 --gid ai-service --home-dir /nonexistent --shell /usr/sbin/nologin ai-service \
    && chown -R ai-service:ai-service /app

USER ai-service

# Port exposé
EXPOSE 8000

# Démarrage du serveur
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
