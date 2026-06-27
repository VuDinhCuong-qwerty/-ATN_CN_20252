import os, sys
from PIL import Image

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Hinhve")
MAX_W = 2400  # chieu rong toi da (px) - du net khi in A4

total_before = 0
total_after = 0
changed = 0
rows = []

for root, _, files in os.walk(SRC):
    for fn in files:
        if not fn.lower().endswith((".png", ".jpg", ".jpeg")):
            continue
        path = os.path.join(root, fn)
        before = os.path.getsize(path)
        total_before += before
        try:
            im = Image.open(path)
            w, h = im.size
        except Exception as e:
            print(f"SKIP (cannot open) {fn}: {e}")
            total_after += before
            continue

        if w > MAX_W:
            new_h = round(h * MAX_W / w)
            im2 = im.resize((MAX_W, new_h), Image.LANCZOS)
            # giu nguyen che do mau; PNG optimize
            ext = os.path.splitext(fn)[1].lower()
            if ext in (".jpg", ".jpeg"):
                if im2.mode in ("RGBA", "P"):
                    im2 = im2.convert("RGB")
                im2.save(path, quality=85, optimize=True)
            else:
                im2.save(path, optimize=True)
            after = os.path.getsize(path)
            changed += 1
            rows.append((fn, f"{w}x{h}", f"{MAX_W}x{new_h}", before/1e6, after/1e6))
        else:
            after = before
        total_after += after
        im.close()

rows.sort(key=lambda r: r[3], reverse=True)
print(f"{'FILE':38} {'BEFORE px':14} {'AFTER px':12} {'MB->':>8} {'MB':>8}")
for fn, b, a, mb_b, mb_a in rows:
    print(f"{fn:38} {b:14} {a:12} {mb_b:8.2f} {mb_a:8.2f}")
print("-"*90)
print(f"Files resized: {changed}")
print(f"TOTAL Hinhve: {total_before/1e6:.1f} MB -> {total_after/1e6:.1f} MB")
