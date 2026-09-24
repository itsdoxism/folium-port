#!/usr/bin/env python3
"""
Classfile-level helper for locating RenderPearl vertex-format APIs in a local
Minecraft client JAR. It intentionally does not decompile or redistribute
Minecraft source.
"""

import sys
import zipfile
from pathlib import Path

TARGETS = (
    "com/mojang/renderpearl/api/vertex/VertexFormat.class",
    "com/mojang/renderpearl/api/vertex/VertexFormatElement.class",
    "com/mojang/blaze3d/vertex/DefaultVertexFormat.class",
)


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python tools/dump-vertex-formats.py <minecraft-client.jar>")
        return 2

    jar = Path(sys.argv[1]).expanduser().resolve()
    if not jar.is_file():
        print(f"error: file not found: {jar}")
        return 2

    with zipfile.ZipFile(jar) as zf:
        names = set(zf.namelist())
        print("RenderPearl vertex-format inventory")
        print("----------------------------------")
        for target in TARGETS:
            print(("FOUND " if target in names else "MISS  ") + target)

        related = sorted(
            name for name in names
            if "VertexFormat" in name and name.endswith(".class")
        )

        print()
        print("Related classes:")
        for name in related:
            print("  " + name)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
