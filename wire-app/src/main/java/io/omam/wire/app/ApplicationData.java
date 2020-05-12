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
package io.omam.wire.app;

import java.util.Collection;

/**
 * Describes an application launched on the Cast device.
 */
public interface ApplicationData {

    /**
     * A namespace defines a communication channel and protocol.
     */
    public static interface Namespace {

        /**
         * Returns the namespace, e.g. {@code urn:x-cast:com.google.cast.tp.connection}.
         *
         * @return the namespace
         */
        String name();

    }

    /**
     * Returns the ID of this application.
     *
     * @return the ID of this application
     */
    String applicationId();

    /**
     * Determines whether this is the backdrop application.
     *
     * @return {@code true} only if this is the backdrop application
     */
    boolean isIdleScreen();

    /**
     * Determines whether this application has been launched from the cloud.
     *
     * @return {@code true} if application has been launched from the cloud
     */
    boolean launchedFromCloud();

    /**
     * Returns the display name of this application.
     *
     * @return the display name of this application
     */
    String name();

    /**
     * Returns all {@link Namespace namespace}(s) implemented by this application.
     *
     * @return all namespace(s) implemented by this application
     */
    Collection<Namespace> namespaces();

    /**
     * Returns the ID of the running session.
     *
     * @return the ID of the running session
     */
    String sessionId();

    /**
     * Returns status text.
     *
     * @return the status text
     */
    String statusText();

    /**
     * Returns the destination ID to be used to communicate with this application.
     *
     * @return the destination ID to be used to communicate with this application
     */
    String transportId();

}
