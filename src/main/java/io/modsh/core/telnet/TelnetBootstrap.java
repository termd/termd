/*
 * Copyright 2014 Julien Viet
 *
 * Julien Viet licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.modsh.core.telnet;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetBootstrap {

  protected final String host;
  protected final int port;

  public TelnetBootstrap(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void start() {
    start(consumer -> new TelnetSession(consumer) {

      @Override
      protected void onOpen() {
        System.out.println("New client");
      }

      @Override
      protected void onClose() {
        System.out.println("Client closed");
      }

      @Override
      protected void onSize(int width, int height) {
        System.out.println("Resize:(" + width + "," + height + ")");
      }

      @Override
      protected void onTerminalType(String terminalType) {
        System.out.println("Terminal type: " + terminalType);
      }

      @Override
      protected void onNAWS(boolean naws) {
        System.out.println("Option NAWS:" + naws);
      }

      @Override
      protected void onEcho(boolean echo) {
        System.out.println("Option echo:" + echo);
      }

      @Override
      protected void onSGA(boolean sga) {
        System.out.println("Option SGA:" + sga);
      }

      @Override
      protected void onChar(int c) {
        if (c >= 32) {
          System.out.println("Char:" + (char)c);
        } else {
          System.out.println("Char:<" + c + ">");
        }
      }

      @Override
      protected void onOptionWill(byte optionCode) {
        System.out.println("Will:" + optionCode);
      }

      @Override
      protected void onOptionWont(byte optionCode) {
        System.out.println("Wont:" + optionCode);
      }

      @Override
      protected void onOptionDo(byte optionCode) {
        System.out.println("Do:" + optionCode);
      }

      @Override
      protected void onOptionDont(byte optionCode) {
        System.out.println("Dont:" + optionCode);
      }

      @Override
      protected void onCommand(byte command) {
        System.out.println("Command:" + command);
      }
    });
  }

  public abstract void start(Function<Consumer<byte[]>, TelnetSession> factory);
}
