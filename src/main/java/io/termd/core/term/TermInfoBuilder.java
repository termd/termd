/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfoBuilder extends ParserHandler {

  static class Entry {
    final List<String> names = new ArrayList<>();
    final List<Feature<?>> features = new ArrayList<>();
    final List<String> uses = new ArrayList<>();
    boolean resolving;
    Device resolved;
  }

  private Entry entry;
  private final List<Entry> entries = new ArrayList<>();

  @Override
  public void beginHeaderLine(String name) {
    entry = new Entry();
    entry.names.add(name);
  }

  @Override
  public void addHeader(String name) {
    entry.names.add(name);
  }

  @Override
  public void endHeaderLine() {
  }

  @Override
  public void addBooleanFeature(String name, boolean value) {
    entry.features.add(Feature.create(name, value));
  }

  @Override
  public void addStringFeature(String name, Sequence value) {
    if ("use".equals(name)) {
      entry.uses.add(value.toString());
    } else {
      entry.features.add(Feature.create(name, value));
    }
  }

  @Override
  public void addNumericFeature(String name, int value) {
    entry.features.add(Feature.create(name, value));
  }

  @Override
  public void endDevice() {
    entries.add(entry);
    entry = null;
  }

  @Override
  public void endDatabase() {
  }

  public TermInfo build() {
    Map<String, Entry> entryMap = new HashMap<>();
    for (Entry entry : entries) {
      for (String alias : entry.names) {
        entryMap.put(alias, entry);
      }
    }
    Map<String, Device> devices = new LinkedHashMap<>();
    for (Entry entry : entryMap.values()) {
      Device resolved = resolve(entryMap, entry.names.get(0));
      devices.put(resolved.name, resolved);
      for (String synonym : resolved.synonyms) {
        devices.put(synonym, resolved);
      }
    }
    return new TermInfo(devices);
  }

  private Device resolve(
      Map<String, Entry> entryMap,
      String name) {
    Entry entry = entryMap.get(name);
    if (entry == null) {
      throw new IllegalStateException("Entry not found " + name);
    }
    if (entry.resolved != null) {
      return entry.resolved;
    }
    if (entry.resolving) {
      throw new IllegalStateException("Detected cycle in term info dependencies");
    }
    entry.resolving = true;
    Device result = new Device(entry.names);
    for (String use : entry.uses) {
      Device useEntry = resolve(entryMap, use);
      result.addFeatures(useEntry.getFeatures());
    }
    result.addFeatures(entry.features);
    entry.resolving = false;
    entry.resolved = result;
    return result;
  }
}
