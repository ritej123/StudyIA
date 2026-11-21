import base64
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient
from PIL import Image
import fitz

import main


class AiServiceTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(main.app)

    @staticmethod
    def blank_pdf():
        pdf = fitz.open()
        try:
            pdf.new_page(width=100, height=100)
            return pdf.tobytes()
        finally:
            pdf.close()

    @staticmethod
    def text_pdf(text):
        pdf = fitz.open()
        try:
            page = pdf.new_page(width=612, height=792)
            page.insert_text((72, 72), text)
            return pdf.tobytes()
        finally:
            pdf.close()

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

    def test_extract_uploaded_utf8_text(self):
        response = self.client.post(
            "/extract-file",
            files={"file": ("cours.txt", "La cellule est l'unité du vivant.".encode("utf-8"), "text/plain")},
        )

        self.assertEqual(response.status_code, 200)
        self.assertGreater(response.json()["chars"], 0)
        self.assertEqual(response.json()["text"], "La cellule est l'unité du vivant.")

    def test_extract_endpoint_accepts_multipart_text(self):
        response = self.client.post(
            "/extract",
            files={"file": ("cours.txt", "Texte transmis en multipart.".encode("utf-8"), "text/plain")},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["text"], "Texte transmis en multipart.")
        self.assertEqual(response.json()["chars"], len("Texte transmis en multipart."))

    def test_rejects_invalid_base64(self):
        response = self.client.post(
            "/extract",
            json={"filename": "cours.txt", "content_base64": "%%%"},
        )

        self.assertEqual(response.status_code, 400)

    def test_rejects_pdf_without_extractable_text(self):
        output = self.blank_pdf()
        with patch.object(main.pdf_text.pytesseract, "image_to_string", return_value=""):
            response = self.client.post(
                "/extract",
                json={
                    "filename": "cours.pdf",
                    "content_base64": base64.b64encode(output).decode(),
                },
            )

        self.assertEqual(response.status_code, 422)

    def test_extracts_text_from_scanned_pdf_with_ocr(self):
        output = self.blank_pdf()

        with patch.object(
            main.pdf_text.pytesseract,
            "image_to_string",
            return_value="Texte détecté par OCR avec suffisamment de caractères pour valider l'extraction.",
        ) as ocr:
            response = self.client.post(
                "/extract",
                json={
                    "filename": "cours-scan.pdf",
                    "content_base64": base64.b64encode(output).decode(),
                },
            )

        self.assertEqual(response.status_code, 200)
        self.assertIn("Texte détecté par OCR", response.json()["text"])
        ocr.assert_called_once()
        self.assertEqual(ocr.call_args.kwargs["lang"], "fra+eng+ara")

    def test_retries_ocr_with_language_specific_modes_when_initial_pass_finds_no_text(self):
        image = Image.new("L", (10, 10))
        try:
            with patch.object(
                main.pdf_text.pytesseract,
                "image_to_string",
                side_effect=["", "", "Texte français reconnu"],
            ) as ocr:
                text = main.pdf_text.extract_image_text(image)
        finally:
            image.close()

        self.assertEqual(text, "Texte français reconnu")
        self.assertEqual(ocr.call_count, 3)
        self.assertEqual(ocr.call_args_list[1].kwargs["lang"], "eng")
        self.assertEqual(ocr.call_args_list[2].kwargs["lang"], "fra")

    def test_rejects_malformed_pdf(self):
        with patch.object(main.pdf_text.fitz, "open", side_effect=fitz.FileDataError("malformed")):
            response = self.client.post(
                "/extract",
                json={
                    "filename": "secours.pdf",
                    "content_base64": base64.b64encode(b"%PDF-1.4 malformed").decode(),
                },
            )

        self.assertEqual(response.status_code, 422)

    def test_rejects_scanned_pdf_over_ocr_page_limit(self):
        output = self.blank_pdf()

        with patch.object(main.pdf_text, "MAX_PAGES", 0):
            response = self.client.post(
                "/extract",
                json={
                    "filename": "cours-scan.pdf",
                    "content_base64": base64.b64encode(output).decode(),
                },
            )

        self.assertEqual(response.status_code, 413)

    def test_extracts_text_from_pdf(self):
        pdf = self.text_pdf(
            "Biologie cellulaire : les cellules constituent les unités fondamentales du vivant."
        )

        response = self.client.post(
            "/extract",
            json={
                "filename": "cours.pdf",
                "content_base64": base64.b64encode(pdf).decode(),
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
