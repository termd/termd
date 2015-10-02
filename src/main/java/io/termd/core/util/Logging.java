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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulate logging stuff as much as possible.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public final class Logging {

  public static final Logger IO_ERROR = Logger.getLogger("io.termd.core.io_error");
  public static final Logger READLINE = Logger.getLogger("io.termd.core.readline");
  public static final Logger TERMINFO = Logger.getLogger("io.termd.core.terminfo");

  /**
   * Log an io error reported by the IO layer that lead to closing the resource
   *
   * @param cause the error
   */
  public static void logReportedIoError(Throwable cause) {
    Logging.IO_ERROR.log(Level.SEVERE, "Reported io error => closing", cause);
  }

  /**
   * Log an io error caught by Termd that could not be propagated to the call stack due
   * to the async nature.
   *
   * @param cause the error
   */
  public static void logUndeclaredIoError(Throwable cause) {
    Logging.IO_ERROR.log(Level.SEVERE, "IO error", cause);
  }

  private Logging() {
  }
}
