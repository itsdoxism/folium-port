#!/usr/bin/env python3
import re
import sys
import zipfile
from pathlib import Path

INCLUDE = re.compile(
    r'(?m)^\s*#include\s+[<\"]([^>\"]+)[>\"]\s*$'
)


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python tools/check-shader-includes.py <minecraft-client.jar>")
        return 2

    jar = Path(sys.argv[1]).expanduser().resolve()
    if not jar.is_file():
        print(f"error: file not found: {jar}")
        return 2

    includes: dict[str, str] = {}
    shaders: dict[str, str] = {}

    with zipfile.ZipFile(jar) as zf:
        for name in zf.namelist():
            if not name.startswith("assets/"):
                continue

            parts = name.split("/")
            if len(parts) < 3:
                continue

            namespace = parts[1]

            if "/shaders/include/" in name and name.endswith(".glsl"):
                relative = name.split("/shaders/include/", 1)[1]
                includes[f"{namespace}:{relative}"] = (
                    zf.read(name).decode("utf-8")
                )

            if "/shaders/core/" in name and name.endswith((".vsh", ".fsh")):
                relative = name.split("/shaders/core/", 1)[1]
                shaders[f"{namespace}:{relative}"] = (
                    zf.read(name).decode("utf-8")
                )

    missing: list[tuple[str, str]] = []
    cycles: list[list[str]] = []
    max_depth = 0

    def expand(
        owner: str,
        source: str,
        stack: list[str],
        depth: int,
    ) -> str:
        nonlocal max_depth
        max_depth = max(max_depth, depth)

        def replace(match: re.Match[str]) -> str:
            raw = match.group(1)
            include_id = raw if ":" in raw else f"minecraft:{raw}"

            if include_id in stack:
                cycles.append(stack + [include_id])
                return ""

            include_source = includes.get(include_id)
            if include_source is None:
                missing.append((owner, include_id))
                return ""

            return expand(
                include_id,
                include_source,
                stack + [include_id],
                depth + 1,
            )

        return INCLUDE.sub(replace, source)

    for shader_id, source in shaders.items():
        expanded = expand(shader_id, source, [], 0)
        if INCLUDE.search(expanded):
            missing.append((shader_id, "<still unresolved>"))

    print(f"core shaders: {len(shaders)}")
    print(f"raw includes: {len(includes)}")
    print(f"missing includes: {len(missing)}")
    print(f"include cycles: {len(cycles)}")
    print(f"max include depth: {max_depth}")

    if missing:
        print("\nMissing:")
        for owner, include_id in missing:
            print(f"  {owner} -> {include_id}")

    if cycles:
        print("\nCycles:")
        for cycle in cycles:
            print("  " + " -> ".join(cycle))

    return 1 if missing or cycles else 0


if __name__ == "__main__":
    raise SystemExit(main())
