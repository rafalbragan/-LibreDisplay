from pathlib import Path
from PIL import Image, ImageDraw


def ensure_square(image: Image.Image) -> Image.Image:
    w, h = image.size
    if w == h:
        return image
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return image.crop((left, top, left + side, top + side))


def save_resized(image: Image.Image, output: Path, size: int) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(output, format="PNG")


def create_notification_icon(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    icon = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icon)
    draw.ellipse((3, 3, 21, 21), outline=(255, 255, 255, 255), width=3)
    draw.ellipse((8, 8, 16, 16), fill=(255, 255, 255, 255))
    icon.save(path, format="PNG")


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    src = root / "app" / "src" / "main" / "res" / "drawable" / "librecare_icon_source.png"
    if not src.exists():
        raise FileNotFoundError(f"Icon source not found: {src}")

    img = Image.open(src).convert("RGBA")
    img = ensure_square(img)

    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }

    for density, size in densities.items():
        folder = root / "app" / "src" / "main" / "res" / f"mipmap-{density}"
        save_resized(img, folder / "ic_launcher.png", size)
        save_resized(img, folder / "ic_launcher_round.png", size)

    # Foreground PNG for adaptive icon resources.
    save_resized(
        img,
        root / "app" / "src" / "main" / "res" / "drawable" / "ic_launcher_foreground_librecare.png",
        432,
    )

    save_resized(img, root / "play-store" / "icon-512.png", 512)

    create_notification_icon(
        root / "app" / "src" / "main" / "res" / "drawable" / "ic_stat_librecare.png"
    )

    print(f"Generated launcher and Play icons from: {src}")


if __name__ == "__main__":
    main()


