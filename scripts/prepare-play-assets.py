"""Generate branding graphics, never fabricated UI screenshots. Requires Pillow."""
from pathlib import Path
import argparse
import hashlib
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "docs" / "google-play" / "assets"


def font(size: int, bold: bool = False):
    windows = Path("C:/Windows/Fonts") / ("segoeuib.ttf" if bold else "segoeui.ttf")
    if windows.exists():
        return ImageFont.truetype(str(windows), size)
    return ImageFont.truetype("DejaVuSans.ttf", size)


def build_branding():
    DEST.mkdir(parents=True, exist_ok=True)
    source = ROOT / "app/src/main/res/drawable/librecare_icon_source.png"
    with Image.open(source) as image:
        icon = image.convert("RGBA").resize((512, 512), Image.Resampling.LANCZOS)
    background = Image.new("RGB", (512, 512), "#0B1220")
    background.paste(icon, (0, 0), icon)
    background.save(DEST / "LibreCare-icon-512.png", optimize=True)
    graphic = Image.new("RGB", (1024, 500), "#0B1220")
    graphic.paste(background.resize((280, 280), Image.Resampling.LANCZOS), (54, 110))
    draw = ImageDraw.Draw(graphic)
    draw.text((380, 115), "LibreCare", font=font(68, True), fill="#5EEAD4")
    draw.text((383, 215), "Twoja historia glikemii", font=font(34), fill="#F1F5F9")
    draw.text((383, 285), "Import LibreView CSV", font=font(27), fill="#CBD5E1")
    draw.text((383, 330), "Interaktywna historia", font=font(27), fill="#CBD5E1")
    graphic.save(DEST / "LibreCare-feature-1024x500.png", optimize=True)


def manifest():
    entries = []
    for file in sorted(DEST.glob("*.png")):
        with Image.open(file) as image:
            width, height = image.size
            if "icon-512" in file.name:
                assert (width, height) == (512, 512)
            elif "feature-1024x500" in file.name:
                assert (width, height) == (1024, 500)
            elif file.name.startswith("screen-"):
                assert min(width, height) >= 320 and max(width, height) <= 3840
                assert max(width, height) <= 2 * min(width, height), "Capture on a suitable emulator display; do not fabricate screenshots"
            entries.append({"file": file.name, "width": width, "height": height,
                            "bytes": file.stat().st_size,
                            "sha256": hashlib.sha256(file.read_bytes()).hexdigest(),
                            "kind": "emulator-screenshot" if file.name.startswith("screen-") else "branding-graphic"})
    (DEST / "manifest.json").write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(entries, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest-only", action="store_true")
    args = parser.parse_args()
    if not args.manifest_only:
        build_branding()
    manifest()


