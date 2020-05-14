Feature: Discovering devices on the local network
  
  Cast devices can be discovered using the mDNS and DNS-SD protocols.

  # this scenario assumes that a real cast device exists and therefore
  # does not check the attributes of the discovered device
  @RealDevice
  Scenario: Real device discovery
    When the network is browsed for cast devices
    Then a cast device shall be discovered

  @EmulatedDevice
  Scenario: Device discovery
    Given the cast device "EMULATED_DEVICE" exists
    When the network is browsed for cast devices
    Then the cast device "EMULATED_DEVICE" shall be discovered

  @EmulatedDevice
  Scenario: Device with friendly name discovery
    Given the cast device "EMULATED_DEVICE" reporting the friendly name "emulated device" exists
    When the network is browsed for cast devices
    Then the cast device "EMULATED_DEVICE" reporting the friendly name "emulated device" shall be discovered

  @EmulatedDevice
  Scenario: Device is removed when no longer discovered
    Given the cast device "EMULATED_DEVICE" has been discovered on the network
    When the cast device "EMULATED_DEVICE" is deregistered
    Then the cast device "EMULATED_DEVICE" is no longer discovered
