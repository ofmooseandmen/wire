Feature: Interfacing with the receiver to control applications
  
    Controlling applications is achieved through the "urn:x-cast:com.google.cast.receiver" namespace/protocol.

  @EmulatedDevice @RealDevice
  Scenario: Device status
    Given the connection with the device has been opened
    When the device status is requested
    Then the received device status shall report no available application

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

  # this test launches the default media receiver application which is always installed on the device.
  @EmulatedDevice @RealDevice
  Scenario: Launch application
    Given the connection with the device has been opened
    When the application "CC1AD845" is requested to be launched
    Then the application "CC1AD845" shall be running on the device

  @EmulatedDevice @RealDevice
  Scenario: Stop application
    Given the connection with the device has been opened
    And the application "CC1AD845" has been launched
    When the application "CC1AD845" is requested to be stopped
    Then the application "CC1AD845" shall be not running on the device
    And the device controller listener shall be notified of the following events:
      | DEVICE_STATUS |
