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

/**
 * The volume of a device.
 *
 * @see <a href="https://developers.google.com/cast/docs/reference/chrome/chrome.cast.Volume">Google Cast
 *      Reference: Volume</a>
 */
public interface CastDeviceVolume extends Volume {

    /**
     * Types of volume control.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/chrome/chrome.cast#.VolumeControlType">Google Cast
     *      Reference: Volume Control Type</a>
     */
    public enum VolumeControlType {
        /** Cast device volume can be changed. */
        ATTENUATION,
        /** Cast device volume is fixed and cannot be changed. */
        FIXED,
        /** Master system volume control, i.e. TV or Audio device volume is changed. */
        MASTER;
    }

    /**
     * @return type of volume control that is available.
     */
    VolumeControlType controlType();

    /**
     * @return the valid steps for changing volume.
     */
    double stepInterval();

}
