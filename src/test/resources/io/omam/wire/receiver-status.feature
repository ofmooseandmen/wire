Feature: Interfacing with the receiver status
  
    Interrogating the receiver status is achieved through the "urn:x-cast:com.google.cast.receiver" namespace/protocol.

  @EmulatedDevice @RealDevice
  Scenario: Device status
    Given the connection with the device has been opened
    When the device status is requested
    Then the received device status shall report no available application
