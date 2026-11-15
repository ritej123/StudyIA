import base64
import io
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient
from pypdf import PdfWriter

import main


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
        output = io.BytesIO()
        writer = PdfWriter()
        writer.add_blank_page(width=100, height=100)
        writer.write(output)
        response = self.client.post(
            "/extract",
            json={
                "filename": "cours.pdf",
                "content_base64": base64.b64encode(output.getvalue()).decode(),
            },
        )

        self.assertEqual(response.status_code, 422)

    def test_extracts_text_from_pdf(self):
        content = b"BT /F1 12 Tf 72 720 Td (Biologie cellulaire) Tj ET"
        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
            b"/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            b"<< /Length " + str(len(content)).encode() + b" >>\nstream\n"
            + content + b"\nendstream",
        ]
        pdf = bytearray(b"%PDF-1.4\n")
        offsets = []
        for index, body in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{index} 0 obj\n".encode())
            pdf.extend(body + b"\nendobj\n")
        xref_offset = len(pdf)
        pdf.extend(b"xref\n0 6\n0000000000 65535 f \n")
        for offset in offsets:
            pdf.extend(f"{offset:010} 00000 n \n".encode())
        pdf.extend(
            b"trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n"
            + str(xref_offset).encode()
            + b"\n%%EOF\n"
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
