package de.exit.artnet2rig.core.artnet;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe multi-universe DMX512 buffer.
 * Holds up to {@value #MAX_UNIVERSES} universes, each with 512 channels.
 */
public class DmxUniverseBuffer {

    public static final int MAX_UNIVERSES = 16;
    public static final int CHANNELS_PER_UNIVERSE = 512;

    private final byte[][] universes = new byte[MAX_UNIVERSES][CHANNELS_PER_UNIVERSE];
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Updates an entire universe buffer from raw packet data.
     *
     * @param universe universe index (0 - 15)
     * @param data     incoming DMX byte array
     * @param length   number of valid channels in data (1 - 512)
     * @throws IllegalArgumentException if universe is out of bounds
     */
    public void updateUniverse(int universe, byte[] data, int length) {
        validateUniverse(universe);
        if (data == null || length <= 0) {
            return;
        }

        int copyLen = Math.min(length, CHANNELS_PER_UNIVERSE);
        rwLock.writeLock().lock();
        try {
            System.arraycopy(data, 0, universes[universe], 0, copyLen);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Sets a single channel value (1-512) for the given universe.
     *
     * @param universe universe index (0 - 15)
     * @param channel  1-based channel address (1 - 512)
     * @param value    DMX value (0 - 255)
     */
    public void setChannel(int universe, int channel, int value) {
        validateUniverse(universe);
        validateChannel(channel);

        rwLock.writeLock().lock();
        try {
            universes[universe][channel - 1] = (byte) (value & 0xFF);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the current 8-bit unsigned value (0-255) of a channel.
     *
     * @param universe universe index (0 - 15)
     * @param channel  1-based channel address (1 - 512)
     * @return unsigned channel value (0 - 255)
     */
    public int getChannel(int universe, int channel) {
        validateUniverse(universe);
        validateChannel(channel);

        rwLock.readLock().lock();
        try {
            return universes[universe][channel - 1] & 0xFF;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Creates a thread-safe snapshot copy of an entire universe buffer.
     *
     * @param universe universe index (0 - 15)
     * @return copy of the 512-byte universe array
     */
    public byte[] getUniverse(int universe) {
        validateUniverse(universe);

        rwLock.readLock().lock();
        try {
            return Arrays.copyOf(universes[universe], CHANNELS_PER_UNIVERSE);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Clears all universes back to zero.
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            for (byte[] u : universes) {
                Arrays.fill(u, (byte) 0);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void validateUniverse(int universe) {
        if (universe < 0 || universe >= MAX_UNIVERSES) {
            throw new IllegalArgumentException("Universe index out of range (0-" + (MAX_UNIVERSES - 1) + "): " + universe);
        }
    }

    private void validateChannel(int channel) {
        if (channel < 1 || channel > CHANNELS_PER_UNIVERSE) {
            throw new IllegalArgumentException("Channel address out of range (1-" + CHANNELS_PER_UNIVERSE + "): " + channel);
        }
    }
}
