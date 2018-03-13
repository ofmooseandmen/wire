Feature: Interfacing with the device to handle connection
  
    The connection with a device is established through the "urn:x-cast:com.google.cast.tp.connection" namespace/protocol.
    The connection is kept alive by exchanging PING/PONG message on the "urn:x-cast:com.google.cast.tp.heartbeat" namespace/protocol.
    Device authentication is handled by the "urn:x-cast:com.google.cast.tp.deviceauth" namespace/protocol.
   
    The PING interval is set to 2 seconds and the number of missed PONG messages is set to 2 when 
    running tests with an emulated device, therefore the connection shall timeout after 4 seconds.
    
    The client closes the connection after each scenario

  @EmulatedDevice
  Scenario: Open Connection
    When the connection with the device is opened
    Then the following messages shall be received by the device:
      | namespace                                | type    |
      | urn:x-cast:com.google.cast.tp.deviceauth | AUTH    |
      | urn:x-cast:com.google.cast.tp.heartbeat  | PING    |
      | urn:x-cast:com.google.cast.tp.connection | CONNECT |

  @EmulatedDevice
  Scenario: Authentication error
    Given the device rejects all authentication requests
    When the connection with the device is opened
    Then a "java.io.IOException" shall be thrown with message containing "Failed to authenticate with Cast device"

  @EmulatedDevice
  Scenario: Connection timeout
    Given the device has become unresponsive
    When the connection with the device is opened with a timeout of "PT1S"
    Then a "java.util.concurrent.TimeoutException" shall be thrown with message containing "No response received within"

  @EmulatedDevice
  Scenario: Disconnection
    Given the connection with the device has been opened
    When the connection is closed
    Then the following messages shall be received by the device:
      | namespace                                | type    |
      | urn:x-cast:com.google.cast.tp.deviceauth | AUTH    |
      | urn:x-cast:com.google.cast.tp.heartbeat  | PING    |
      | urn:x-cast:com.google.cast.tp.connection | CONNECT |
      | urn:x-cast:com.google.cast.tp.connection | CLOSE   |

  @EmulatedDevice
  Scenario: Disconnection after heartbeat timeout
    Given the connection with the device has been opened
    When the device becomes unresponsive
    Then the connection shall be closed after "PT4S"
    And the device controller listener is notified that the connection is dead
