package dev.folium.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public final class FoliumPatcherMain {
    private static final String SHADER_MANAGER =
        "net/minecraft/client/renderer/ShaderManager.class";
    private static final String PREFERRED_GRAPHICS_API =
        "net/minecraft/client/PreferredGraphicsApi.class";

    private FoliumPatcherMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println(
                "usage: folium-patcher <minecraft-client.jar> <patched-client.jar>"
            );
            System.exit(2);
        }

        Path input = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();

        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException(
                "Minecraft client JAR does not exist: " + input
            );
        }

        if (input.equals(output)) {
            throw new IllegalArgumentException(
                "Refusing to overwrite the original Minecraft client JAR"
            );
        }

        Files.createDirectories(output.getParent());

        List<String> applied = patchJar(input, output);

        System.out.println("Folium client patch complete");
        System.out.println("input:  " + input);
        System.out.println("output: " + output);
        System.out.println("input sha256:  " + sha256(input));
        System.out.println("output sha256: " + sha256(output));
        System.out.println("patches:");

        for (String patch : applied) {
            System.out.println("  - " + patch);
        }
    }

    private static List<String> patchJar(
        Path input,
        Path output
    ) throws IOException {
        List<String> applied = new ArrayList<>();
        boolean sawShaderManager = false;
        boolean sawGraphicsApi = false;

        try (
            JarFile jar = new JarFile(input.toFile());
            OutputStream fileOut = Files.newOutputStream(output);
            JarOutputStream out = new JarOutputStream(fileOut)
        ) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                JarEntry copy = new JarEntry(entry.getName());
                copy.setTime(0L);
                out.putNextEntry(copy);

                if (entry.isDirectory()) {
                    out.closeEntry();
                    continue;
                }

                byte[] bytes;
                try (InputStream in = jar.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                if (SHADER_MANAGER.equals(entry.getName())) {
                    bytes = patchShaderManager(bytes, applied);
                    sawShaderManager = true;
                } else if (PREFERRED_GRAPHICS_API.equals(entry.getName())) {
                    bytes = patchPreferredGraphicsApi(bytes, applied);
                    sawGraphicsApi = true;
                }

                out.write(bytes);
                out.closeEntry();
            }
        } catch (Throwable failure) {
            Files.deleteIfExists(output);
            throw failure;
        }

        if (!sawShaderManager || !sawGraphicsApi) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                "Input JAR does not match the expected Minecraft 26.3 client layout"
            );
        }

        return List.copyOf(applied);
    }

    private static byte[] patchShaderManager(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode loadConfigs = requireMethod(
            node,
            "loadConfigs",
            "(Lnet/minecraft/server/packs/resources/ResourceManager;)" +
                "Lnet/minecraft/client/renderer/ShaderManager$Configs;"
        );

        loadConfigs.instructions.insert(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumShaderReloadHook",
            "beginReload",
            "()V",
            false
        ));

        int commits = 0;
        for (
            AbstractInsnNode insn = loadConfigs.instructions.getFirst();
            insn != null;
            insn = insn.getNext()
        ) {
            if (insn.getOpcode() == Opcodes.ARETURN) {
                loadConfigs.instructions.insertBefore(insn, new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "dev/folium/render/webgpu/FoliumShaderReloadHook",
                    "commitReload",
                    "()V",
                    false
                ));
                commits++;
            }
        }

        if (commits != 1) {
            throw drift(
                "ShaderManager.loadConfigs expected exactly 1 ARETURN, found " +
                    commits
            );
        }

        MethodNode loadInclude = requireMethod(
            node,
            "loadInclude",
            "(Lnet/minecraft/resources/Identifier;" +
                "Lnet/minecraft/server/packs/resources/Resource;" +
                "Lcom/google/common/collect/ImmutableMap$Builder;)V"
        );

        InsnList capture = new InsnList();
        capture.add(new VarInsnNode(Opcodes.ALOAD, 0));
        capture.add(new VarInsnNode(Opcodes.ALOAD, 1));
        capture.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumShaderReloadHook",
            "captureInclude",
            "(Lnet/minecraft/resources/Identifier;" +
                "Lnet/minecraft/server/packs/resources/Resource;)V",
            false
        ));
        loadInclude.instructions.insert(capture);

        applied.add("ShaderManager.loadConfigs: begin/commit raw include reload");
        applied.add("ShaderManager.loadInclude: capture raw include source");

        return write(node);
    }

    private static byte[] patchPreferredGraphicsApi(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode method = requireMethod(
            node,
            "getBackendsToTry",
            "()[Lcom/mojang/renderpearl/api/device/GpuBackend;"
        );

        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }

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

        method.maxStack = 5;
        method.maxLocals = Math.max(method.maxLocals, 1);

        applied.add(
            "PreferredGraphicsApi.getBackendsToTry: force FoliumWebGpuBackend"
        );

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
        List<MethodNode> matches = node.methods.stream()
            .filter(method ->
                method.name.equals(name) &&
                method.desc.equals(descriptor)
            )
            .toList();

        if (matches.size() != 1) {
            throw drift(
                node.name + "." + name + descriptor +
                    " expected exactly once, found " + matches.size()
            );
        }

        return matches.getFirst();
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException(
            "Minecraft 26.3 patch target drift: " + message
        );
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
