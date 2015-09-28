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
 * A two dimensional vector object, used for dimension, position, etc...
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public final class Vector {

  private final int x;
  private final int y;

  public Vector(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int x() {
    return x;
  }

  public int y() {
    return y;
  }

  @Override
  public int hashCode() {
    return x + y * 101;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Vector) {
      Vector that = (Vector) obj;
      return x == that.x && y == that.y;
    }
    return false;
  }

  @Override
  public String toString() {
    return "Vector[x=" + x + ",y=" + y + "]";
  }
}
