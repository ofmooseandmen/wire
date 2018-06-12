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

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;

import java.time.Duration;
import java.util.Locale;

import cucumber.api.TypeRegistry;
import cucumber.api.TypeRegistryConfigurer;
import io.cucumber.cucumberexpressions.CaptureGroupTransformer;
import io.cucumber.cucumberexpressions.ParameterType;
import io.cucumber.datatable.DataTableType;
import io.omam.wire.ScenarioRuntime.EventType;

/**
 * Type registry configuration for custom types used in step definitions.
 */
public final class TypeRegistryConfiguration implements TypeRegistryConfigurer {

    /**
     * Constructor.
     *
     */
    public TypeRegistryConfiguration() {
        // empty.
    }

    @Override
    public final void configureTypeRegistry(final TypeRegistry typeRegistry) {

        typeRegistry.defineParameterType(
                new ParameterType<>("duration", singletonList("PT[HMS0-9]."), Duration.class,
                                    (CaptureGroupTransformer<Duration>) s -> Duration.parse(s[0])));

        typeRegistry.defineParameterType(
                new ParameterType<>("eventType",
                                    stream(EventType.values()).map(EventType::toString).collect(joining("|")),
                                    EventType.class,
                                    (CaptureGroupTransformer<EventType>) s -> EventType.valueOf(s[0])));

        typeRegistry.defineDataTableType(new DataTableType(ReceivedMessage.class, ReceivedMessage::new));

    }

    @Override
    public Locale locale() {
        return Locale.ENGLISH;
    }
}
