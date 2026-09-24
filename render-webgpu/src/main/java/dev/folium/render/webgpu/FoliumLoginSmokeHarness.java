package dev.folium.render.webgpu;

import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.util.Crypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.UUID;

public final class FoliumLoginSmokeHarness {
    private final Connection connection;
    private final SmokeLoginListener listener;

    private boolean stopped;

    private FoliumLoginSmokeHarness(
        Connection connection,
        SmokeLoginListener listener
    ) {
        this.connection = connection;
        this.listener = listener;
    }

    public static FoliumLoginSmokeHarness start(
        String handshakeHost,
        int port,
        String username
    ) {
        FoliumNetworkTrace.reset();
        FoliumNetworkTrace.stage("CONNECT");

        try {
            Connection connection = Connection.connectToServer(
                new InetSocketAddress(handshakeHost, port),
                null,
                null
            );

            SmokeLoginListener listener =
                new SmokeLoginListener(connection);

            FoliumNetworkTrace.stage("HANDSHAKE");

            connection.initiateServerboundPlayConnection(
                handshakeHost,
                port,
                listener
            );

            UUID profileId = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username)
                    .getBytes(StandardCharsets.UTF_8)
            );

            FoliumNetworkTrace.stage("LOGIN_HELLO_SEND");
            connection.send(
                new ServerboundHelloPacket(
                    username,
                    profileId
                )
            );

            return new FoliumLoginSmokeHarness(
                connection,
                listener
            );
        } catch (Throwable error) {
            FoliumNetworkTrace.fail("START", error);
            throw error;
        }
    }

    public void tick() {
        if (stopped) {
            return;
        }

        try {
            connection.tick();

            if (listener.finished) {
                stopped = true;
                FoliumNetworkTrace.stage("LOGIN_FINISHED");
            }

            if (!connection.isConnected()) {
                stopped = true;
                FoliumNetworkTrace.event(
                    "connection closed at " +
                        FoliumNetworkTrace.stage()
                );
            }
        } catch (Throwable error) {
            stopped = true;
            FoliumNetworkTrace.fail(
                FoliumNetworkTrace.stage(),
                error
            );
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public Connection connection() {
        return connection;
    }

    private static final class SmokeLoginListener
        implements ClientLoginPacketListener {

        private final Connection connection;

        private boolean accepting = true;
        private boolean finished;

        private SmokeLoginListener(Connection connection) {
            this.connection = connection;
        }

        @Override
        public PacketFlow flow() {
            return PacketFlow.CLIENTBOUND;
        }

        @Override
        public ConnectionProtocol protocol() {
            return ConnectionProtocol.LOGIN;
        }

        @Override
        public boolean isAcceptingMessages() {
            return accepting;
        }

        @Override
        public void onDisconnect(
            DisconnectionDetails details
        ) {
            accepting = false;
            FoliumNetworkTrace.event(
                "disconnect: " + details
            );
        }

        @Override
        public void handleCompression(
            ClientboundLoginCompressionPacket packet
        ) {
            int threshold = packet.getCompressionThreshold();

            FoliumNetworkTrace.stage(
                "COMPRESSION_" + threshold
            );

            connection.setupCompression(
                threshold,
                true
            );
        }

        @Override
        public void handleHello(
            ClientboundHelloPacket packet
        ) {
            FoliumNetworkTrace.stage(
                "ENCRYPTION_CHALLENGE"
            );

            try {
                PublicKey publicKey = packet.getPublicKey();
                SecretKey secretKey = Crypt.generateSecretKey();

                new ServerboundKeyPacket(
                    secretKey,
                    publicKey,
                    packet.getChallenge()
                );

                Cipher decrypt = Crypt.getCipher(
                    Cipher.DECRYPT_MODE,
                    secretKey
                );

                Cipher encrypt = Crypt.getCipher(
                    Cipher.ENCRYPT_MODE,
                    secretKey
                );

                FoliumNetworkTrace.event(
                    "JCE crypto primitives OK; " +
                        "server authentication required=" +
                        packet.shouldAuthenticate()
                );

                if (packet.shouldAuthenticate()) {
                    accepting = false;
                    FoliumNetworkTrace.stage(
                        "SESSION_AUTH_REQUIRED"
                    );
                    return;
                }

                ServerboundKeyPacket response =
                    new ServerboundKeyPacket(
                        secretKey,
                        publicKey,
                        packet.getChallenge()
                    );

                connection.send(response);
                connection.setEncryptionKey(
                    decrypt,
                    encrypt
                );

                FoliumNetworkTrace.stage(
                    "ENCRYPTION_ENABLED"
                );
            } catch (Throwable error) {
                accepting = false;
                FoliumNetworkTrace.fail(
                    "ENCRYPTION_CRYPTO",
                    error
                );
            }
        }

        @Override
        public void handleLoginFinished(
            ClientboundLoginFinishedPacket packet
        ) {
            finished = true;
            FoliumNetworkTrace.event(
                "login profile=" +
                    packet.gameProfile().getName()
            );
        }

        @Override
        public void handleDisconnect(
            ClientboundLoginDisconnectPacket packet
        ) {
            accepting = false;
            FoliumNetworkTrace.event(
                "login disconnect: " +
                    packet.reason().getString()
            );
        }

        @Override
        public void handleCustomQuery(
            ClientboundCustomQueryPacket packet
        ) {
            FoliumNetworkTrace.event(
                "custom login query id=" +
                    packet.transactionId()
            );

            connection.send(
                new ServerboundCustomQueryAnswerPacket(
                    packet.transactionId(),
                    null
                )
            );
        }

        @Override
        public void handleRequestCookie(
            ClientboundCookieRequestPacket packet
        ) {
            FoliumNetworkTrace.event(
                "cookie request " + packet.key()
            );

            connection.send(
                new ServerboundCookieResponsePacket(
                    packet.key(),
                    null
                )
            );
        }
    }
}
