import io

import fitz
import pytesseract
from PIL import Image, ImageOps

MIN_CHARS = 50
LANGS = "fra+eng+ara"
MAX_PAGES = 30


def preprocess(img: Image.Image) -> Image.Image:
    return ImageOps.autocontrast(ImageOps.grayscale(img))


def extract_text(pdf_bytes: bytes) -> str:
    try:
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    except Exception as exc:
        raise ValueError(f"PDF corrompu ou illisible : {exc}") from exc

    try:
        if doc.needs_pass and not doc.authenticate(""):
            raise ValueError("PDF protégé par mot de passe")

        direct = "\n".join(page.get_text() for page in doc).strip()
        if len(direct) >= MIN_CHARS:
            return direct

        if len(doc) > MAX_PAGES:
            raise ValueError(f"L'OCR est limité à {MAX_PAGES} pages scannées par document.")

        parts = []
        for page in doc:
            pix = page.get_pixmap(dpi=300, alpha=False)
            with Image.open(io.BytesIO(pix.tobytes("png"))) as img:
                text = pytesseract.image_to_string(
                    preprocess(img),
                    lang=LANGS,
                    config="--oem 1 --psm 3",
                )
                parts.append(text)

        text = "\n".join(parts).strip()
        if len(text) < MIN_CHARS:
            raise ValueError(
                "Aucun texte détecté : scan trop flou, page vide, "
                "ou langue OCR non installée."
            )
        return text
    finally:
        doc.close()
