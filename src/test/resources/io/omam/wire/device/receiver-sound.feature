Feature: Interfacing with the receiver to control device sound
  
    Controlling device sound is achieved through the "urn:x-cast:com.google.cast.receiver" namespace/protocol.

  @EmulatedDevice @RealDevice
  Scenario: Set device volume
    Given the connection with the device has been opened
    When the device volume is requested to be set to 0.5
    Then the received device status shall report a volume level of 0.5

  @EmulatedDevice @RealDevice
  Scenario: Mute device
    Given the connection with the device has been opened
    When the device is requested to be muted
    Then the received device status shall report a muted volume

  @EmulatedDevice @RealDevice
  Scenario: Unmute device
    Given the connection with the device has been opened
    When the device is requested to be unmuted
    Then the received device status shall report an umuted volume
