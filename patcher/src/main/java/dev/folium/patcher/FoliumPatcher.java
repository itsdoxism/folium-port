package dev.folium.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Build-time transformer for the locally supplied Minecraft client JAR.
 *
 * It never ships or downloads Minecraft. The user provides their own local
 * client JAR and Folium writes a generated patched copy under build output.
 */
public final class FoliumPatcher {
    private static final String PREFERRED_GRAPHICS =
        "net/minecraft/client/PreferredGraphicsApi.class";

    private static final String RENDER_SYSTEM =
        "com/mojang/blaze3d/systems/RenderSystem.class";

    private FoliumPatcher() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println(
                "usage: FoliumPatcher <minecraft-client-26.3.jar> <patched-output.jar>"
            );
            System.exit(2);
        }

        Path input = Path.of(args[0]).toAbsolutePath();
        Path output = Path.of(args[1]).toAbsolutePath();

        if (!Files.isRegularFile(input)) {
            throw new IOException("Minecraft client JAR not found: " + input);
        }

        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Set<String> patched = new HashSet<>();
        Set<String> strippedSignatures = new HashSet<>();

        try (
            InputStream rawIn = Files.newInputStream(input);
            ZipInputStream zin = new ZipInputStream(rawIn);
            OutputStream rawOut = Files.newOutputStream(output);
            ZipOutputStream zout = new ZipOutputStream(rawOut)
        ) {
            ZipEntry entry;

            while ((entry = zin.getNextEntry()) != null) {
                String entryName = entry.getName();

                // The official client JAR is signed. Any bytecode transform
                // invalidates those signatures, so generated Folium build
                // artifacts must not retain stale signature blocks.
                if (isJarSignature(entryName)) {
                    strippedSignatures.add(entryName);
                    continue;
                }

                ZipEntry outEntry = new ZipEntry(entryName);
                outEntry.setTime(entry.getTime());
                zout.putNextEntry(outEntry);

                byte[] data = zin.readAllBytes();

                if (PREFERRED_GRAPHICS.equals(entryName)) {
                    data = patchPreferredGraphicsApi(data);
                    patched.add(PREFERRED_GRAPHICS);
                } else if (RENDER_SYSTEM.equals(entryName)) {
                    data = patchRenderSystem(data);
                    patched.add(RENDER_SYSTEM);
                }

                zout.write(data);
                zout.closeEntry();
            }
        } catch (Throwable failure) {
            Files.deleteIfExists(output);
            throw failure;
        }

        if (!patched.contains(PREFERRED_GRAPHICS) || !patched.contains(RENDER_SYSTEM)) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                "Expected Minecraft 26.3 classes were not found; refusing to emit a partial patch"
            );
        }

        System.out.println("Folium patched: " + input);
        System.out.println("Output: " + output);
        System.out.println("  ✓ PreferredGraphicsApi.getBackendsToTry");
        System.out.println("  ✓ RenderSystem.initBackendSystem");

        for (String signature : strippedSignatures) {
            System.out.println("  ✓ stripped stale JAR signature: " + signature);
        }
    }

    private static boolean isJarSignature(String entryName) {
        String normalized = entryName.toUpperCase(Locale.ROOT);

        if (!normalized.startsWith("META-INF/")) {
            return false;
        }

        return normalized.endsWith(".SF")
            || normalized.endsWith(".RSA")
            || normalized.endsWith(".DSA")
            || normalized.endsWith(".EC");
    }

    private static byte[] patchPreferredGraphicsApi(byte[] original) {
        ClassNode node = read(original);

        MethodNode method = requireMethod(
            node,
            "getBackendsToTry",
            "()[Lcom/mojang/renderpearl/api/device/GpuBackend;"
        );

        clearMethod(method);

        InsnList code = method.instructions;
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new TypeInsnNode(
            Opcodes.ANEWARRAY,
            "com/mojang/renderpearl/api/device/GpuBackend"
        ));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new TypeInsnNode(
            Opcodes.NEW,
            "dev/folium/render/webgpu/FoliumWebGpuBackend"
        ));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "dev/folium/render/webgpu/FoliumWebGpuBackend",
            "<init>",
            "()V",
            false
        ));
        code.add(new InsnNode(Opcodes.AASTORE));
        code.add(new InsnNode(Opcodes.ARETURN));

        method.maxStack = 4;
        method.maxLocals = 1;

        return write(node);
    }

    private static byte[] patchRenderSystem(byte[] original) {
        ClassNode node = read(original);

        MethodNode method = requireMethod(
            node,
            "initBackendSystem",
            "()Lnet/minecraft/util/TimeSource$NanoTimeSource;"
        );

        clearMethod(method);

        method.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/bridge/FoliumTime",
            "nanoTimeSource",
            "()Lnet/minecraft/util/TimeSource$NanoTimeSource;",
            false
        ));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));

        method.maxStack = 1;
        method.maxLocals = 0;

        return write(node);
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode requireMethod(
        ClassNode node,
        String name,
        String descriptor
    ) {
        return node.methods.stream()
            .filter(method ->
                method.name.equals(name) && method.desc.equals(descriptor)
            )
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Method not found: " + node.name + "." + name + descriptor
            ));
    }

    private static void clearMethod(MethodNode method) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();

        if (method.localVariables != null) {
            method.localVariables.clear();
        }

        if (method.visibleLocalVariableAnnotations != null) {
            method.visibleLocalVariableAnnotations.clear();
        }

        if (method.invisibleLocalVariableAnnotations != null) {
            method.invisibleLocalVariableAnnotations.clear();
        }
    }
}
