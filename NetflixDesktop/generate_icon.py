"""Draw the original Netflix Floating Panel skull icon for Windows builds."""
from PIL import Image, ImageDraw, ImageFilter
from pathlib import Path
import math

SIZE = 900
im = Image.new("RGBA", (SIZE,SIZE), (0,0,0,0))
glow = Image.new("RGBA", (SIZE,SIZE), (0,0,0,0))
gd = ImageDraw.Draw(glow)
gd.ellipse((65,65,835,835),fill=(227,14,42,135),outline=(252,40,57,160),width=24)
halo = glow.filter(ImageFilter.GaussianBlur(75))
im.alpha_composite(halo)
d = ImageDraw.Draw(im)
# Dark background, layered concentric red outlines and flares.
d.ellipse((105,105,795,795), fill=(8,11,20,255), outline=(238,14,48,250), width=17)
d.ellipse((125,125,775,775), outline=(120,13,28,220), width=8)
# Angular crown, distinctly visible in floating 48px icon.
crown=[(230,290),(174,165),(345,233),(453,120),(567,232),(725,168),(675,297)]
d.polygon(crown, fill=(210,22,38,255))
d.line(crown+[crown[0]], fill=(255,89,88,255),width=14,joint="curve")
d.ellipse((436,153,472,191),fill=(240,249,255,255))
# Bones: skull and jaw, preserving friendly comic-book shape at small size.
d.ellipse((224,260,676,635),fill=(235,244,250,255),outline=(76,98,116,255),width=18)
d.polygon([(273,525),(627,525),(604,715),(298,715)],fill=(229,237,246,255))
d.ellipse((192,485,325,584),fill=(232,240,249,255),outline=(76,98,116,255),width=12)
d.ellipse((575,485,709,584),fill=(232,240,249,255),outline=(76,98,116,255),width=12)
# Eye sockets and electric blue irises.
d.ellipse((264,388,442,543),fill=(9,24,37,255))
d.ellipse((458,388,636,543),fill=(9,24,37,255))
d.ellipse((284,403,431,534),fill=(23,215,255,255),outline=(100,248,254,255),width=7)
d.ellipse((468,403,615,534),fill=(23,215,255,255),outline=(100,248,254,255),width=7)
d.ellipse((349,436,414,516),fill=(19,63,83,255))
d.ellipse((488,436,553,516),fill=(19,63,83,255))
d.ellipse((327,424,350,448),fill=(255,255,255,255))
d.ellipse((492,424,515,448),fill=(255,255,255,255))
# Nose, mouth and separate bone teeth.
d.polygon([(450,516),(401,605),(498,605)],fill=(14,25,35,255))
d.rounded_rectangle((330,629,570,697),radius=14,fill=(43,53,67,255))
for x in (337,386,436,486,535):
    d.rounded_rectangle((x,632,x+40,692),radius=5,fill=(248,250,254,255))
# Red chin and subtle cyan cheek reflections.
d.arc((230,320,660,645),5,176,fill=(127,173,194,130),width=10)
d.line([(314,721),(586,721)],fill=(230,25,48,255),width=18)
small = im.resize((256,256),Image.Resampling.LANCZOS)
out = Path(__file__).parent / "Netflix.ico"
small.save(out,format="ICO",sizes=[(256,256),(128,128),(64,64),(48,48),(32,32),(16,16)])
print(f"Generated icon: {out}")
