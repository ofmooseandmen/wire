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
package io.omam.wire.discovery;

import java.io.IOException;

/**
 * A browser represents an active browsing operation to discover Cast devices.
 */
public interface CastDeviceBrowser extends AutoCloseable {

    /**
     * Browses the network for Cast devices and notifies changes in their availability to the given listener.
     * <p>
     * Cast devices already notified to the listener are not {@link #close() closed}, when the browser is closed.
     *
     * @param listener the listener to be notified when a Cast device availability changes
     * @return a {@link CastDeviceBrowser browser} to terminate the browsing operation
     * @throws IOException in case of I/O error
     */
    static CastDeviceBrowser start(final CastDeviceBrowserListener listener) throws IOException {
        final CastDeviceBrowserImpl browser = new CastDeviceBrowserImpl(listener);
        browser.start();
        return browser;
    }

    /**
     * Closes this browser. The browsing operation will be terminated and the associated listener will no longer
     * receive events.
     */
    @Override
    void close();

}
