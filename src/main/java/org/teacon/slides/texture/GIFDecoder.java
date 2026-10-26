package org.teacon.slides.texture;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class GIFDecoder {

    public static final int DEFAULT_DELAY_MILLIS = 40;

    private final byte[] mBuf;
    private final LZWDecoder mDec;

    private int mPos;
    private final int mHeaderPos;

    private final int mScreenWidth;
    private final int mScreenHeight;
    private final int[] mGlobalPalette;  // rgba0 rgba1 ...
    private final int[] mImage;  // rgba0 rgba1 ...

    private int[] mTmpPalette;  // rgba0 rgba1 ...
    private final byte[] mTmpImage;

    private final int[] mTmpInterlace;

    public GIFDecoder(byte[] buf, LZWDecoder dec, boolean checkMagic) throws IOException {
        mBuf = buf;
        mDec = dec;

        // read GIF file header
        if (checkMagic) {
            int b;
            if (readByte() != 'G' || readByte() != 'I' || readByte() != 'F' ||
                    readByte() != '8' || ((b = readByte()) != '7' && b != '9') || readByte() != 'a') {
                throw new IOException();
            }
        } else {
            mPos = 6;
        }
        mScreenWidth = readShort();
        mScreenHeight = readShort();
        int packedField = readByte();
        skipBytes(2);

        if ((packedField & 0x80) != 0) {
            mGlobalPalette = readPalette(2 << (packedField & 7), -1, null);
        } else {
            mGlobalPalette = null;
        }
        mImage = new int[mScreenWidth * mScreenHeight];
        mTmpImage = new byte[mScreenWidth * mScreenHeight];

        mTmpInterlace = new int[mScreenHeight];

        mHeaderPos = mPos;
    }

    public static boolean checkMagic(byte @NotNull [] buf) {
        return buf.length >= 6 &&
                buf[0] == 'G' && buf[1] == 'I' && buf[2] == 'F' &&
                buf[3] == '8' && (buf[4] == '7' || buf[4] == '9') && buf[5] == 'a';
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public int decodeNextFrame(ByteBuffer pixels) throws IOException {
        int imageControlCode = syncNextFrame();

        if (imageControlCode < 0) {
            throw new IOException();
        }

        int left = readShort(), top = readShort(), width = readShort(), height = readShort();

        if (left + width > mScreenWidth || top + height > mScreenHeight) {
            throw new IOException();
        }

        int packedField = readByte();

        boolean isTransparent = ((imageControlCode >>> 24) & 1) != 0;
        int transparentIndex = isTransparent ? (imageControlCode >>> 16) & 0xFF : -1;
        boolean localPalette = (packedField & 0x80) != 0;
        boolean isInterlaced = (packedField & 0x40) != 0;

        int paletteSize = 2 << (packedField & 7);
        if (mTmpPalette == null || mTmpPalette.length < paletteSize) {
            mTmpPalette = new int[paletteSize];
        }
        int[] palette = localPalette ? readPalette(paletteSize, transparentIndex, mTmpPalette) : mGlobalPalette;
        if (palette == null) {
            throw new IOException();
        }

        int delayTime = imageControlCode & 0xFFFF;

        int disposalCode = (imageControlCode >>> 26) & 7;
        decodeImage(mTmpImage, width, height, isInterlaced ? computeInterlaceReIndex(height, mTmpInterlace) : null);

        decodePalette(mTmpImage, palette, transparentIndex, left, top, width, height, disposalCode, pixels);

        return delayTime != 0 ? delayTime * 10 : DEFAULT_DELAY_MILLIS;
    }

    private int @NotNull [] readPalette(int size, int transparentIndex, int[] palette) throws IOException {
        // max size is 256, one packed rgba int per entry (little-endian: r | g<<8 | b<<16 | a<<24)
        if (palette == null) {
            palette = new int[size];
        }
        for (int i = 0; i < size; ++i) {
            int r = readByte();
            int g = readByte();
            int b = readByte();
            int a = (i == transparentIndex) ? 0 : 0xFF;
            palette[i] = r | (g << 8) | (b << 16) | (a << 24);
        }
        return palette;
    }

    public void skipExtension() throws IOException {
        for (int blockSize = readByte();
             blockSize != 0;
             blockSize = readByte()) {
            skipBytes(blockSize);
        }
    }

    /**
     * Peeks ahead whether another image separator follows the frame that was just decoded,
     * without consuming input. Single-frame GIFs use this to skip per-tick re-decoding.
     */
    public boolean hasNextFrame() {
        int pos = mPos;
        try {
            for (; ; ) {
                int ch = read();
                switch (ch) {
                    case 0x2C:  // Image Separator
                        return true;
                    case 0x21:  // Extension Introducer
                        readByte();
                        skipExtension();
                        break;
                    case -1:    // EOF
                    case 0x3B:  // Trailer
                    default:
                        return false;
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            mPos = pos;
        }
    }

    private int readControlCode() throws IOException {
        int blockSize = readByte();
        int packedField = readByte();
        int delayTime = readShort();
        int transparentIndex = readByte();

        if (blockSize != 4 || readByte() != 0) {
            throw new IOException();
        }
        return ((packedField & 0x1F) << 24) + (transparentIndex << 16) + delayTime;
    }

    private int syncNextFrame() throws IOException {
        int controlData = 0;
        boolean restarted = false;
        for (; ; ) {
            int ch = read();
            switch (ch) {
                case 0x2C:
                    return controlData;
                case 0x21:
                    if (readByte() == 0xF9) {
                        controlData = readControlCode();
                    } else {
                        skipExtension();
                    }
                    break;
                case -1:
                case 0x3B:
                    if (restarted) {
                        return -1;
                    }
                    mPos = mHeaderPos;
                    controlData = 0;
                    restarted = true;
                    continue;
                default:
                    throw new IOException();
            }
        }
    }

    private void decodeImage(byte[] image, int width, int height, int[] interlace) throws IOException {
        final LZWDecoder dec = mDec;
        byte[] data = dec.setContext(this);
        int y = 0, iPos = 0, xr = width;
        for (; ; ) {
            int len = dec.readString();
            if (len == -1) { // end of stream
                skipExtension();
                return;
            }
            for (int pos = 0; pos < len; ) {
                int ax = Math.min(xr, (len - pos));
                System.arraycopy(data, pos, image, iPos, ax);
                iPos += ax;
                pos += ax;
                if ((xr -= ax) == 0) {
                    if (++y == height) { // image is full
                        skipExtension();
                        return;
                    }
                    int iY = interlace == null ? y : interlace[y];
                    iPos = iY * width;
                    xr = width;
                }
            }
        }
    }

    private int @NotNull [] computeInterlaceReIndex(int height, int[] data) {
        int pos = 0;
        for (int i = 0; i < height; i += 8) data[pos++] = i;
        for (int i = 4; i < height; i += 8) data[pos++] = i;
        for (int i = 2; i < height; i += 4) data[pos++] = i;
        for (int i = 1; i < height; i += 2) data[pos++] = i;
        return data;
    }

    // GIF specification states that restore to background should fill the frame
    // with background color, but actually all modern programs fill with transparent color.
    private void restoreToBackground(int[] image, int left, int top, int width, int height) {
        for (int y = 0; y < height; ++y) {
            int iPos = (top + y) * mScreenWidth + left;
            for (int x = 0; x < width; iPos++, ++x) {
                image[iPos] &= 0xFFFFFF;
            }
        }
    }

    private void decodePalette(byte[] srcImage, int[] palette, int transparentIndex,
                               int left, int top, int width, int height, int disposalCode,
                               ByteBuffer pixels) {
        // Restore to previous
        if (disposalCode == 3) {
            pixels.asIntBuffer().put(mImage);
            for (int y = 0; y < height; ++y) {
                int iPos = ((top + y) * mScreenWidth + left) * 4;
                int i = y * width;
                if (transparentIndex < 0) {
                    for (int x = 0; x < width; ++x) {
                        pixels.putInt(iPos, palette[0xFF & srcImage[i + x]]);
                        iPos += 4;
                    }
                } else {
                    for (int x = 0; x < width; ++x) {
                        int index = 0xFF & srcImage[i + x];
                        if (index != transparentIndex) {
                            pixels.putInt(iPos, palette[index]);
                        }
                        iPos += 4;
                    }
                }
            }
        } else {
            final int[] image = mImage;
            for (int y = 0; y < height; ++y) {
                int iPos = (top + y) * mScreenWidth + left;
                int i = y * width;
                if (transparentIndex < 0) {
                    for (int x = 0; x < width; ++x) {
                        image[iPos++] = palette[0xFF & srcImage[i + x]];
                    }
                } else {
                    for (int x = 0; x < width; ++x) {
                        int index = 0xFF & srcImage[i + x];
                        if (index != transparentIndex) {
                            image[iPos] = palette[index];
                        }
                        iPos++;
                    }
                }
            }

            pixels.asIntBuffer().put(image);
            if (disposalCode == 2) {
                restoreToBackground(mImage, left, top, width, height);
            }
        }
    }

    private int read() {
        if (mPos < mBuf.length)
            return (mBuf[mPos++] & 0xff);
        return -1;
    }

    public int readByte() throws IOException {
        if (mPos < mBuf.length)
            return (mBuf[mPos++] & 0xff);
        throw new EOFException();
    }

    private int readShort() throws IOException {
        int lsb = readByte(), msb = readByte();
        return lsb + (msb << 8);
    }

    public void readBytes(byte[] b, int off, int len) throws IOException {
        int avail = mBuf.length - mPos;
        if (avail <= 0) {
            throw new EOFException();
        }
        if (len > avail) {
            len = avail;
        }
        if (len <= 0) {
            return;
        }
        System.arraycopy(mBuf, mPos, b, off, len);
        mPos += len;
    }

    private void skipBytes(int n) throws IOException {
        if (mPos + n < mBuf.length) {
            mPos += n;
        } else {
            throw new EOFException();
        }
    }
}
