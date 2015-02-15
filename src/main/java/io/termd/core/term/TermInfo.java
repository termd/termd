package io.termd.core.term;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfo {

  final Map<String, Device> devices;

  TermInfo(Map<String, Device> devices) {
    this.devices = devices;
  }

  public Device getDevice(String name) {
    return devices.get(name);
  }

  public Collection<Device> getDevices() {
    return devices.values();
  }
}
