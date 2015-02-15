package io.termd.core.term;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfo {

  private static TermInfo loadDefault() {
    try {
      InputStream in = TermInfo.class.getResourceAsStream("terminfo.src");
      TermInfoParser parser = new TermInfoParser(new InputStreamReader(in, "US-ASCII"));
      TermInfoBuilder builder = new TermInfoBuilder();
      parser.parseDatabase(builder);
      return builder.build();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

  private static final TermInfo DEFAULT = loadDefault();

  public static TermInfo getDefault() {
    return DEFAULT;
  }

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
