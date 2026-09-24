#!/usr/bin/env python3
import argparse
import os
from pathlib import Path


def infer_root(client_jar: Path) -> Path:
    current = client_jar.parent

    # libraries/net/minecraft/26.3/client.jar -> libraries
    for _ in range(3):
        current = current.parent

    return current


def usable_jar(path: Path, root: Path) -> bool:
    rel = path.relative_to(root).as_posix().lower()
    name = path.name.lower()

    if rel.startswith("net/minecraft/"):
        return False

    if "natives" in name:
        return False

    if name.endswith("-sources.jar") or name.endswith("-javadoc.jar"):
        return False

    return True


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Inspect the local Pandora/Minecraft Java library classpath"
    )
    parser.add_argument("client_jar", type=Path)
    parser.add_argument(
        "--root",
        type=Path,
        help="override the inferred Pandora libraries directory",
    )
    parser.add_argument(
        "--print-classpath",
        action="store_true",
        help="print an OS-separated classpath instead of a summary",
    )
    args = parser.parse_args()

    client_jar = args.client_jar.expanduser().resolve()
    root = (
        args.root.expanduser().resolve()
        if args.root
        else infer_root(client_jar)
    )

    if not client_jar.is_file():
        raise SystemExit(f"client JAR not found: {client_jar}")

    if not root.is_dir():
        raise SystemExit(f"library root not found: {root}")

    jars = sorted(
        path
        for path in root.rglob("*.jar")
        if usable_jar(path, root)
    )

    if args.print_classpath:
        print(os.pathsep.join(str(path) for path in jars))
        return 0

    print(f"Client: {client_jar}")
    print(f"Libraries: {root}")
    print(f"Java dependency JARs: {len(jars)}")

    for path in jars[:40]:
        print(f"  {path.relative_to(root)}")

    if len(jars) > 40:
        print(f"  ... and {len(jars) - 40} more")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
