/*
Copyright 2018 Cedric Liegeois

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
package io.omam.wire;

import static io.omam.wire.Bytes.toBytes;
import static io.omam.wire.Bytes.toInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import io.omam.wire.CastChannel.CastMessage;

/**
 * Encoder/decoder of {@link CastMessage}.
 */
final class CastMessageCodec {

    /**
     * Constructor.
     */
    private CastMessageCodec() {
        // empty.
    }

    /**
     * Reads the next Cast message from the given input stream.
     *
     * @param is input stream
     * @return a new Cast message or empty if the end of the stream has been reached
     * @throws IOException in case of I/O error
     */
    static Optional<CastMessage> read(final InputStream is) throws IOException {
        final int size = readSize(is);
        if (size == -1) {
            return Optional.empty();
        }
        final byte[] buf = new byte[size];
        int read = 0;
        while (read < size) {
            final int readLen = is.read(buf, read, buf.length - read);
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
     * @param os output stream
     * @throws IOException in case of I/O error
     */
    static void write(final CastMessage message, final OutputStream os) throws IOException {
        /* first the size of the message. */
        os.write(toBytes(message.getSerializedSize()));
        /* then the message itself. */
        message.writeTo(os);
    }

    /**
     * Reads the 4 first bytes from the given input stream: this is the size of the message.
     *
     * @param is input stream
     * @return size of message or {@code -1} if the end of the stream has been reached
     * @throws IOException in case of I/O error
     */
    private static int readSize(final InputStream is) throws IOException {
        final byte[] buf = new byte[4];
        int read = 0;
        while (read < buf.length) {
            final int nextByte = is.read();
            if (nextByte == -1) {
                return -1;
            }
            buf[read++] = (byte) nextByte;
        }
        return toInt(buf);
    }

}
