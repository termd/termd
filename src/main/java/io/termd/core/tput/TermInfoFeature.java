package io.termd.core.tput;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public abstract class TermInfoFeature {

  public static class Boolean extends TermInfoFeature {
    final java.lang.String name;
    public Boolean(java.lang.String name) {
      this.name = name;
    }
    public boolean equals(Object obj) {
      if (obj instanceof Boolean) {
        Boolean that = (Boolean) obj;
        return name.equals(that.name);
      }
      return false;
    }
    public java.lang.String toString() {
      return "Feature[type=Boolean,name=" + name + "]";
    }
  }

  public static class String extends TermInfoFeature {
    final java.lang.String name;
    final java.lang.String value;
    public String(java.lang.String name, java.lang.String value) {
      this.name = name;
      this.value = value;
    }
    public boolean equals(Object obj) {
      if (obj instanceof String) {
        String that = (String) obj;
        return name.equals(that.name) && value.equals(that.value);
      }
      return false;
    }
    public java.lang.String toString() {
      return "Feature[type=String,name=" + name + ",value=" + value + "]";
    }
  }

  public static class Numeric extends TermInfoFeature {
    final java.lang.String name;
    final java.lang.String value;
    public Numeric(java.lang.String name, java.lang.String value) {
      this.name = name;
      this.value = value;
    }
    public boolean equals(Object obj) {
      if (obj instanceof Numeric) {
        Numeric that = (Numeric) obj;
        return name.equals(that.name) && value.equals(that.value);
      }
      return false;
    }
    public java.lang.String toString() {
      return "Feature[type=Numeric,name=" + name + ",value=" + value + "]";
    }
  }
}
