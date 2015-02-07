package io.termd.core.tput;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfo {

  public static class Entry {
    final String name;
    final List<String> aliases;
    final List<Feature> features;
    public Entry(String name, List<String> aliases) {
      this.name = name;
      this.aliases = aliases;
      this.features = new ArrayList<>();
    }
  }

  public abstract static class Feature {
    public static class Boolean extends Feature {
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
    public static class String extends Feature {
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
    public static class Numeric extends Feature {
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
}
