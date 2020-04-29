/*
Copyright 2020-2020 Cedric Liegeois

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

import static io.omam.wire.Payloads.parse;

import java.util.Optional;

import io.omam.wire.CastChannel.CastMessage;

/**
 * Standard application controller that assumes request/reply correlation is implemented through the
 * {@link Payload#requestId() payload request identifier}.
 *
 * @see CastDeviceController#launchApp(String, java.util.function.BiFunction, java.time.Duration)
 * @see CastDeviceController#stopApp(ApplicationController, java.time.Duration)
 */
public abstract class StandardApplicationController implements ApplicationController {

    /** the details of this application. */
    private final ApplicationData details;

    /**
     * Constructor.
     *
     * @param someDetails the details of this application
     */
    protected StandardApplicationController(final ApplicationData someDetails) {
        details = someDetails;
    }

    @Override
    public final ApplicationData details() {
        return details;
    }

    @Override
    public final void messageReceived(final CastMessage message) {
        final Optional<Payload.Any> optPayload = parse(message);
        if (!optPayload.isPresent()) {
            return;
        }
        final Payload.Any payload = optPayload.get();
        if (payload.requestId().isPresent()) {
            return;
        }
        final Optional<String> optType = payload.type();
        if (!optType.isPresent()) {
            return;
        }
        unsolicitedMessageReceived(optType.get(), message);
    }

    @Override
    public final void socketError() {
        // ignore, handled by connection.
    }

    /**
     * Invoked when an unsolicited message with from the application has been received.
     * <p>
     * This method is invoked only with messages of the {@link ApplicationData#namespaces() application namespaces}
     * which are not responses to requests.
     *
     * @param type the type of the payload
     * @param message application message
     */
    protected abstract void unsolicitedMessageReceived(final String type, final CastMessage message);

}
