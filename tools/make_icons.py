"""Renders ANTEVAULT's icon set from the game's own spade geometry and fonts.

    python tools/make_icons.py

Writes the desktop window icons, Android launcher icons, the web favicon and the social
preview image. Needs Pillow. The shape mirrors Art.suitField() in core, so the icon and the
cards always agree.
"""
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
BG_TOP, BG_BOTTOM = (58, 20, 36), (20, 9, 24)
GOLD, GOLD_HI = (255, 201, 92), (255, 233, 170)
BIG = 1024


def spade_mask(size, scale):
    """Spade silhouette as an 'L' mask, centred, occupying `scale` of the canvas."""
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    k = size * scale / 2.16          # shape space is roughly [-1.08, 1.08]
    cx, cy = size / 2, size / 2 + size * scale * 0.02

    def px(x, y):
        return cx + x * k, cy - y * k

    for sx in (-0.42, 0.42):                                   # lobes
        x, y = px(sx, -0.16)
        d.ellipse([x - 0.47 * k, y - 0.47 * k, x + 0.47 * k, y + 0.47 * k], fill=255)
    d.polygon([px(-0.88, -0.16), px(0, 1.0), px(0.88, -0.16)], fill=255)   # tip

    top, steps = 0.06, 40                                      # tapered stem
    left, right = [], []
    for i in range(steps + 1):
        y = top - (top + 0.96) * i / steps
        t = max(0.0, min(1.0, (top - y) / 1.02))
        w = 0.055 + 0.40 * t ** 3
        left.append(px(-w, y))
        right.append(px(w, y))
    d.polygon(left + right[::-1], fill=255)
    return m


def gradient(size, top, bottom):
    g = Image.new("RGB", (1, size))
    for y in range(size):
        t = y / (size - 1)
        g.putpixel((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return g.resize((size, size))


def rounded(size, radius_frac):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * radius_frac), fill=255)
    return m


def icon(size=BIG, round_corners=True, spade_scale=0.56):
    base = gradient(size, BG_TOP, BG_BOTTOM).convert("RGBA")

    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    halo = spade_mask(size, spade_scale * 1.06).filter(ImageFilter.GaussianBlur(size * 0.045))
    glow.paste(Image.new("RGBA", (size, size), GOLD + (150,)), mask=halo)
    base = Image.alpha_composite(base, glow)

    mask = spade_mask(size, spade_scale)
    fill = gradient(size, GOLD_HI, GOLD).convert("RGBA")
    base.paste(fill, mask=mask)

    ring = Image.new("L", (size, size), 0)
    inset = int(size * 0.055)
    ImageDraw.Draw(ring).rounded_rectangle([inset, inset, size - inset, size - inset],
                                           radius=int(size * 0.15), outline=255, width=max(2, size // 90))
    base.paste(Image.new("RGBA", (size, size), GOLD + (95,)), mask=ring)

    if round_corners:
        base.putalpha(ImageChops.multiply(base.getchannel("A"), rounded(size, 0.21)))
    return base


def save(img, rel, size):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(path)
    print(f"  {rel}  {size}px")


def social(rel):
    w, h = 1200, 630
    img = gradient(max(w, h), BG_TOP, BG_BOTTOM).resize((w, h)).convert("RGBA")
    mark = icon(BIG, round_corners=True).resize((420, 420), Image.LANCZOS)
    img.alpha_composite(mark, (90, (h - 420) // 2))
    d = ImageDraw.Draw(img)
    title_path = str(ROOT / "tools/fonts/title.ttf")
    x0, room = 560, w - 560 - 60
    size = 110
    while size > 40 and d.textlength("ANTEVAULT", font=ImageFont.truetype(title_path, size)) > room:
        size -= 2                                   # fit the title to the space, never clip it
    title = ImageFont.truetype(title_path, size)
    body = ImageFont.truetype(str(ROOT / "tools/fonts/body.ttf"), 34)
    d.text((x0, 210), "ANTEVAULT", font=title, fill=GOLD)
    d.text((x0 + 4, 210 + size + 18), "a poker roguelite", font=body, fill=(179, 154, 164))
    d.text((x0 + 4, 210 + size + 66), "break the seals, open the vault", font=body, fill=(179, 154, 164))
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(path, optimize=True)
    print(f"  {rel}  {w}x{h}")


if __name__ == "__main__":
    master = icon()
    print("desktop")
    for s in (16, 32, 64, 128):
        save(master, f"lwjgl3/src/main/resources/icon{s}.png", s)

    print("android")
    for folder, s in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        save(master, f"android/src/main/res/mipmap-{folder}/ic_launcher.png", s)

    print("web")
    save(master, "html/webapp/favicon.png", 192)
    save(master, "html/webapp/apple-touch-icon.png", 180)
    social("html/webapp/social.png")
    save(master, "docs/icon.png", 256)
