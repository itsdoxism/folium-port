#!/usr/bin/env python3
import struct
import sys
import zipfile
from pathlib import Path

TARGETS = {
    "net/minecraft/client/renderer/ShaderManager.class": {
        ("loadConfigs", "(Lnet/minecraft/server/packs/resources/ResourceManager;)Lnet/minecraft/client/renderer/ShaderManager$Configs;"),
        ("loadInclude", "(Lnet/minecraft/resources/Identifier;Lnet/minecraft/server/packs/resources/Resource;Lcom/google/common/collect/ImmutableMap$Builder;)V"),
    },
    "net/minecraft/client/PreferredGraphicsApi.class": {
        ("getBackendsToTry", "()[Lcom/mojang/renderpearl/api/device/GpuBackend;"),
    },
    "com/mojang/blaze3d/systems/RenderSystem.class": {
        ("initBackendSystem", "()Lnet/minecraft/util/TimeSource$NanoTimeSource;"),
    },
    "com/mojang/blaze3d/platform/SDLEventHandler.class": {
        ("pollEvents", "()V"),
        ("flushInputEvents", "()V"),
    },
    "com/mojang/blaze3d/platform/InputConstants.class": {
        ("isKeyDown", "(I)Z"),
        ("grabMouse", "(Lcom/mojang/blaze3d/platform/Window;DD)V"),
        ("releaseMouse", "(Lcom/mojang/blaze3d/platform/Window;DD)V"),
    },
    "com/mojang/blaze3d/platform/Window.class": {
        ("<init>", "(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;ZLjava/lang/String;Lcom/mojang/blaze3d/platform/MonitorManager;Lcom/mojang/renderpearl/api/device/GpuBackend;I)V"),
        ("getPlatform", "()Ljava/lang/String;"),
        ("queryFramebufferSize", "()Lcom/mojang/blaze3d/platform/Window$FramebufferSize;"),
        ("refreshFramebufferSize", "()V"),
        ("setTitle", "(Ljava/lang/String;)V"),
        ("setWindowMaxSize", "(II)V"),
        ("close", "()V"),
        ("setIcon", "(Lnet/minecraft/server/packs/PackMetadataResources;Lcom/mojang/blaze3d/platform/IconSet;)V"),
        ("updateFullscreenIfChanged", "()V"),
        ("changeFullscreenVideoMode", "()V"),
        ("selectCursor", "(Lcom/mojang/blaze3d/platform/cursor/CursorType;)V"),
        ("setFullscreen", "(Z)V"),
    },
}


def read_methods(data: bytes):
    i = 8
    cp_count = struct.unpack_from(">H", data, i)[0]
    i += 2
    cp = [None] * cp_count
    index = 1

    while index < cp_count:
        tag = data[i]
        i += 1

        if tag == 1:
            length = struct.unpack_from(">H", data, i)[0]
            i += 2
            cp[index] = data[i:i + length].decode("utf-8", "replace")
            i += length
        elif tag in (3, 4):
            i += 4
        elif tag in (5, 6):
            i += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            i += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            i += 4
        elif tag == 15:
            i += 3
        else:
            raise ValueError(f"unknown constant-pool tag {tag}")

        index += 1

    i += 6
    interfaces = struct.unpack_from(">H", data, i)[0]
    i += 2 + interfaces * 2

    fields = struct.unpack_from(">H", data, i)[0]
    i += 2

    for _ in range(fields):
        i += 6
        attributes = struct.unpack_from(">H", data, i)[0]
        i += 2
        for _ in range(attributes):
            i += 2
            length = struct.unpack_from(">I", data, i)[0]
            i += 4 + length

    method_count = struct.unpack_from(">H", data, i)[0]
    i += 2
    methods = []

    for _ in range(method_count):
        _access, name_index, descriptor_index = struct.unpack_from(">HHH", data, i)
        i += 6
        attributes = struct.unpack_from(">H", data, i)[0]
        i += 2

        methods.append((cp[name_index], cp[descriptor_index]))

        for _ in range(attributes):
            i += 2
            length = struct.unpack_from(">I", data, i)[0]
            i += 4 + length

    return methods


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: python tools/verify-patch-targets.py <minecraft-client.jar>")
        return 2

    jar = Path(sys.argv[1]).expanduser().resolve()
    if not jar.is_file():
        print(f"error: file not found: {jar}")
        return 2

    failures = []

    with zipfile.ZipFile(jar) as zf:
        names = set(zf.namelist())

        for class_name, expected in TARGETS.items():
            if class_name not in names:
                failures.append(f"missing class: {class_name}")
                continue

            methods = read_methods(zf.read(class_name))

            for target in expected:
                count = methods.count(target)
                status = "OK" if count == 1 else "FAIL"
                print(f"{status} {class_name} :: {target[0]}{target[1]} [{count}]")
                if count != 1:
                    failures.append(
                        f"{class_name}::{target[0]}{target[1]} count={count}"
                    )

        signatures = [
            name for name in names
            if name.upper().startswith("META-INF/")
            and name.upper().endswith((".SF", ".RSA", ".DSA", ".EC"))
        ]

        print(f"signed metadata entries: {len(signatures)}")
        for name in signatures:
            print(f"  {name}")

    if failures:
        print("\nPatch target verification failed:")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print("\nAll Folium 26.3 patch targets matched exactly once.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
