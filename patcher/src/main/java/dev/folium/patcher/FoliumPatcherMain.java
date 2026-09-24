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
import java.nio.charset.StandardCharsets;
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
    private static final String RENDER_SYSTEM =
        "com/mojang/blaze3d/systems/RenderSystem.class";
    private static final String WINDOW =
        "com/mojang/blaze3d/platform/Window.class";
    private static final String SDL_EVENT_HANDLER =
        "com/mojang/blaze3d/platform/SDLEventHandler.class";
    private static final String INPUT_CONSTANTS =
        "com/mojang/blaze3d/platform/InputConstants.class";

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
        boolean sawRenderSystem = false;
        boolean sawWindow = false;
        boolean sawEventHandler = false;
        boolean sawInputConstants = false;

        try (
            JarFile jar = new JarFile(input.toFile());
            OutputStream fileOut = Files.newOutputStream(output);
            JarOutputStream out = new JarOutputStream(fileOut)
        ) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (isSignatureEntry(entry.getName())) {
                    applied.add("strip JAR signature metadata: " + entry.getName());
                    continue;
                }

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

                if ("META-INF/MANIFEST.MF".equalsIgnoreCase(entry.getName())) {
                    bytes = (
                        "Manifest-Version: 1.0\r\n" +
                        "Main-Class: net.minecraft.client.Main\r\n" +
                        "\r\n"
                    ).getBytes(StandardCharsets.UTF_8);
                    applied.add("sanitize signed JAR manifest");
                } else if (SHADER_MANAGER.equals(entry.getName())) {
                    bytes = patchShaderManager(bytes, applied);
                    sawShaderManager = true;
                } else if (PREFERRED_GRAPHICS_API.equals(entry.getName())) {
                    bytes = patchPreferredGraphicsApi(bytes, applied);
                    sawGraphicsApi = true;
                } else if (RENDER_SYSTEM.equals(entry.getName())) {
                    bytes = patchRenderSystem(bytes, applied);
                    sawRenderSystem = true;
                } else if (WINDOW.equals(entry.getName())) {
                    bytes = patchWindow(bytes, applied);
                    sawWindow = true;
                } else if (SDL_EVENT_HANDLER.equals(entry.getName())) {
                    bytes = patchSdlEventHandler(bytes, applied);
                    sawEventHandler = true;
                } else if (INPUT_CONSTANTS.equals(entry.getName())) {
                    bytes = patchInputConstants(bytes, applied);
                    sawInputConstants = true;
                }

                out.write(bytes);
                out.closeEntry();
            }
        } catch (IOException | RuntimeException | Error failure) {
            Files.deleteIfExists(output);
            throw failure;
        }

        if (
            !sawShaderManager ||
            !sawGraphicsApi ||
            !sawRenderSystem ||
            !sawWindow ||
            !sawEventHandler ||
            !sawInputConstants
        ) {
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

    private static byte[] patchSdlEventHandler(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode pollEvents = requireMethod(node, "pollEvents", "()V");
        pollEvents.instructions.clear();
        pollEvents.tryCatchBlocks.clear();
        if (pollEvents.localVariables != null) {
            pollEvents.localVariables.clear();
        }
        pollEvents.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        pollEvents.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/SDLEventHandler",
            "minecraft",
            "Lnet/minecraft/client/Minecraft;"
        ));
        pollEvents.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        pollEvents.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/SDLEventHandler",
            "window",
            "Lcom/mojang/blaze3d/platform/Window;"
        ));
        pollEvents.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputPump",
            "poll",
            "(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/platform/Window;)V",
            false
        ));
        pollEvents.instructions.add(new InsnNode(Opcodes.RETURN));
        pollEvents.maxStack = 2;
        pollEvents.maxLocals = 1;

        MethodNode flush = requireMethod(node, "flushInputEvents", "()V");
        flush.instructions.clear();
        flush.tryCatchBlocks.clear();
        if (flush.localVariables != null) {
            flush.localVariables.clear();
        }
        flush.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputPump",
            "clear",
            "()V",
            false
        ));
        flush.instructions.add(new InsnNode(Opcodes.RETURN));
        flush.maxStack = 0;
        flush.maxLocals = 1;

        applied.add("SDLEventHandler.pollEvents: drain Folium DOM input queue");
        applied.add("SDLEventHandler.flushInputEvents: clear Folium DOM input queue");

        return write(node);
    }

    private static byte[] patchInputConstants(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode isKeyDown = requireMethod(node, "isKeyDown", "(I)Z");
        isKeyDown.instructions.clear();
        isKeyDown.tryCatchBlocks.clear();
        if (isKeyDown.localVariables != null) {
            isKeyDown.localVariables.clear();
        }
        isKeyDown.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
        isKeyDown.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputBridge",
            "isKeyDown",
            "(I)Z",
            false
        ));
        isKeyDown.instructions.add(new InsnNode(Opcodes.IRETURN));
        isKeyDown.maxStack = 1;
        isKeyDown.maxLocals = 1;

        MethodNode grabMouse = requireMethod(
            node,
            "grabMouse",
            "(Lcom/mojang/blaze3d/platform/Window;DD)V"
        );
        grabMouse.instructions.clear();
        grabMouse.tryCatchBlocks.clear();
        if (grabMouse.localVariables != null) {
            grabMouse.localVariables.clear();
        }
        grabMouse.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputBridge",
            "grabMouse",
            "()V",
            false
        ));
        grabMouse.instructions.add(new InsnNode(Opcodes.RETURN));
        grabMouse.maxStack = 0;
        grabMouse.maxLocals = 5;

        MethodNode releaseMouse = requireMethod(
            node,
            "releaseMouse",
            "(Lcom/mojang/blaze3d/platform/Window;DD)V"
        );
        releaseMouse.instructions.clear();
        releaseMouse.tryCatchBlocks.clear();
        if (releaseMouse.localVariables != null) {
            releaseMouse.localVariables.clear();
        }
        releaseMouse.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputBridge",
            "releaseMouse",
            "()V",
            false
        ));
        releaseMouse.instructions.add(new InsnNode(Opcodes.RETURN));
        releaseMouse.maxStack = 0;
        releaseMouse.maxLocals = 5;

        applied.add("InputConstants.isKeyDown: browser key-state bridge");
        applied.add("InputConstants.grabMouse/releaseMouse: browser pointer lock");

        return write(node);
    }

    private static byte[] patchWindow(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode constructor = requireMethod(
            node,
            "<init>",
            "(Lcom/mojang/blaze3d/platform/WindowEventHandler;" +
                "Lcom/mojang/blaze3d/platform/DisplayData;" +
                "Ljava/lang/String;ZLjava/lang/String;" +
                "Lcom/mojang/blaze3d/platform/MonitorManager;" +
                "Lcom/mojang/renderpearl/api/device/GpuBackend;I)V"
        );

        constructor.instructions.clear();
        constructor.tryCatchBlocks.clear();
        if (constructor.localVariables != null) {
            constructor.localVariables.clear();
        }

        InsnList code = constructor.instructions;

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        ));

        putStringField(code, "errorSection", "Startup");

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "focused",
            "Z"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.GETSTATIC,
            "com/mojang/blaze3d/platform/cursor/CursorType",
            "DEFAULT",
            "Lcom/mojang/blaze3d/platform/cursor/CursorType;"
        ));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "currentCursor",
            "Lcom/mojang/blaze3d/platform/cursor/CursorType;"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 6));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "monitorManager",
            "Lcom/mojang/blaze3d/platform/MonitorManager;"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "eventHandler",
            "Lcom/mojang/blaze3d/platform/WindowEventHandler;"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/util/Optional",
            "empty",
            "()Ljava/util/Optional;",
            false
        ));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "preferredFullscreenVideoMode",
            "Ljava/util/Optional;"
        ));

        putBooleanField(code, "fullscreenRequested", false);
        putBooleanField(code, "fullscreen", false);
        putBooleanField(code, "exclusiveFullscreen", false);
        putBooleanField(code, "borderlessFullscreen", false);
        putBooleanField(code, "iconified", false);
        putBooleanField(code, "shouldClose", false);

        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "framebufferWidth",
            "()I",
            false
        ));
        code.add(new VarInsnNode(Opcodes.ISTORE, 9));

        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "framebufferHeight",
            "()I",
            false
        ));
        code.add(new VarInsnNode(Opcodes.ISTORE, 10));

        putIntFieldFromLocal(code, "width", 9);
        putIntFieldFromLocal(code, "windowedWidth", 9);
        putIntFieldFromLocal(code, "framebufferWidth", 9);
        putIntFieldFromLocal(code, "height", 10);
        putIntFieldFromLocal(code, "windowedHeight", 10);
        putIntFieldFromLocal(code, "framebufferHeight", 10);
        putIntField(code, "x", 0);
        putIntField(code, "y", 0);
        putIntField(code, "windowedX", 0);
        putIntField(code, "windowedY", 0);

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "windowHandle",
            "()J",
            false
        ));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "handle",
            "J"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 5));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "setTitle",
            "(Ljava/lang/String;)V",
            false
        ));

        code.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 3;
        constructor.maxLocals = 11;

        replaceStaticStringMethod(
            node,
            "getPlatform",
            "()Ljava/lang/String;",
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "platformName"
        );

        MethodNode queryFramebuffer = requireMethod(
            node,
            "queryFramebufferSize",
            "()Lcom/mojang/blaze3d/platform/Window$FramebufferSize;"
        );
        replaceWithStaticReturn(
            queryFramebuffer,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "queryFramebufferSize",
            "()Lcom/mojang/blaze3d/platform/Window$FramebufferSize;",
            Opcodes.ARETURN
        );

        MethodNode refreshFramebuffer = requireMethod(
            node,
            "refreshFramebufferSize",
            "()V"
        );
        refreshFramebuffer.instructions.clear();
        refreshFramebuffer.tryCatchBlocks.clear();
        refreshFramebuffer.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        refreshFramebuffer.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "framebufferWidth",
            "()I",
            false
        ));
        refreshFramebuffer.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "framebufferWidth",
            "I"
        ));
        refreshFramebuffer.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        refreshFramebuffer.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "framebufferHeight",
            "()I",
            false
        ));
        refreshFramebuffer.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "framebufferHeight",
            "I"
        ));
        refreshFramebuffer.instructions.add(new InsnNode(Opcodes.RETURN));
        refreshFramebuffer.maxStack = 2;
        refreshFramebuffer.maxLocals = 1;

        MethodNode setTitle = requireMethod(
            node,
            "setTitle",
            "(Ljava/lang/String;)V"
        );
        setTitle.instructions.clear();
        setTitle.tryCatchBlocks.clear();
        setTitle.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        setTitle.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumWindowBootstrap",
            "setTitle",
            "(Ljava/lang/String;)V",
            false
        ));
        setTitle.instructions.add(new InsnNode(Opcodes.RETURN));
        setTitle.maxStack = 1;
        setTitle.maxLocals = 2;

        replaceWithReturnVoid(node, "setWindowMaxSize", "(II)V");
        replaceWithReturnVoid(node, "close", "()V");
        replaceWithReturnVoid(
            node,
            "setIcon",
            "(Lnet/minecraft/server/packs/PackMetadataResources;" +
                "Lcom/mojang/blaze3d/platform/IconSet;)V"
        );
        replaceWithReturnVoid(node, "updateFullscreenIfChanged", "()V");
        replaceWithReturnVoid(node, "changeFullscreenVideoMode", "()V");
        replaceWithReturnVoid(
            node,
            "selectCursor",
            "(Lcom/mojang/blaze3d/platform/cursor/CursorType;)V"
        );

        MethodNode setFullscreen = requireMethod(node, "setFullscreen", "(Z)V");
        setFullscreen.instructions.clear();
        setFullscreen.tryCatchBlocks.clear();
        setFullscreen.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setFullscreen.instructions.add(new InsnNode(Opcodes.ICONST_0));
        setFullscreen.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "fullscreenRequested",
            "Z"
        ));
        setFullscreen.instructions.add(new InsnNode(Opcodes.RETURN));
        setFullscreen.maxStack = 2;
        setFullscreen.maxLocals = 2;

        applied.add("Window.<init>: replace SDL window construction with browser canvas state");
        applied.add("Window.getPlatform/queryFramebufferSize/setTitle: browser host bridge");
        applied.add("Window.refreshFramebufferSize: browser canvas dimensions");
        applied.add("Window.close/setWindowMaxSize/setIcon: remove SDL calls");
        applied.add("Window fullscreen/cursor startup paths: browser-safe no-op");

        return write(node);
    }

    private static void putStringField(
        InsnList code,
        String field,
        String value
    ) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(value));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            field,
            "Ljava/lang/String;"
        ));
    }

    private static void putBooleanField(
        InsnList code,
        String field,
        boolean value
    ) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            field,
            "Z"
        ));
    }

    private static void putIntField(
        InsnList code,
        String field,
        int value
    ) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (value == 0) {
            code.add(new InsnNode(Opcodes.ICONST_0));
        } else {
            code.add(new org.objectweb.asm.tree.LdcInsnNode(value));
        }
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            field,
            "I"
        ));
    }

    private static void putIntFieldFromLocal(
        InsnList code,
        String field,
        int local
    ) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ILOAD, local));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            field,
            "I"
        ));
    }

    private static void replaceStaticStringMethod(
        ClassNode node,
        String name,
        String descriptor,
        String owner,
        String target
    ) {
        MethodNode method = requireMethod(node, name, descriptor);
        replaceWithStaticReturn(
            method,
            owner,
            target,
            "()Ljava/lang/String;",
            Opcodes.ARETURN
        );
    }

    private static void replaceWithStaticReturn(
        MethodNode method,
        String owner,
        String name,
        String descriptor,
        int returnOpcode
    ) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }
        method.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            owner,
            name,
            descriptor,
            false
        ));
        method.instructions.add(new InsnNode(returnOpcode));
        method.maxStack = 2;
    }

    private static void replaceWithReturnVoid(
        ClassNode node,
        String name,
        String descriptor
    ) {
        MethodNode method = requireMethod(node, name, descriptor);
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 0;
    }

    private static byte[] patchRenderSystem(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode method = requireMethod(
            node,
            "initBackendSystem",
            "()Lnet/minecraft/util/TimeSource$NanoTimeSource;"
        );

        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }

        method.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumBackendBootstrap",
            "initBackendSystem",
            "()Lnet/minecraft/util/TimeSource$NanoTimeSource;",
            false
        ));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 1;
        method.maxLocals = 0;

        applied.add(
            "RenderSystem.initBackendSystem: replace SDL bootstrap with Folium clock"
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

        return matches.get(0);
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException(
            "Minecraft 26.3 patch target drift: " + message
        );
    }

    private static boolean isSignatureEntry(String name) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);

        if (!upper.startsWith("META-INF/")) {
            return false;
        }

        return upper.endsWith(".SF") ||
            upper.endsWith(".RSA") ||
            upper.endsWith(".DSA") ||
            upper.endsWith(".EC");
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
