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
    private static final String TEXT_INPUT_MANAGER =
        "com/mojang/blaze3d/platform/TextInputManager.class";
    private static final String CLIPBOARD_MANAGER =
        "com/mojang/blaze3d/platform/ClipboardManager.class";
    private static final String MONITOR =
        "com/mojang/blaze3d/platform/Monitor.class";
    private static final String MONITOR_MANAGER =
        "com/mojang/blaze3d/platform/MonitorManager.class";
    private static final String VIDEO_MODE =
        "com/mojang/blaze3d/platform/VideoMode.class";
    private static final String NATIVE_LIBRARIES_BOOTSTRAP =
        "com/mojang/blaze3d/platform/NativeLibrariesBootstrap.class";
    private static final String BLAZE3D =
        "com/mojang/blaze3d/Blaze3D.class";
    private static final String CURSOR_TYPE =
        "com/mojang/blaze3d/platform/cursor/CursorType.class";
    private static final String INPUT_QUIRKS =
        "net/minecraft/client/input/InputQuirks.class";
    private static final String MOUSE_HANDLER =
        "net/minecraft/client/MouseHandler.class";
    private static final String MESSAGE_BOX =
        "com/mojang/blaze3d/platform/MessageBox.class";
    private static final String MACOS_UTIL =
        "com/mojang/blaze3d/platform/MacosUtil.class";
    private static final String CONNECTION =
        "net/minecraft/network/Connection.class";

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
        boolean sawTextInputManager = false;
        boolean sawClipboardManager = false;
        boolean sawMonitor = false;
        boolean sawMonitorManager = false;
        boolean sawVideoMode = false;
        boolean sawNativeLibrariesBootstrap = false;
        boolean sawBlaze3D = false;
        boolean sawCursorType = false;
        boolean sawInputQuirks = false;
        boolean sawMouseHandler = false;
        boolean sawMessageBox = false;
        boolean sawMacosUtil = false;
        boolean sawConnection = false;

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
                } else if (TEXT_INPUT_MANAGER.equals(entry.getName())) {
                    bytes = patchTextInputManager(bytes, applied);
                    sawTextInputManager = true;
                } else if (CLIPBOARD_MANAGER.equals(entry.getName())) {
                    bytes = patchClipboardManager(bytes, applied);
                    sawClipboardManager = true;
                } else if (MONITOR.equals(entry.getName())) {
                    bytes = patchMonitor(bytes, applied);
                    sawMonitor = true;
                } else if (MONITOR_MANAGER.equals(entry.getName())) {
                    bytes = patchMonitorManager(bytes, applied);
                    sawMonitorManager = true;
                } else if (VIDEO_MODE.equals(entry.getName())) {
                    bytes = patchVideoMode(bytes, applied);
                    sawVideoMode = true;
                } else if (NATIVE_LIBRARIES_BOOTSTRAP.equals(entry.getName())) {
                    bytes = patchNativeLibrariesBootstrap(bytes, applied);
                    sawNativeLibrariesBootstrap = true;
                } else if (BLAZE3D.equals(entry.getName())) {
                    bytes = patchBlaze3D(bytes, applied);
                    sawBlaze3D = true;
                } else if (CURSOR_TYPE.equals(entry.getName())) {
                    bytes = patchCursorType(bytes, applied);
                    sawCursorType = true;
                } else if (INPUT_QUIRKS.equals(entry.getName())) {
                    bytes = patchInputQuirks(bytes, applied);
                    sawInputQuirks = true;
                } else if (MOUSE_HANDLER.equals(entry.getName())) {
                    bytes = patchMouseHandler(bytes, applied);
                    sawMouseHandler = true;
                } else if (MESSAGE_BOX.equals(entry.getName())) {
                    bytes = patchMessageBox(bytes, applied);
                    sawMessageBox = true;
                } else if (MACOS_UTIL.equals(entry.getName())) {
                    bytes = patchMacosUtil(bytes, applied);
                    sawMacosUtil = true;
                } else if (CONNECTION.equals(entry.getName())) {
                    bytes = patchConnection(bytes, applied);
                    sawConnection = true;
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
            !sawInputConstants ||
            !sawTextInputManager ||
            !sawClipboardManager ||
            !sawMonitor ||
            !sawMonitorManager ||
            !sawVideoMode ||
            !sawNativeLibrariesBootstrap ||
            !sawBlaze3D ||
            !sawCursorType ||
            !sawInputQuirks ||
            !sawMouseHandler ||
            !sawMessageBox ||
            !sawMacosUtil ||
            !sawConnection
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

    private static byte[] patchConnection(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode connectToServer = requireMethod(
            node,
            "connectToServer",
            "(Ljava/net/InetSocketAddress;" +
                "Lnet/minecraft/server/network/EventLoopGroupHolder;" +
                "Lnet/minecraft/util/debugchart/LocalSampleLogger;)" +
                "Lnet/minecraft/network/Connection;"
        );
        connectToServer.instructions.clear();
        connectToServer.tryCatchBlocks.clear();
        if (connectToServer.localVariables != null) {
            connectToServer.localVariables.clear();
        }

        InsnList connect = connectToServer.instructions;
        connect.add(new TypeInsnNode(
            Opcodes.NEW,
            "net/minecraft/network/Connection"
        ));
        connect.add(new InsnNode(Opcodes.DUP));
        connect.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.GETSTATIC,
            "net/minecraft/network/protocol/PacketFlow",
            "CLIENTBOUND",
            "Lnet/minecraft/network/protocol/PacketFlow;"
        ));
        connect.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "net/minecraft/network/Connection",
            "<init>",
            "(Lnet/minecraft/network/protocol/PacketFlow;)V",
            false
        ));
        connect.add(new VarInsnNode(Opcodes.ASTORE, 3));

        connect.add(new VarInsnNode(Opcodes.ALOAD, 3));
        connect.add(new VarInsnNode(Opcodes.ALOAD, 0));
        connect.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/network/Connection",
            "address",
            "Ljava/net/SocketAddress;"
        ));

        connect.add(new VarInsnNode(Opcodes.ALOAD, 3));
        connect.add(new VarInsnNode(Opcodes.ALOAD, 0));
        connect.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "connect",
            "(Lnet/minecraft/network/Connection;" +
                "Ljava/net/InetSocketAddress;)V",
            false
        ));

        connect.add(new VarInsnNode(Opcodes.ALOAD, 3));
        connect.add(new InsnNode(Opcodes.ARETURN));
        connectToServer.maxStack = 3;
        connectToServer.maxLocals = 4;

        MethodNode setupInbound = requireMethod(
            node,
            "setupInboundProtocol",
            "(Lnet/minecraft/network/ProtocolInfo;" +
                "Lnet/minecraft/network/PacketListener;)V"
        );
        setupInbound.instructions.clear();
        setupInbound.tryCatchBlocks.clear();
        if (setupInbound.localVariables != null) {
            setupInbound.localVariables.clear();
        }
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        setupInbound.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "net/minecraft/network/Connection",
            "validateListener",
            "(Lnet/minecraft/network/ProtocolInfo;" +
                "Lnet/minecraft/network/PacketListener;)V",
            false
        ));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        setupInbound.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/network/Connection",
            "packetListener",
            "Lnet/minecraft/network/PacketListener;"
        ));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setupInbound.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        setupInbound.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/network/Connection",
            "disconnectListener",
            "Lnet/minecraft/network/PacketListener;"
        ));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setupInbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        setupInbound.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "setInboundProtocol",
            "(Lnet/minecraft/network/Connection;" +
                "Lnet/minecraft/network/ProtocolInfo;)V",
            false
        ));
        setupInbound.instructions.add(new InsnNode(Opcodes.RETURN));
        setupInbound.maxStack = 3;
        setupInbound.maxLocals = 3;

        MethodNode setupOutbound = requireMethod(
            node,
            "setupOutboundProtocol",
            "(Lnet/minecraft/network/ProtocolInfo;)V"
        );
        setupOutbound.instructions.clear();
        setupOutbound.tryCatchBlocks.clear();
        if (setupOutbound.localVariables != null) {
            setupOutbound.localVariables.clear();
        }
        setupOutbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        setupOutbound.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        setupOutbound.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "setOutboundProtocol",
            "(Lnet/minecraft/network/Connection;" +
                "Lnet/minecraft/network/ProtocolInfo;)V",
            false
        ));
        setupOutbound.instructions.add(new InsnNode(Opcodes.RETURN));
        setupOutbound.maxStack = 2;
        setupOutbound.maxLocals = 2;

        patchConnectionSend(node, "send",
            "(Lnet/minecraft/network/protocol/Packet;)V");
        patchConnectionSend(node, "sendPacket",
            "(Lnet/minecraft/network/protocol/Packet;" +
                "Lio/netty/channel/ChannelFutureListener;Z)V");

        MethodNode sendFull = requireMethod(
            node,
            "send",
            "(Lnet/minecraft/network/protocol/Packet;" +
                "Lio/netty/channel/ChannelFutureListener;Z)V"
        );
        sendFull.instructions.clear();
        sendFull.tryCatchBlocks.clear();
        if (sendFull.localVariables != null) {
            sendFull.localVariables.clear();
        }
        sendFull.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        sendFull.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        sendFull.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "send",
            "(Lnet/minecraft/network/Connection;" +
                "Lnet/minecraft/network/protocol/Packet;)V",
            false
        ));
        sendFull.instructions.add(new InsnNode(Opcodes.RETURN));
        sendFull.maxStack = 2;
        sendFull.maxLocals = 4;

        MethodNode sendListener = requireMethod(
            node,
            "send",
            "(Lnet/minecraft/network/protocol/Packet;" +
                "Lio/netty/channel/ChannelFutureListener;)V"
        );
        sendListener.instructions.clear();
        sendListener.tryCatchBlocks.clear();
        if (sendListener.localVariables != null) {
            sendListener.localVariables.clear();
        }
        sendListener.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        sendListener.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        sendListener.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "send",
            "(Lnet/minecraft/network/Connection;" +
                "Lnet/minecraft/network/protocol/Packet;)V",
            false
        ));
        sendListener.instructions.add(new InsnNode(Opcodes.RETURN));
        sendListener.maxStack = 2;
        sendListener.maxLocals = 3;

        MethodNode runOnce = requireMethod(
            node,
            "runOnceConnected",
            "(Ljava/util/function/Consumer;)V"
        );
        runOnce.instructions.clear();
        runOnce.tryCatchBlocks.clear();
        if (runOnce.localVariables != null) {
            runOnce.localVariables.clear();
        }
        runOnce.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        runOnce.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        runOnce.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEINTERFACE,
            "java/util/function/Consumer",
            "accept",
            "(Ljava/lang/Object;)V",
            true
        ));
        runOnce.instructions.add(new InsnNode(Opcodes.RETURN));
        runOnce.maxStack = 2;
        runOnce.maxLocals = 2;

        MethodNode tick = requireMethod(node, "tick", "()V");
        tick.instructions.clear();
        tick.tryCatchBlocks.clear();
        if (tick.localVariables != null) {
            tick.localVariables.clear();
        }
        tick.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        tick.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "tick",
            "(Lnet/minecraft/network/Connection;)V",
            false
        ));
        tick.instructions.add(new InsnNode(Opcodes.RETURN));
        tick.maxStack = 1;
        tick.maxLocals = 1;

        MethodNode connected = requireMethod(node, "isConnected", "()Z");
        replaceConnectionBoolean(
            connected,
            "isConnected"
        );

        MethodNode connecting = requireMethod(node, "isConnecting", "()Z");
        replaceConnectionBoolean(
            connecting,
            "isConnecting"
        );

        MethodNode disconnect = requireMethod(
            node,
            "disconnect",
            "(Lnet/minecraft/network/DisconnectionDetails;)V"
        );
        disconnect.instructions.clear();
        disconnect.tryCatchBlocks.clear();
        if (disconnect.localVariables != null) {
            disconnect.localVariables.clear();
        }
        disconnect.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        disconnect.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        disconnect.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/network/Connection",
            "disconnectionDetails",
            "Lnet/minecraft/network/DisconnectionDetails;"
        ));
        disconnect.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        disconnect.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        disconnect.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "disconnect",
            "(Lnet/minecraft/network/Connection;" +
                "Lnet/minecraft/network/DisconnectionDetails;)V",
            false
        ));
        disconnect.instructions.add(new InsnNode(Opcodes.RETURN));
        disconnect.maxStack = 2;
        disconnect.maxLocals = 2;

        replaceWithReturnVoid(node, "flushChannel", "()V");
        replaceWithReturnVoid(node, "setReadOnly", "()V");

        MethodNode memory = requireMethod(node, "isMemoryConnection", "()Z");
        memory.instructions.clear();
        memory.tryCatchBlocks.clear();
        memory.instructions.add(new InsnNode(Opcodes.ICONST_0));
        memory.instructions.add(new InsnNode(Opcodes.IRETURN));
        memory.maxStack = 1;
        memory.maxLocals = 1;

        applied.add("Connection.connectToServer: open Folium WebSocket session");
        applied.add("Connection protocol setup: replace Netty pipeline mutation");
        applied.add("Connection send/tick/disconnect: FoliumNetworkSession bridge");
        applied.add("Connection status/flush/read-only: browser transport semantics");

        return write(node);
    }

    private static void patchConnectionSend(
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
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            "send",
            "(Lnet/minecraft/network/Connection;" +
                "Lnet/minecraft/network/protocol/Packet;)V",
            false
        ));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
    }

    private static void replaceConnectionBoolean(
        MethodNode method,
        String bridgeMethod
    ) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumConnectionBridge",
            bridgeMethod,
            "(Lnet/minecraft/network/Connection;)Z",
            false
        ));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
    }

    private static byte[] patchMessageBox(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode error = requireMethod(
            node,
            "error",
            "(Ljava/lang/String;)V"
        );
        error.instructions.clear();
        error.tryCatchBlocks.clear();
        if (error.localVariables != null) {
            error.localVariables.clear();
        }
        error.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        error.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumUtilityBridge",
            "error",
            "(Ljava/lang/String;)V",
            false
        ));
        error.instructions.add(new InsnNode(Opcodes.RETURN));
        error.maxStack = 1;
        error.maxLocals = 1;

        MethodNode cont = requireMethod(
            node,
            "errorWithContinue",
            "(Ljava/lang/String;)Z"
        );
        cont.instructions.clear();
        cont.tryCatchBlocks.clear();
        if (cont.localVariables != null) {
            cont.localVariables.clear();
        }
        cont.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        cont.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumUtilityBridge",
            "errorWithContinue",
            "(Ljava/lang/String;)Z",
            false
        ));
        cont.instructions.add(new InsnNode(Opcodes.IRETURN));
        cont.maxStack = 1;
        cont.maxLocals = 1;

        applied.add("MessageBox: replace SDL message boxes with browser dialogs");
        return write(node);
    }

    private static byte[] patchMacosUtil(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        replaceWithReturnVoid(node, "disableCloseWindowMenuItem", "()V");
        replaceWithReturnVoid(node, "setFullscreenMenuVisibility", "(Z)V");
        replaceWithReturnVoid(node, "setCtrlClickEmulatesRightClick", "(Z)V");

        applied.add("MacosUtil: remove SDL/macOS native menu hints");
        return write(node);
    }

    private static byte[] patchNativeLibrariesBootstrap(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        replaceWithReturnVoid(node, "loadLibraries", "()V");

        MethodNode vulkanAvailable = requireMethod(
            node,
            "isVulkanLoaderAvailable",
            "()Z"
        );
        vulkanAvailable.instructions.clear();
        vulkanAvailable.tryCatchBlocks.clear();
        if (vulkanAvailable.localVariables != null) {
            vulkanAvailable.localVariables.clear();
        }
        vulkanAvailable.instructions.add(new InsnNode(Opcodes.ICONST_0));
        vulkanAvailable.instructions.add(new InsnNode(Opcodes.IRETURN));
        vulkanAvailable.maxStack = 1;
        vulkanAvailable.maxLocals = 0;

        applied.add("NativeLibrariesBootstrap.loadLibraries: browser no-op");
        applied.add("NativeLibrariesBootstrap.isVulkanLoaderAvailable: false");
        return write(node);
    }

    private static byte[] patchBlaze3D(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode getTime = requireMethod(node, "getTime", "()D");
        getTime.instructions.clear();
        getTime.tryCatchBlocks.clear();
        if (getTime.localVariables != null) {
            getTime.localVariables.clear();
        }
        getTime.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumSystemBridge",
            "timeSeconds",
            "()D",
            false
        ));
        getTime.instructions.add(new InsnNode(Opcodes.DRETURN));
        getTime.maxStack = 2;
        getTime.maxLocals = 0;

        applied.add("Blaze3D.getTime: browser clock bridge");
        MethodNode openUri = requireMethod(
            node,
            "openUri",
            "(Ljava/net/URI;)V"
        );
        openUri.instructions.clear();
        openUri.tryCatchBlocks.clear();
        if (openUri.localVariables != null) {
            openUri.localVariables.clear();
        }
        openUri.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        openUri.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumUtilityBridge",
            "openUri",
            "(Ljava/net/URI;)V",
            false
        ));
        openUri.instructions.add(new InsnNode(Opcodes.RETURN));
        openUri.maxStack = 1;
        openUri.maxLocals = 1;

        MethodNode openPath = requireMethod(
            node,
            "openPath",
            "(Ljava/nio/file/Path;)V"
        );
        openPath.instructions.clear();
        openPath.tryCatchBlocks.clear();
        if (openPath.localVariables != null) {
            openPath.localVariables.clear();
        }
        openPath.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        openPath.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumUtilityBridge",
            "openPath",
            "(Ljava/nio/file/Path;)V",
            false
        ));
        openPath.instructions.add(new InsnNode(Opcodes.RETURN));
        openPath.maxStack = 1;
        openPath.maxLocals = 1;

        applied.add("Blaze3D.openUri/openPath: browser external navigation");
        return write(node);
    }

    private static byte[] patchCursorType(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode select = requireMethod(node, "select", "()V");
        select.instructions.clear();
        select.tryCatchBlocks.clear();
        if (select.localVariables != null) {
            select.localVariables.clear();
        }
        select.instructions.add(new InsnNode(Opcodes.RETURN));
        select.maxStack = 0;
        select.maxLocals = 1;

        MethodNode create = requireMethod(
            node,
            "createStandardCursor",
            "(ILjava/lang/String;Lcom/mojang/blaze3d/platform/cursor/CursorType;)" +
                "Lcom/mojang/blaze3d/platform/cursor/CursorType;"
        );
        create.instructions.clear();
        create.tryCatchBlocks.clear();
        if (create.localVariables != null) {
            create.localVariables.clear();
        }
        create.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        create.instructions.add(new InsnNode(Opcodes.ARETURN));
        create.maxStack = 1;
        create.maxLocals = 3;

        MethodNode clinit = requireMethod(node, "<clinit>", "()V");
        clinit.instructions.clear();
        clinit.tryCatchBlocks.clear();
        if (clinit.localVariables != null) {
            clinit.localVariables.clear();
        }
        clinit.instructions.add(new TypeInsnNode(
            Opcodes.NEW,
            "com/mojang/blaze3d/platform/cursor/CursorType"
        ));
        clinit.instructions.add(new InsnNode(Opcodes.DUP));
        clinit.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("default"));
        clinit.instructions.add(new InsnNode(Opcodes.LCONST_1));
        clinit.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "com/mojang/blaze3d/platform/cursor/CursorType",
            "<init>",
            "(Ljava/lang/String;J)V",
            false
        ));
        clinit.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTSTATIC,
            "com/mojang/blaze3d/platform/cursor/CursorType",
            "DEFAULT",
            "Lcom/mojang/blaze3d/platform/cursor/CursorType;"
        ));
        clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        clinit.maxStack = 4;
        clinit.maxLocals = 0;

        applied.add("CursorType: remove SDL cursor creation/selection");
        return write(node);
    }

    private static byte[] patchInputQuirks(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode quit = requireMethod(node, "isQuitShortcutDown", "()Z");
        quit.instructions.clear();
        quit.tryCatchBlocks.clear();
        if (quit.localVariables != null) {
            quit.localVariables.clear();
        }
        quit.instructions.add(new InsnNode(Opcodes.ICONST_0));
        quit.instructions.add(new InsnNode(Opcodes.IRETURN));
        quit.maxStack = 1;
        quit.maxLocals = 0;

        applied.add("InputQuirks.isQuitShortcutDown: disable SDL modifier poll");
        return write(node);
    }

    private static byte[] patchMouseHandler(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode resync = requireMethod(
            node,
            "resyncMousePosition",
            "()V"
        );

        resync.instructions.clear();
        resync.tryCatchBlocks.clear();
        if (resync.localVariables != null) {
            resync.localVariables.clear();
        }

        resync.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        resync.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputBridge",
            "mouseX",
            "()D",
            false
        ));
        resync.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/client/MouseHandler",
            "xpos",
            "D"
        ));

        resync.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        resync.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumInputBridge",
            "mouseY",
            "()D",
            false
        ));
        resync.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/client/MouseHandler",
            "ypos",
            "D"
        ));

        resync.instructions.add(new InsnNode(Opcodes.RETURN));
        resync.maxStack = 3;
        resync.maxLocals = 1;

        applied.add("MouseHandler.resyncMousePosition: browser pointer state");
        return write(node);
    }

    private static byte[] patchMonitor(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode tryCreate = requireMethod(
            node,
            "tryCreate",
            "(I)Lcom/mojang/blaze3d/platform/Monitor;"
        );
        tryCreate.instructions.clear();
        tryCreate.tryCatchBlocks.clear();
        if (tryCreate.localVariables != null) {
            tryCreate.localVariables.clear();
        }
        tryCreate.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumDisplayBootstrap",
            "primaryMonitor",
            "()Lcom/mojang/blaze3d/platform/Monitor;",
            false
        ));
        tryCreate.instructions.add(new InsnNode(Opcodes.ARETURN));
        tryCreate.maxStack = 1;
        tryCreate.maxLocals = 1;

        MethodNode queryName = requireMethod(
            node,
            "queryMonitorName",
            "(I)Ljava/lang/String;"
        );
        queryName.instructions.clear();
        queryName.tryCatchBlocks.clear();
        if (queryName.localVariables != null) {
            queryName.localVariables.clear();
        }
        queryName.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
        queryName.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumDisplayBootstrap",
            "monitorName",
            "(I)Ljava/lang/String;",
            false
        ));
        queryName.instructions.add(new InsnNode(Opcodes.ARETURN));
        queryName.maxStack = 1;
        queryName.maxLocals = 1;

        applied.add("Monitor.tryCreate/queryMonitorName: synthetic browser display");
        return write(node);
    }

    private static byte[] patchMonitorManager(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode constructor = requireMethod(node, "<init>", "()V");
        constructor.instructions.clear();
        constructor.tryCatchBlocks.clear();
        if (constructor.localVariables != null) {
            constructor.localVariables.clear();
        }
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        ));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumDisplayBootstrap",
            "monitorMap",
            "()Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;",
            false
        ));
        constructor.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/MonitorManager",
            "monitors",
            "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"
        ));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 2;
        constructor.maxLocals = 1;

        replaceWithReturnVoid(node, "onDisplayConnected", "(I)V");
        replaceWithReturnVoid(node, "onDisplayDisconnected", "(I)V");
        replaceWithReturnVoid(node, "onDisplayModeChanged", "(I)V");

        MethodNode add = requireMethod(
            node,
            "addDisplay",
            "(I)Lcom/mojang/blaze3d/platform/Monitor;"
        );
        replaceWithStaticReturn(
            add,
            "dev/folium/render/webgpu/FoliumDisplayBootstrap",
            "primaryMonitor",
            "()Lcom/mojang/blaze3d/platform/Monitor;",
            Opcodes.ARETURN
        );

        MethodNode get = requireMethod(
            node,
            "getMonitor",
            "(I)Lcom/mojang/blaze3d/platform/Monitor;"
        );
        get.instructions.clear();
        get.tryCatchBlocks.clear();
        if (get.localVariables != null) {
            get.localVariables.clear();
        }
        get.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        get.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumDisplayBootstrap",
            "monitor",
            "(I)Lcom/mojang/blaze3d/platform/Monitor;",
            false
        ));
        get.instructions.add(new InsnNode(Opcodes.ARETURN));
        get.maxStack = 1;
        get.maxLocals = 2;

        MethodNode find = requireMethod(
            node,
            "findBestMonitor",
            "(Lcom/mojang/blaze3d/platform/Window;)Lcom/mojang/blaze3d/platform/Monitor;"
        );
        replaceWithStaticReturn(
            find,
            "dev/folium/render/webgpu/FoliumDisplayBootstrap",
            "primaryMonitor",
            "()Lcom/mojang/blaze3d/platform/Monitor;",
            Opcodes.ARETURN
        );

        applied.add("MonitorManager: replace SDL display enumeration with synthetic browser monitor");
        return write(node);
    }

    private static byte[] patchVideoMode(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        boolean removed = node.methods.removeIf(method ->
            method.name.equals("<init>") &&
            method.desc.equals("(Lorg/lwjgl/sdl/SDL_DisplayMode;)V")
        );

        if (!removed) {
            throw drift(
                "VideoMode SDL constructor expected exactly once"
            );
        }

        applied.add("VideoMode: remove SDL_DisplayMode constructor");
        return write(node);
    }

    private static byte[] patchTextInputManager(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode constructor = requireMethod(
            node,
            "<init>",
            "(Lcom/mojang/blaze3d/platform/Window;)V"
        );
        constructor.instructions.clear();
        constructor.tryCatchBlocks.clear();
        if (constructor.localVariables != null) {
            constructor.localVariables.clear();
        }
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        ));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        constructor.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "window",
            "Lcom/mojang/blaze3d/platform/Window;"
        ));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 2;
        constructor.maxLocals = 2;

        MethodNode setArea = requireMethod(
            node,
            "setTextInputArea",
            "(IIII)V"
        );
        setArea.instructions.clear();
        setArea.tryCatchBlocks.clear();
        putBooleanFieldOn(
            setArea.instructions,
            "com/mojang/blaze3d/platform/TextInputManager",
            "hasTextInputArea",
            true
        );
        putIntFieldFromLocalOn(
            setArea.instructions,
            "com/mojang/blaze3d/platform/TextInputManager",
            "areaX",
            1
        );
        putIntFieldFromLocalOn(
            setArea.instructions,
            "com/mojang/blaze3d/platform/TextInputManager",
            "areaY",
            2
        );
        putIntFieldFromLocalOn(
            setArea.instructions,
            "com/mojang/blaze3d/platform/TextInputManager",
            "areaWidth",
            3
        );
        putIntFieldFromLocalOn(
            setArea.instructions,
            "com/mojang/blaze3d/platform/TextInputManager",
            "areaHeight",
            4
        );
        setArea.instructions.add(new InsnNode(Opcodes.RETURN));
        setArea.maxStack = 2;
        setArea.maxLocals = 5;

        replaceWithReturnVoid(node, "applyTextInputArea", "()V");

        MethodNode start = requireMethod(
            node,
            "startTextInput",
            "(Ljava/lang/Object;)V"
        );
        start.instructions.clear();
        start.tryCatchBlocks.clear();
        start.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        start.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        start.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "owner",
            "Ljava/lang/Object;"
        ));
        start.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        start.instructions.add(new InsnNode(Opcodes.ICONST_1));
        start.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "textInputEnabled",
            "Z"
        ));
        start.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumTextInputBridge",
            "startTextInput",
            "()V",
            false
        ));
        start.instructions.add(new InsnNode(Opcodes.RETURN));
        start.maxStack = 2;
        start.maxLocals = 2;

        MethodNode stopOwner = requireMethod(
            node,
            "stopTextInput",
            "(Ljava/lang/Object;)V"
        );
        stopOwner.instructions.clear();
        stopOwner.tryCatchBlocks.clear();
        org.objectweb.asm.tree.LabelNode keep = new org.objectweb.asm.tree.LabelNode();
        stopOwner.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stopOwner.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.GETFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "owner",
            "Ljava/lang/Object;"
        ));
        stopOwner.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        stopOwner.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(
            Opcodes.IF_ACMPNE,
            keep
        ));
        stopOwner.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stopOwner.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/mojang/blaze3d/platform/TextInputManager",
            "stopTextInput",
            "()V",
            false
        ));
        stopOwner.instructions.add(keep);
        stopOwner.instructions.add(new InsnNode(Opcodes.RETURN));
        stopOwner.maxStack = 2;
        stopOwner.maxLocals = 2;

        MethodNode stop = requireMethod(node, "stopTextInput", "()V");
        stop.instructions.clear();
        stop.tryCatchBlocks.clear();
        stop.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stop.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        stop.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "owner",
            "Ljava/lang/Object;"
        ));
        stop.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        stop.instructions.add(new InsnNode(Opcodes.ICONST_0));
        stop.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            "com/mojang/blaze3d/platform/TextInputManager",
            "textInputEnabled",
            "Z"
        ));
        stop.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumTextInputBridge",
            "stopTextInput",
            "()V",
            false
        ));
        stop.instructions.add(new InsnNode(Opcodes.RETURN));
        stop.maxStack = 2;
        stop.maxLocals = 1;

        MethodNode focus = requireMethod(
            node,
            "onTextInputFocusChange",
            "(Ljava/lang/Object;Z)V"
        );
        focus.instructions.clear();
        focus.tryCatchBlocks.clear();
        org.objectweb.asm.tree.LabelNode unfocused = new org.objectweb.asm.tree.LabelNode();
        focus.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        focus.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(
            Opcodes.IFEQ,
            unfocused
        ));
        focus.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        focus.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        focus.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/mojang/blaze3d/platform/TextInputManager",
            "startTextInput",
            "(Ljava/lang/Object;)V",
            false
        ));
        focus.instructions.add(new InsnNode(Opcodes.RETURN));
        focus.instructions.add(unfocused);
        focus.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        focus.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        focus.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/mojang/blaze3d/platform/TextInputManager",
            "stopTextInput",
            "(Ljava/lang/Object;)V",
            false
        ));
        focus.instructions.add(new InsnNode(Opcodes.RETURN));
        focus.maxStack = 2;
        focus.maxLocals = 3;

        applied.add("TextInputManager: replace SDL text input with browser composition bridge");
        return write(node);
    }

    private static byte[] patchClipboardManager(
        byte[] original,
        List<String> applied
    ) {
        ClassNode node = read(original);

        MethodNode get = requireMethod(
            node,
            "getClipboard",
            "()Ljava/lang/String;"
        );
        get.instructions.clear();
        get.tryCatchBlocks.clear();
        if (get.localVariables != null) {
            get.localVariables.clear();
        }
        get.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumClipboardBridge",
            "getClipboard",
            "()Ljava/lang/String;",
            false
        ));
        get.instructions.add(new InsnNode(Opcodes.ARETURN));
        get.maxStack = 1;
        get.maxLocals = 1;

        MethodNode set = requireMethod(
            node,
            "setClipboard",
            "(Ljava/lang/String;)V"
        );
        set.instructions.clear();
        set.tryCatchBlocks.clear();
        if (set.localVariables != null) {
            set.localVariables.clear();
        }
        set.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        set.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "dev/folium/render/webgpu/FoliumClipboardBridge",
            "setClipboard",
            "(Ljava/lang/String;)V",
            false
        ));
        set.instructions.add(new InsnNode(Opcodes.RETURN));
        set.maxStack = 1;
        set.maxLocals = 2;

        applied.add("ClipboardManager: replace SDL clipboard with browser cache bridge");
        return write(node);
    }

    private static void putBooleanFieldOn(
        InsnList code,
        String owner,
        String field,
        boolean value
    ) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new InsnNode(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            owner,
            field,
            "Z"
        ));
    }

    private static void putIntFieldFromLocalOn(
        InsnList code,
        String owner,
        String field,
        int local
    ) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ILOAD, local));
        code.add(new org.objectweb.asm.tree.FieldInsnNode(
            Opcodes.PUTFIELD,
            owner,
            field,
            "I"
        ));
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
