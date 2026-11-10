package de.exit.artnet2view.service;

import de.exit.artnet2view.core.artnet.ArtNetDmxPacket;
import de.exit.artnet2view.core.artnet.ArtNetPacketParser;
import de.exit.artnet2view.core.artnet.DmxUniverseBuffer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing the high-performance UDP receiver socket for Art-Net 4 network streaming.
 */
public class ArtNetService {

    private static final Logger log = LoggerFactory.getLogger(ArtNetService.class);
    public static final int DEFAULT_PORT = 6454;

    private final DmxUniverseBuffer buffer;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket;
    private Thread receiverThread;

    private final AtomicLong packetCount = new AtomicLong(0);
    private final DoubleProperty fpsProperty = new SimpleDoubleProperty(0.0);
    private Thread fpsTrackerThread;

    public ArtNetService(DmxUniverseBuffer buffer) {
        this(buffer, DEFAULT_PORT);
    }

    public ArtNetService(DmxUniverseBuffer buffer, int port) {
        this.buffer = buffer != null ? buffer : new DmxUniverseBuffer();
        this.port = port;
    }

    /**
     * Starts listening for Art-Net UDP packets on a dedicated daemon thread.
     */
    public synchronized void start() {
        if (running.get()) {
            return;
        }

        try {
            socket = new DatagramSocket(port);
            socket.setReceiveBufferSize(256 * 1024); // 256 KB buffer
            running.set(true);

            receiverThread = new Thread(this::listenLoop, "ArtNetReceiver-Thread");
            receiverThread.setDaemon(true);
            receiverThread.setPriority(Thread.MAX_PRIORITY);
            receiverThread.start();

            startFpsTracker();
            log.info("ArtNetService listening on UDP port {}", port);
        } catch (SocketException e) {
            log.error("Could not bind Art-Net socket to port {}", port, e);
            running.set(false);
        }
    }

    private void listenLoop() {
        byte[] receiveBuffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);

        while (running.get() && socket != null && !socket.isClosed()) {
            try {
                socket.receive(packet);
                packetCount.incrementAndGet();

                Optional<ArtNetDmxPacket> dmxOpt = ArtNetPacketParser.parse(packet.getData(), packet.getLength());
                if (dmxOpt.isPresent()) {
                    ArtNetDmxPacket dmx = dmxOpt.get();
                    if (dmx.universe() >= 0 && dmx.universe() < DmxUniverseBuffer.MAX_UNIVERSES) {
                        buffer.updateUniverse(dmx.universe(), dmx.data(), dmx.length());
                    }
                }
            } catch (SocketException e) {
                if (!running.get()) {
                    break;
                }
                log.warn("Art-Net socket closed: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Error processing Art-Net packet", e);
            }
        }
    }

    private void startFpsTracker() {
        fpsTrackerThread = new Thread(() -> {
            long lastCount = 0;
            while (running.get()) {
                try {
                    Thread.sleep(1000);
                    long currentCount = packetCount.get();
                    long diff = currentCount - lastCount;
                    lastCount = currentCount;
                    javafx.application.Platform.runLater(() -> fpsProperty.set(diff));
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "ArtNet-FpsTracker");
        fpsTrackerThread.setDaemon(true);
        fpsTrackerThread.start();
    }

    /**
     * Gracefully stops the receiver thread and closes the UDP socket.
     */
    public synchronized void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        if (fpsTrackerThread != null) {
            fpsTrackerThread.interrupt();
        }

        fpsProperty.set(0.0);
        log.info("ArtNetService stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return port;
    }

    public DmxUniverseBuffer getBuffer() {
        return buffer;
    }

    public DoubleProperty fpsProperty() {
        return fpsProperty;
    }
}
