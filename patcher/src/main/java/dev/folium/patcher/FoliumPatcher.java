package dev.folium.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

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

    private static final String NATIVE_LIBRARIES =
        "com/mojang/blaze3d/platform/NativeLibrariesBootstrap.class";

    private static final String WINDOW =
        "com/mojang/blaze3d/platform/Window.class";

    private static final String MONITOR_MANAGER =
        "com/mojang/blaze3d/platform/MonitorManager.class";

    private static final String SDL_EVENT_HANDLER =
        "com/mojang/blaze3d/platform/SDLEventHandler.class";

    private static final String TEXT_INPUT_MANAGER =
        "com/mojang/blaze3d/platform/TextInputManager.class";

    private static final String MINECRAFT =
        "net/minecraft/client/Minecraft.class";

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
                } else if (NATIVE_LIBRARIES.equals(entryName)) {
                    data = patchNativeLibrariesBootstrap(data);
                    patched.add(NATIVE_LIBRARIES);
                } else if (WINDOW.equals(entryName)) {
                    data = patchWindow(data);
                    patched.add(WINDOW);
                } else if (MONITOR_MANAGER.equals(entryName)) {
                    data = patchMonitorManager(data);
                    patched.add(MONITOR_MANAGER);
                } else if (SDL_EVENT_HANDLER.equals(entryName)) {
                    data = patchSdlEventHandler(data);
                    patched.add(SDL_EVENT_HANDLER);
                } else if (TEXT_INPUT_MANAGER.equals(entryName)) {
                    data = patchTextInputManager(data);
                    patched.add(TEXT_INPUT_MANAGER);
                } else if (MINECRAFT.equals(entryName)) {
                    data = patchMinecraft(data);
                    patched.add(MINECRAFT);
                }

                zout.write(data);
                zout.closeEntry();
            }
        } catch (Throwable failure) {
            Files.deleteIfExists(output);
            throw failure;
        }

        if (
            !patched.contains(PREFERRED_GRAPHICS)
                || !patched.contains(RENDER_SYSTEM)
                || !patched.contains(NATIVE_LIBRARIES)
                || !patched.contains(WINDOW)
                || !patched.contains(MONITOR_MANAGER)
                || !patched.contains(SDL_EVENT_HANDLER)
                || !patched.contains(TEXT_INPUT_MANAGER)
                || !patched.contains(MINECRAFT)
        ) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                "Expected Minecraft 26.3 classes were not found; refusing to emit a partial patch"
            );
        }

        System.out.println("Folium patched: " + input);
        System.out.println("Output: " + output);
        System.out.println("  ✓ PreferredGraphicsApi.getBackendsToTry");
        System.out.println("  ✓ RenderSystem.initBackendSystem");
        System.out.println("  ✓ NativeLibrariesBootstrap.loadLibraries");
        System.out.println("  ✓ NativeLibrariesBootstrap.isVulkanLoaderAvailable");
        System.out.println("  ✓ Minecraft Vulkan preflight");
        System.out.println("  ✓ MonitorManager browser fallback");
        System.out.println("  ✓ Window browser shell");
        System.out.println("  ✓ SDLEventHandler polling bypass");
        System.out.println("  ✓ TextInputManager SDL bypass");

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

    private static byte[] patchMinecraft(byte[] original) {
        ClassNode node = read(original);
        int replacements = 0;

        for (MethodNode method : node.methods) {
            for (var instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESTATIC
                    && call.owner.equals("com/mojang/renderpearl/backend/vulkan/VulkanBackend")
                    && call.name.equals("checkBackendAvailable")
                    && call.desc.equals("()Lcom/mojang/renderpearl/api/device/BackendCreationException;")) {
                    method.instructions.set(call, new InsnNode(Opcodes.ACONST_NULL));
                    replacements++;
                }
            }
        }

        if (replacements == 0) {
            throw new IllegalStateException(
                "Minecraft Vulkan preflight call was not found"
            );
        }

        return write(node);
    }

    private static byte[] patchMonitorManager(byte[] original) {
        ClassNode node = read(original);

        MethodNode constructor = requireMethod(node, "<init>", "()V");
        clearMethod(constructor);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        ));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new TypeInsnNode(
            Opcodes.NEW,
            "it/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap"
        ));
        constructor.instructions.add(new InsnNode(Opcodes.DUP));
        constructor.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "it/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap",
            "<init>",
            "()V",
            false
        ));
        constructor.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/MonitorManager",
            "monitors",
            "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"
        ));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 3;
        constructor.maxLocals = 1;

        MethodNode findBestMonitor = requireMethod(
            node,
            "findBestMonitor",
            "(Lcom/mojang/blaze3d/platform/Window;)Lcom/mojang/blaze3d/platform/Monitor;"
        );
        clearMethod(findBestMonitor);
        findBestMonitor.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        findBestMonitor.instructions.add(new InsnNode(Opcodes.ARETURN));
        findBestMonitor.maxStack = 1;
        findBestMonitor.maxLocals = 2;

        return write(node);
    }

    private static byte[] patchSdlEventHandler(byte[] original) {
        ClassNode node = read(original);

        for (String methodName : new String[] {
            "pollEvents",
            "pumpEvents",
            "flushInputEvents"
        }) {
            MethodNode method = requireMethod(node, methodName, "()V");
            clearMethod(method);
            method.instructions.add(new InsnNode(Opcodes.RETURN));
            method.maxStack = 0;
            method.maxLocals = 1;
        }

        return write(node);
    }

    private static byte[] patchTextInputManager(byte[] original) {
        ClassNode node = read(original);

        MethodNode applyArea = requireMethod(node, "applyTextInputArea", "()V");
        clearMethod(applyArea);
        applyArea.instructions.add(new InsnNode(Opcodes.RETURN));
        applyArea.maxStack = 0;
        applyArea.maxLocals = 1;

        MethodNode start = requireMethod(
            node,
            "startTextInput",
            "(Ljava/lang/Object;)V"
        );
        clearMethod(start);
        start.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        start.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        start.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "owner",
            "Ljava/lang/Object;"
        ));
        start.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        start.instructions.add(new InsnNode(Opcodes.ICONST_1));
        start.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "textInputEnabled",
            "Z"
        ));
        start.instructions.add(new InsnNode(Opcodes.RETURN));
        start.maxStack = 2;
        start.maxLocals = 2;

        MethodNode stop = requireMethod(node, "stopTextInput", "()V");
        clearMethod(stop);
        stop.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stop.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        stop.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "owner",
            "Ljava/lang/Object;"
        ));
        stop.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stop.instructions.add(new InsnNode(Opcodes.ICONST_0));
        stop.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "textInputEnabled",
            "Z"
        ));
        stop.instructions.add(new InsnNode(Opcodes.RETURN));
        stop.maxStack = 2;
        stop.maxLocals = 1;

        return write(node);
    }

    private static byte[] patchWindow(byte[] original) {
        ClassNode node = read(original);

        patchWindowConstructor(node);
        patchWindowSimpleMethods(node);

        return write(node);
    }

    private static void patchWindowConstructor(ClassNode node) {
        String descriptor =
            "(Lcom/mojang/blaze3d/platform/WindowEventHandler;"
                + "Lcom/mojang/blaze3d/platform/DisplayData;"
                + "Ljava/lang/String;"
                + "Z"
                + "Ljava/lang/String;"
                + "Lcom/mojang/blaze3d/platform/MonitorManager;"
                + "Lcom/mojang/renderpearl/api/device/GpuBackend;"
                + "I)V";

        MethodNode constructor = requireMethod(node, "<init>", descriptor);
        clearMethod(constructor);
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
        putBooleanField(code, "focused", true);

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
            Opcodes.GETSTATIC,
            "com/mojang/blaze3d/platform/cursor/CursorType",
            "DEFAULT",
            "Lcom/mojang/blaze3d/platform/cursor/CursorType;"
        ));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "currentCursor",
            "Lcom/mojang/blaze3d/platform/cursor/CursorType;"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 6));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "monitorManager",
            "Lcom/mojang/blaze3d/platform/MonitorManager;"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "eventHandler",
            "Lcom/mojang/blaze3d/platform/WindowEventHandler;"
        ));

        putBooleanField(code, "exclusiveFullscreen", false);
        putBooleanField(code, "fullscreenRequested", false);
        putBooleanField(code, "fullscreen", false);

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/util/Optional",
            "empty",
            "()Ljava/util/Optional;",
            false
        ));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "preferredFullscreenVideoMode",
            "Ljava/util/Optional;"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/mojang/blaze3d/platform/DisplayData",
            "width",
            "()I",
            false
        ));
        code.add(new IntInsnNode(Opcodes.SIPUSH, 320));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/Math",
            "max",
            "(II)I",
            false
        ));
        code.add(new VarInsnNode(Opcodes.ISTORE, 9));

        code.add(new VarInsnNode(Opcodes.ALOAD, 2));
        code.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/mojang/blaze3d/platform/DisplayData",
            "height",
            "()I",
            false
        ));
        code.add(new IntInsnNode(Opcodes.SIPUSH, 240));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/lang/Math",
            "max",
            "(II)I",
            false
        ));
        code.add(new VarInsnNode(Opcodes.ISTORE, 10));

        for (String field : new String[] {"width", "windowedWidth", "framebufferWidth", "guiScaledWidth"}) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new VarInsnNode(Opcodes.ILOAD, 9));
            code.add(new FieldInsnNode(
                Opcodes.PUTFIELD,
                "com/mojang/blaze3d/platform/Window",
                field,
                "I"
            ));
        }

        for (String field : new String[] {"height", "windowedHeight", "framebufferHeight", "guiScaledHeight"}) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new VarInsnNode(Opcodes.ILOAD, 10));
            code.add(new FieldInsnNode(
                Opcodes.PUTFIELD,
                "com/mojang/blaze3d/platform/Window",
                field,
                "I"
            ));
        }

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(Opcodes.ICONST_1));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "guiScale",
            "I"
        ));

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 7));
        code.add(new VarInsnNode(Opcodes.ALOAD, 5));
        code.add(new VarInsnNode(Opcodes.ILOAD, 9));
        code.add(new VarInsnNode(Opcodes.ILOAD, 10));
        code.add(new InsnNode(Opcodes.LCONST_0));
        code.add(new MethodInsnNode(
            Opcodes.INVOKEINTERFACE,
            "com/mojang/renderpearl/api/device/GpuBackend",
            "createWindow",
            "(Ljava/lang/String;IIJ)J",
            true
        ));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            "handle",
            "J"
        ));

        code.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 7;
        constructor.maxLocals = 11;
    }

    private static void patchWindowSimpleMethods(ClassNode node) {
        replaceWithString(node, "getPlatform", "()Ljava/lang/String;", "Web");
        replaceWithVoid(
            node,
            "setIcon",
            "(Lnet/minecraft/server/packs/PackMetadataResources;Lcom/mojang/blaze3d/platform/IconSet;)V",
            3
        );
        replaceWithVoid(node, "setTitle", "(Ljava/lang/String;)V", 2);
        replaceWithVoid(node, "setWindowMaxSize", "(II)V", 3);
        replaceWithFloat(node, "getPixelDensity", "()F", 1.0F);
        replaceWithNull(
            node,
            "findBestMonitor",
            "()Lcom/mojang/blaze3d/platform/Monitor;",
            1
        );

        MethodNode close = requireMethod(node, "close", "()V");
        clearMethod(close);
        putBooleanField(close.instructions, "shouldClose", true);
        close.instructions.add(new InsnNode(Opcodes.RETURN));
        close.maxStack = 2;
        close.maxLocals = 1;

        MethodNode refresh = requireMethod(node, "refreshFramebufferSize", "()V");
        clearMethod(refresh);
        copyIntField(refresh.instructions, "width", "framebufferWidth");
        copyIntField(refresh.instructions, "height", "framebufferHeight");
        refresh.instructions.add(new InsnNode(Opcodes.RETURN));
        refresh.maxStack = 2;
        refresh.maxLocals = 1;

        MethodNode query = requireMethod(
            node,
            "queryFramebufferSize",
            "()Lcom/mojang/blaze3d/platform/Window$FramebufferSize;"
        );
        clearMethod(query);
        query.instructions.add(new TypeInsnNode(
            Opcodes.NEW,
            "com/mojang/blaze3d/platform/Window$FramebufferSize"
        ));
        query.instructions.add(new InsnNode(Opcodes.DUP));
        query.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        query.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/Window",
            "framebufferWidth",
            "I"
        ));
        query.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        query.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/Window",
            "framebufferHeight",
            "I"
        ));
        query.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "com/mojang/blaze3d/platform/Window$FramebufferSize",
            "<init>",
            "(II)V",
            false
        ));
        query.instructions.add(new InsnNode(Opcodes.ARETURN));
        query.maxStack = 4;
        query.maxLocals = 1;

        MethodNode setMode = requireMethod(node, "setMode", "()V");
        clearMethod(setMode);
        putBooleanField(setMode.instructions, "fullscreen", false);
        putBooleanField(setMode.instructions, "dirty", false);
        setMode.instructions.add(new InsnNode(Opcodes.RETURN));
        setMode.maxStack = 2;
        setMode.maxLocals = 1;

        replaceWithVoid(node, "updateWindowMouseGrab", "()V", 1);
        replaceWithVoid(node, "restoreWindow", "()V", 1);
        replaceWithVoid(node, "syncWindow", "()V", 1);

        MethodNode getActiveVideoMode = requireMethod(
            node,
            "getActiveVideoMode",
            "()Lcom/mojang/blaze3d/platform/VideoMode;"
        );
        clearMethod(getActiveVideoMode);
        getActiveVideoMode.instructions.add(new TypeInsnNode(
            Opcodes.NEW,
            "com/mojang/blaze3d/platform/VideoMode"
        ));
        getActiveVideoMode.instructions.add(new InsnNode(Opcodes.DUP));
        getActiveVideoMode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        getActiveVideoMode.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/Window",
            "width",
            "I"
        ));
        getActiveVideoMode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        getActiveVideoMode.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/Window",
            "height",
            "I"
        ));
        for (int value : new int[] {8, 8, 8, 60}) {
            getActiveVideoMode.instructions.add(new IntInsnNode(Opcodes.BIPUSH, value));
        }
        getActiveVideoMode.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "com/mojang/blaze3d/platform/VideoMode",
            "<init>",
            "(IIIIII)V",
            false
        ));
        getActiveVideoMode.instructions.add(new InsnNode(Opcodes.ARETURN));
        getActiveVideoMode.maxStack = 8;
        getActiveVideoMode.maxLocals = 1;
    }

    private static void putStringField(InsnList code, String field, String value) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new LdcInsnNode(value));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            field,
            "Ljava/lang/String;"
        ));
    }

    private static void putBooleanField(InsnList code, String field, boolean value) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            field,
            "Z"
        ));
    }

    private static void copyIntField(InsnList code, String from, String to) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/Window",
            from,
            "I"
        ));
        code.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/Window",
            to,
            "I"
        ));
    }

    private static void replaceWithVoid(
        ClassNode node,
        String name,
        String descriptor,
        int maxLocals
    ) {
        MethodNode method = requireMethod(node, name, descriptor);
        clearMethod(method);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 0;
        method.maxLocals = maxLocals;
    }

    private static void replaceWithString(
        ClassNode node,
        String name,
        String descriptor,
        String value
    ) {
        MethodNode method = requireMethod(node, name, descriptor);
        clearMethod(method);
        method.instructions.add(new LdcInsnNode(value));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 1;
        method.maxLocals = 0;
    }

    private static void replaceWithFloat(
        ClassNode node,
        String name,
        String descriptor,
        float value
    ) {
        MethodNode method = requireMethod(node, name, descriptor);
        clearMethod(method);
        method.instructions.add(new LdcInsnNode(value));
        method.instructions.add(new InsnNode(Opcodes.FRETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
    }

    private static void replaceWithNull(
        ClassNode node,
        String name,
        String descriptor,
        int maxLocals
    ) {
        MethodNode method = requireMethod(node, name, descriptor);
        clearMethod(method);
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 1;
        method.maxLocals = maxLocals;
    }

    private static byte[] patchNativeLibrariesBootstrap(byte[] original) {
        ClassNode node = read(original);

        MethodNode loadLibraries = requireMethod(
            node,
            "loadLibraries",
            "()V"
        );
        clearMethod(loadLibraries);
        loadLibraries.instructions.add(new InsnNode(Opcodes.RETURN));
        loadLibraries.maxStack = 0;
        loadLibraries.maxLocals = 0;

        MethodNode vulkanAvailable = requireMethod(
            node,
            "isVulkanLoaderAvailable",
            "()Z"
        );
        clearMethod(vulkanAvailable);
        vulkanAvailable.instructions.add(new InsnNode(Opcodes.ICONST_0));
        vulkanAvailable.instructions.add(new InsnNode(Opcodes.IRETURN));
        vulkanAvailable.maxStack = 1;
        vulkanAvailable.maxLocals = 0;

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
