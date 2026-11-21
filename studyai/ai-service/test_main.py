import base64
import io
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient
import fitz

import main


class FakeGeminiResponse:
    def __init__(self, status_code, body):
        self.status_code = status_code
        self.body = body
        self.is_error = status_code >= 400

    def json(self):
        return self.body


class FakeGeminiClient:
    def __init__(self, responses):
        self.responses = iter(responses)
        self.urls = []

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_value, traceback):
        return False

    async def post(self, url, **kwargs):
        self.urls.append(url)
        return next(self.responses)


class AiServiceTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(main.app)

    def test_extracts_utf8_text(self):
        response = self.client.post(
            "/extract",
            json={
                "filename": "cours.txt",
                "content_base64": base64.b64encode("La cellule est l'unité du vivant.".encode()).decode(),
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["text"], "La cellule est l'unité du vivant.")

    def test_rejects_invalid_base64(self):
        response = self.client.post(
            "/extract",
            json={"filename": "cours.txt", "content_base64": "%%%"},
        )

        self.assertEqual(response.status_code, 400)

    def test_rejects_pdf_without_extractable_text(self):
        pdf = fitz.open()
        pdf.new_page(width=100, height=100)
        output = pdf.tobytes()
        pdf.close()
        response = self.client.post(
            "/extract",
            json={
                "filename": "cours.pdf",
                "content_base64": base64.b64encode(output).decode(),
            },
        )

        self.assertEqual(response.status_code, 422)

    def test_extracts_text_from_pdf(self):
        pdf = fitz.open()
        page = pdf.new_page(width=612, height=792)
        page.insert_text((72, 72),
                         "Biologie cellulaire : les cellules constituent les unités fondamentales du vivant.")
        content = pdf.tobytes()
        pdf.close()

        response = self.client.post(
            "/extract",
            json={
                "filename": "cours.pdf",
                "content_base64": base64.b64encode(content).decode(),
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertIn("Biologie cellulaire", response.json()["text"])

    def test_reports_missing_gemini_configuration(self):
        with patch.object(main, "GEMINI_API_KEY", ""):
            response = self.client.post(
                "/generate",
                json={"operation": "summary", "context": "Le cours.", "prompt": ""},
            )

        self.assertEqual(response.status_code, 503)

    def test_falls_back_to_lite_model_when_primary_is_overloaded(self):
        gemini_client = FakeGeminiClient([
            FakeGeminiResponse(503, {"error": {"message": "Model overloaded"}}),
            FakeGeminiResponse(200, {
                "candidates": [{"content": {"parts": [{"text": "Résumé de test"}]}}],
            }),
        ])
        with (
            patch.object(main, "GEMINI_API_KEY", "test-key"),
            patch.object(main, "GEMINI_MODEL", "gemini-flash-latest"),
            patch.object(main, "GEMINI_URL", "https://example.test/primary"),
            patch.object(main, "GEMINI_FALLBACK_MODEL", "gemini-flash-lite-latest"),
            patch("main.httpx.AsyncClient", return_value=gemini_client),
        ):
            response = self.client.post(
                "/generate",
                json={"operation": "summary", "context": "Le cours.", "prompt": ""},
            )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["result"], "Résumé de test")
        self.assertEqual(gemini_client.urls, [
            "https://example.test/primary",
            "https://generativelanguage.googleapis.com/v1beta/models/"
            "gemini-flash-lite-latest:generateContent",
        ])

    def test_returns_service_unavailable_when_fallback_is_overloaded(self):
        gemini_client = FakeGeminiClient([
            FakeGeminiResponse(503, {"error": {"message": "Primary overloaded"}}),
            FakeGeminiResponse(503, {"error": {"message": "Fallback overloaded"}}),
        ])
        with (
            patch.object(main, "GEMINI_API_KEY", "test-key"),
            patch.object(main, "GEMINI_MODEL", "gemini-flash-latest"),
            patch.object(main, "GEMINI_URL", "https://example.test/primary"),
            patch.object(main, "GEMINI_FALLBACK_MODEL", "gemini-flash-lite-latest"),
            patch("main.httpx.AsyncClient", return_value=gemini_client),
        ):
            response = self.client.post(
                "/generate",
                json={"operation": "summary", "context": "Le cours.", "prompt": ""},
            )

        self.assertEqual(response.status_code, 503)
