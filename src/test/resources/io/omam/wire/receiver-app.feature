Feature: Interfacing with the receiver to control applications
  
    Controlling applications is achieved through the "urn:x-cast:com.google.cast.receiver" namespace/protocol.
    
    The following tests launch the default media receiver application which is always installed on the device.

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
    And the device controller listener shall be notified of a DEVICE_STATUS event
