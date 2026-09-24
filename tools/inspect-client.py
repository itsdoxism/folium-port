#!/usr/bin/env python3
import json
import sys
import zipfile
from collections import Counter
from pathlib import Path

INTERESTING_PREFIXES = (
    "com/mojang/blaze3d/",
    "net/minecraft/client/",
)

PLATFORM_MARKERS = {
    "sdl": ("SDL", "Sdl"),
    "glfw": ("GLFW", "Glfw"),
    "lwjgl": ("LWJGL", "lwjgl"),
    "netty": ("io/netty/", "Netty"),
    "jna": ("com/sun/jna/", "JNA"),
    "opengl": ("OpenGL", "GlState", "Gpu"),
    "openal": ("OpenAl", "OpenAL"),
}

def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python tools/inspect-client.py <minecraft-client.jar>")
        return 2

    jar_path = Path(sys.argv[1]).expanduser().resolve()
    if not jar_path.is_file():
        print(f"error: file not found: {jar_path}")
        return 2

    with zipfile.ZipFile(jar_path) as zf:
        names = zf.namelist()
        classes = [n for n in names if n.endswith(".class")]
        version = {}
        if "version.json" in names:
            version = json.loads(zf.read("version.json").decode("utf-8"))

        top_packages = Counter()
        for name in classes:
            parts = name.split("/")
            if len(parts) >= 3:
                top_packages["/".join(parts[:3])] += 1

        platform_hits = {}
        for key, markers in PLATFORM_MARKERS.items():
            hits = [
                name for name in names
                if any(marker.lower() in name.lower() for marker in markers)
            ]
            platform_hits[key] = hits[:200]

        report = {
            "jar": {
                "filename": jar_path.name,
                "entries": len(names),
                "classes": len(classes),
            },
            "minecraft": version,
            "top_packages": top_packages.most_common(40),
            "platform_markers": platform_hits,
            "important_classes": [
                name for name in classes
                if name.startswith(INTERESTING_PREFIXES)
            ][:1000],
        }

    out_dir = Path("reports")
    out_dir.mkdir(parents=True, exist_ok=True)
    version_id = version.get("id", "unknown")
    out_path = out_dir / f"client-{version_id}.json"
    out_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    print(f"Minecraft: {version.get('name', version_id)}")
    print(f"Java target: {version.get('java_version', 'unknown')}")
    print(f"Protocol: {version.get('protocol_version', 'unknown')}")
    print(f"Classes: {report['jar']['classes']}")
    print(f"Wrote: {out_path}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
