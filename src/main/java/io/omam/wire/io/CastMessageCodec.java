/*
Copyright 2018-2020 Cedric Liegeois

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the copyright holder nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package io.omam.wire.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import io.omam.wire.io.CastChannel.CastMessage;

/**
 * Encoder/decoder of {@link CastMessage}.
 */
public final class CastMessageCodec {

    /**
     * Constructor.
     */
    private CastMessageCodec() {
        // empty.
    }

    /**
     * Reads the next Cast message from the given input stream.
     *
     * @param input input stream
     * @return a new Cast message or empty if the end of the stream has been reached
     * @throws IOException in case of I/O error
     */
    public static Optional<CastMessage> read(final InputStream input) throws IOException {
        final int size = readSize(input);
        if (size == -1) {
            return Optional.empty();
        }
        final byte[] buf = new byte[size];
        int read = 0;
        while (read < size) {
            final int readLen = input.read(buf, read, buf.length - read);
            if (readLen == -1) {
                return Optional.empty();
            }
            read += readLen;
        }
        return Optional.of(CastMessage.parseFrom(buf));
    }

    /**
     * Writes the given message to the given output stream.
     *
     * @param message message to write
     * @param output output stream
     * @throws IOException in case of I/O error
     */
    public static void write(final CastMessage message, final OutputStream output) throws IOException {
        /* first the size of the message. */
        output.write(toBytes(message.getSerializedSize()));
        /* then the message itself. */
        message.writeTo(output);
    }

    /**
     * Reads the 4 first bytes from the given input stream: this is the size of the message.
     *
     * @param input input stream
     * @return size of message or {@code -1} if the end of the stream has been reached
     * @throws IOException in case of I/O error
     */
    private static int readSize(final InputStream input) throws IOException {
        final byte[] buf = new byte[4];
        int read = 0;
        while (read < buf.length) {
            final int nextByte = input.read();
            if (nextByte == -1) {
                return -1;
            }
            buf[read++] = (byte) nextByte;
        }
        return toInt(buf);
    }

    /**
     * Converts the given integer into its Big Endian binary representation.
     *
     * @param value integer
     * @return bytes
     */
    private static byte[] toBytes(final int value) {
        return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
    }

    /**
     * Converts the bytes (Big Endian) into an integer.
     *
     * @param bytes bytes
     * @return integer
     */
    private static int toInt(final byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | bytes[3] & 0xFF;
    }

}
