Feature: Handling error responses
  
    Requests can only be issued to the receiver when the connection is opened
    Only applications installed on the receiver can be launched

  @EmulatedDevice @RealDevice
  Scenario: Request device status when not connected
    When the device status is requested
    Then a "java.io.IOException" shall be thrown with message containing "Connection is not opened"

  @EmulatedDevice @RealDevice
  Scenario: Request set device sound when not connected
    When the device volume is requested to be set to 0.5
    Then a "java.io.IOException" shall be thrown with message containing "Connection is not opened"

  @EmulatedDevice @RealDevice
  Scenario: Request mute device when not connected
    When the device is requested to be muted
    Then a "java.io.IOException" shall be thrown with message containing "Connection is not opened"

  @EmulatedDevice @RealDevice
  Scenario: Request unmute device when not connected
    When the device is requested to be unmuted
    Then a "java.io.IOException" shall be thrown with message containing "Connection is not opened"

  @EmulatedDevice @RealDevice
  Scenario: Request application launch when not connected
    When the application "CC1AD845" is requested to be launched
    Then a "java.io.IOException" shall be thrown with message containing "Connection is not opened"

  @EmulatedDevice @RealDevice
  Scenario: Request uninstalled application launch
    Given the connection with the device has been opened
    When the application "FOOBAR" is requested to be launched
    Then a "java.io.IOException" shall be thrown with message containing "LAUNCH_ERROR"
