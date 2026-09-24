#!/usr/bin/env python3
import json
import struct
import sys
import zipfile
from pathlib import Path


def u2(data, offset):
    return struct.unpack_from(">H", data, offset)[0], offset + 2


def u4(data, offset):
    return struct.unpack_from(">I", data, offset)[0], offset + 4


def parse_constant_pool(data):
    count = struct.unpack_from(">H", data, 8)[0]
    offset = 10
    pool = [None] * count
    index = 1

    while index < count:
        tag = data[offset]
        offset += 1

        if tag == 1:
            length = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            raw = data[offset:offset + length]
            offset += length
            pool[index] = (tag, raw.decode("utf-8", "replace"))
        elif tag in (3, 4):
            offset += 4
            pool[index] = (tag, None)
        elif tag in (5, 6):
            offset += 8
            pool[index] = (tag, None)
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            ref = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            pool[index] = (tag, ref)
        elif tag in (9, 10, 11, 12, 17, 18):
            offset += 4
            pool[index] = (tag, None)
        elif tag == 15:
            offset += 3
            pool[index] = (tag, None)
        else:
            raise ValueError(f"unknown constant-pool tag {tag}")

        index += 1

    return pool, offset


def utf8(pool, index):
    entry = pool[index]
    return entry[1] if entry and entry[0] == 1 else None


def class_name(pool, index):
    if index == 0:
        return None
    entry = pool[index]
    if not entry or entry[0] != 7:
        return None
    return utf8(pool, entry[1])


def skip_attributes(data, offset):
    count, offset = u2(data, offset)
    for _ in range(count):
        _, offset = u2(data, offset)
        length, offset = u4(data, offset)
        offset += length
    return offset


def parse_class(data):
    pool, offset = parse_constant_pool(data)
    access, offset = u2(data, offset)
    this_index, offset = u2(data, offset)
    super_index, offset = u2(data, offset)

    interface_count, offset = u2(data, offset)
    interfaces = []
    for _ in range(interface_count):
        index, offset = u2(data, offset)
        interfaces.append(class_name(pool, index))

    field_count, offset = u2(data, offset)
    fields = []
    for _ in range(field_count):
        member_access, offset = u2(data, offset)
        name_index, offset = u2(data, offset)
        descriptor_index, offset = u2(data, offset)
        fields.append({
            "access": member_access,
            "name": utf8(pool, name_index),
            "descriptor": utf8(pool, descriptor_index),
        })
        offset = skip_attributes(data, offset)

    method_count, offset = u2(data, offset)
    methods = []
    for _ in range(method_count):
        member_access, offset = u2(data, offset)
        name_index, offset = u2(data, offset)
        descriptor_index, offset = u2(data, offset)
        methods.append({
            "access": member_access,
            "name": utf8(pool, name_index),
            "descriptor": utf8(pool, descriptor_index),
        })
        offset = skip_attributes(data, offset)

    return {
        "class": class_name(pool, this_index),
        "super": class_name(pool, super_index),
        "interfaces": interfaces,
        "access": access,
        "fields": fields,
        "methods": methods,
    }


def main() -> int:
    if len(sys.argv) < 3:
        print("usage: python tools/dump-class-api.py <jar> <internal-class> [more...]")
        return 2

    jar_path = Path(sys.argv[1]).expanduser().resolve()

    with zipfile.ZipFile(jar_path) as jar:
        results = []
        for requested in sys.argv[2:]:
            internal_name = requested.replace(".", "/")
            entry = internal_name + ".class"
            results.append(parse_class(jar.read(entry)))

    print(json.dumps(results, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
