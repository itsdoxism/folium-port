#!/usr/bin/env python3
import json
import re
import struct
import sys
import zipfile
from collections import Counter
from pathlib import Path

JDK_PREFIXES = ("java/", "javax/", "jdk/", "sun/", "com/sun/")
DESCRIPTOR_RE = re.compile(r"L([A-Za-z0-9_$/]+);")
INTERNAL_RE = re.compile(r"^(?:\[+L)?([A-Za-z0-9_$/]+);?$")


def parse_refs(data: bytes):
    if len(data) < 10 or data[:4] != b"\xca\xfe\xba\xbe":
        return set(), None

    minor = struct.unpack_from(">H", data, 4)[0]
    major = struct.unpack_from(">H", data, 6)[0]
    cp_count = struct.unpack_from(">H", data, 8)[0]
    offset = 10
    cp = [None] * cp_count
    index = 1

    while index < cp_count:
        tag = data[offset]
        offset += 1

        if tag == 1:
            length = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            raw = data[offset:offset + length]
            offset += length
            cp[index] = (tag, raw.decode("utf-8", "replace"))
        elif tag in (3, 4):
            offset += 4
            cp[index] = (tag, None)
        elif tag in (5, 6):
            offset += 8
            cp[index] = (tag, None)
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            ref = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            cp[index] = (tag, ref)
        elif tag in (9, 10, 11, 12, 17, 18):
            offset += 4
            cp[index] = (tag, None)
        elif tag == 15:
            offset += 3
            cp[index] = (tag, None)
        else:
            raise ValueError(f"unknown constant-pool tag {tag} at index {index}")

        index += 1

    refs = set()
    for entry in cp[1:]:
        if not entry:
            continue

        tag, value = entry
        if tag == 7 and isinstance(value, int) and 0 < value < len(cp):
            name_entry = cp[value]
            if name_entry and name_entry[0] == 1:
                match = INTERNAL_RE.match(name_entry[1])
                if match:
                    refs.add(match.group(1))
        elif tag == 1:
            for match in DESCRIPTOR_RE.finditer(value):
                refs.add(match.group(1))

    return refs, {"major": major, "minor": minor}


def package_key(name: str):
    parts = name.split("/")
    if len(parts) >= 3:
        return "/".join(parts[:3])
    if len(parts) >= 2:
        return "/".join(parts[:2])
    return parts[0]


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python tools/scan-dependencies.py <minecraft-client.jar>")
        return 2

    jar_path = Path(sys.argv[1]).expanduser().resolve()
    if not jar_path.is_file():
        print(f"error: file not found: {jar_path}")
        return 2

    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        own_classes = {name[:-6] for name in names if name.endswith(".class")}
        all_refs = set()
        classes_with_external_refs = {}
        classfile_versions = Counter()
        parse_errors = []

        for name in names:
            if not name.endswith(".class"):
                continue

            try:
                refs, version = parse_refs(jar.read(name))
                if version:
                    classfile_versions[f"{version['major']}.{version['minor']}"] += 1
            except Exception as exc:
                parse_errors.append({"class": name, "error": str(exc)})
                continue

            all_refs.update(refs)
            external = sorted(
                ref for ref in refs
                if ref not in own_classes and not ref.startswith(JDK_PREFIXES)
            )
            if external:
                classes_with_external_refs[name[:-6]] = external

        external_refs = sorted(
            ref for ref in all_refs
            if ref not in own_classes and not ref.startswith(JDK_PREFIXES)
        )
        package_counts = Counter(package_key(ref) for ref in external_refs)

        blocker_prefixes = {
            "sdl": ("org/lwjgl/sdl",),
            "lwjgl": ("org/lwjgl",),
            "netty": ("io/netty",),
            "oshi": ("oshi/",),
            "openal": ("org/lwjgl/openal",),
            "opengl": ("org/lwjgl/opengl",),
            "vulkan": ("org/lwjgl/vulkan",),
        }

        blocker_hits = {
            key: [ref for ref in external_refs if ref.startswith(prefixes)]
            for key, prefixes in blocker_prefixes.items()
        }

        version_json = {}
        if "version.json" in names:
            version_json = json.loads(jar.read("version.json").decode("utf-8"))

    report = {
        "jar": {
            "filename": jar_path.name,
        },
        "minecraft": version_json,
        "classfile_versions": dict(classfile_versions),
        "external_class_references": external_refs,
        "external_package_counts": package_counts.most_common(),
        "known_blocker_hits": blocker_hits,
        "classes_with_external_refs": classes_with_external_refs,
        "parse_errors": parse_errors,
    }

    report_dir = Path("reports")
    report_dir.mkdir(parents=True, exist_ok=True)
    version_id = version_json.get("id", "unknown")
    output = report_dir / f"dependencies-{version_id}.json"
    output.write_text(json.dumps(report, indent=2), encoding="utf-8")

    print(f"Minecraft: {version_json.get('name', version_id)}")
    print(f"External class refs: {len(external_refs)}")
    print("Top external packages:")
    for package, count in package_counts.most_common(20):
        print(f"  {package}: {count}")

    print("Known blockers:")
    for key, refs in blocker_hits.items():
        print(f"  {key}: {len(refs)}")

    print(f"Parse errors: {len(parse_errors)}")
    print(f"Wrote: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
