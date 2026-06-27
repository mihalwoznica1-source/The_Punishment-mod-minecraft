#!/usr/bin/env python3
"""
Generuje teksturę encji "The Punishment" — czysty czarny PNG 64x64.
Uruchom: python3 generate_texture.py
Wynik: the_punishment.png (umieść w assets/punishment/textures/entity/)
"""

import struct
import zlib
import os

def create_black_png(width: int, height: int, output_path: str) -> None:
    """
    Tworzy plik PNG wypełniony czystym czarnym kolorem (#000000, alpha=255).

    Format PNG: nagłówek + chunki IHDR, IDAT, IEND.
    Używamy czystej biblioteki standardowej — brak zależności zewnętrznych.
    """

    def make_chunk(chunk_type: bytes, data: bytes) -> bytes:
        """Składa chunk PNG: długość + typ + dane + CRC"""
        crc = zlib.crc32(chunk_type + data) & 0xFFFFFFFF
        return (
            struct.pack('>I', len(data)) +
            chunk_type +
            data +
            struct.pack('>I', crc)
        )

    # PNG signature
    PNG_SIGNATURE = b'\x89PNG\r\n\x1a\n'

    # IHDR chunk: szerokość, wysokość, bit depth=8, colortype=2 (RGB)
    # Używamy RGB (bez alpha) — Minecraft obsługuje oba formaty dla encji
    ihdr_data = struct.pack('>IIBBBBB',
        width,    # szerokość
        height,   # wysokość
        8,        # bit depth
        2,        # color type: 2 = RGB (truecolor)
        0,        # compression method
        0,        # filter method
        0         # interlace method
    )

    # IDAT chunk: dane obrazu (scanlines z filter byte)
    # Filter byte 0 = None (brak filtrowania) na początku każdego wiersza
    raw_rows = []
    for _ in range(height):
        row = b'\x00'  # filter byte
        row += b'\x00\x00\x00' * width  # RGB = czarny dla każdego piksela
        raw_rows.append(row)

    raw_data = b''.join(raw_rows)
    compressed = zlib.compress(raw_data, 9)

    # Złóż plik PNG
    png_data = (
        PNG_SIGNATURE +
        make_chunk(b'IHDR', ihdr_data) +
        make_chunk(b'IDAT', compressed) +
        make_chunk(b'IEND', b'')
    )

    with open(output_path, 'wb') as f:
        f.write(png_data)

    size_kb = len(png_data) / 1024
    print(f"[OK] Wygenerowano teksturę: {output_path}")
    print(f"     Rozmiar: {width}x{height} px, {size_kb:.2f} KB")
    print(f"     Kolor: #000000 (czysty czarny)")


if __name__ == '__main__':
    output = 'the_punishment.png'
    create_black_png(64, 64, output)
    print(f"\nSkopiuj plik do:")
    print(f"  src/main/resources/assets/punishment/textures/entity/the_punishment.png")
