package io.termd.core.tput;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Feature {

  public static class Boolean extends Feature {
    public final String name;
    public Boolean(String name) {
      this.name = name;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Feature.Boolean) {
        return ((Feature.Boolean) obj).name.equals(name);
      }
      return false;
    }
  }

  public static class String extends Feature {
    public final String name;
    public final String value;
    public String(String name, String value) {
      this.name = name;
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Feature.String) {
        String that = (String) obj;
        return that.name.equals(name) && that.value.equals(value);
      }
      return false;
    }
  }

  public static class Numeric extends Feature {
    public final String name;
    public final String value;
    public Numeric(String name, String value) {
      this.name = name;
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Feature.Numeric) {
        Numeric that = (Numeric) obj;
        return that.name.equals(name) && that.value.equals(value);
      }
      return false;
    }
  }

}
