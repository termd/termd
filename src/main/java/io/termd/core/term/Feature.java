package io.termd.core.term;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class Feature<T> {

  public static <T> Feature<T> create(String name, T value) {
    Capability<T> cap = null;
    if (value instanceof Boolean) {
      cap = (Capability<T>) Capability.getCapability(name, Boolean.class);
    } else if (value instanceof Integer) {
      cap = (Capability<T>) Capability.getCapability(name, Integer.class);
    } else if (value instanceof String) {
      cap = (Capability<T>) Capability.getCapability(name, String.class);
    }
    if (cap == null) {
      cap = new Capability<>((Class<T>) value.getClass(), null, name, null, null);
    }
    return new Feature<>(cap, value);
  }

  final Capability<T> capability;
  final T value;

  public Feature(Capability<T> capability, T value) {
    this.capability = capability;
    this.value = value;
  }

  public Feature(String name, T value) {
    this.capability = new Capability<>((Class<T>) value.getClass(), name, name, null, null);
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Feature<?>) {
      Feature<?> that = (Feature<?>) obj;
      return capability.equals(that.capability) && value.equals(that.value);
    }
    return false;
  }

  @Override
  public String toString() {
    if (value instanceof Boolean) {
      Boolean booleanValue = (Boolean) value;
      return booleanValue ? capability.name : (capability.name + "@");
    } else if (value instanceof Integer) {
      return capability.name + "#" + value;
    } else {
      return capability.name + "=" + value;
    }
  }
}
