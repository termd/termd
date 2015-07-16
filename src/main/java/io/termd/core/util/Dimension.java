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

package io.termd.core.util;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public final class Dimension {

  private final int width;
  private final int height;

  public Dimension(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Dimension) {
      Dimension that = (Dimension) obj;
      return width == that.width && height == that.height;
    }
    return false;
  }

  @Override
  public String toString() {
    return "Dimension[width=" + width + ",height=" + height + "]";
  }
}
