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

package io.termd.core.telnet;

/**
 * The handler that defines the callbacks for a telnet connection.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler {

  /**
   * The telnet connection opened.
   *
   * @param conn the connection
   */
  protected void onOpen(TelnetConnection conn) {}

  /**
   * The telnet connection closed.
   */
  protected void onClose() {}

  /**
   * Process data sent by the client.
   *
   * @param data the data
   */
  protected void onData(byte[] data) {}

  protected void onSize(int width, int height) {}
  protected void onTerminalType(String terminalType) {}
  protected void onCommand(byte command) {}
  protected void onNAWS(boolean naws) {}
  protected void onEcho(boolean echo) {}
  protected void onSGA(boolean sga) {}
  protected void onSendBinary(boolean binary) { }
  protected void onReceiveBinary(boolean binary) { }

}
