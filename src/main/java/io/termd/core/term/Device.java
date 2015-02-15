package io.termd.core.term;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class Device {

  final String name;
  final List<String> synonyms;
  final String longname;
  final Map<Capability<?>, Feature<?>> features;

  public Device(List<String> names) {
    name = names.get(0);
    synonyms = names.size() > 2 ? names.subList(1, names.size() - 1) : Collections.<String>emptyList();
    longname = names.size() > 1 ? names.get(names.size() - 1) : null;
    features = new HashMap<>();
  }

  public Device(String name, List<String> synonyms, String longname) {
    this.name = name;
    this.synonyms = synonyms;
    this.features = new HashMap<>();
    this.longname = longname;
  }

  public Collection<Feature<?>> getFeatures() {
    return features.values();
  }

  public <T> T getFeature(Capability<T> capability) {
    return getFeature(capability, null);
  }

  public <T> T getFeature(Capability<T> capability, T def) {
    Feature feature = features.get(capability);
    if (feature != null) {
      return capability.type.cast(feature.value);
    }
    return def;
  }

  public void addFeature(Feature<?> feature) {
    features.put(feature.capability, feature);
  }

  public void addFeature(String name, Object value) {
    addFeature(Feature.create(name, value));
  }

  public void addFeatures(Iterable<Feature<?>> features) {
    for (Feature<?> feature : features) {
      addFeature(feature);
    }
  }

  @Override
  public String toString() {
    return "Device[name=" + name + "]";
  }
}
