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
 * A telnet option.
 *
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public enum Option {

  /**
   * Telnet Binary Transmission (<a href="https://tools.ietf.org/html/rfc856">RFC856</a>).
   */
  BINARY((byte) 0) {

    @Override
    void handleDo(TelnetConnection session) {
      session.sendBinary = true;
      session.handler.onSendBinary(true);
    }

    @Override
    void handleDont(TelnetConnection session) {
      session.sendBinary = false;
      session.handler.onSendBinary(false);
    }

    @Override
    void handleWill(TelnetConnection session) {
      session.receiveBinary = true;
      session.handler.onReceiveBinary(true);
    }

    @Override
    void handleWont(TelnetConnection session) {
      session.receiveBinary = false;
      session.handler.onReceiveBinary(false);
    }
  },

  /**
   * Telnet Echo Option (<a href="https://tools.ietf.org/html/rfc857">RFC857</a>).
   */
  ECHO((byte) 1) {
    @Override
    void handleDo(TelnetConnection session) { session.handler.onEcho(true); }
    void handleDont(TelnetConnection session) { session.handler.onEcho(false); }
  },

  /**
   * Telnet Suppress Go Ahead Option (<a href="https://tools.ietf.org/html/rfc858">RFC858</a>).
   */
  SGA((byte) 3) {
    void handleDo(TelnetConnection session) { session.handler.onSGA(true); }
    void handleDont(TelnetConnection session) { session.handler.onSGA(false); }
  },

  /**
   * Telnet Terminal Type Option (<a href="https://tools.ietf.org/html/rfc884">RFC884</a>).
   */
  TERMINAL_TYPE((byte) 24) {

    final byte BYTE_IS = 0, BYTE_SEND = 1;

    @Override
    void handleWill(TelnetConnection session) {
      session.send(new byte[]{TelnetConnection.BYTE_IAC, TelnetConnection.BYTE_SB, code, BYTE_SEND, TelnetConnection.BYTE_IAC, TelnetConnection.BYTE_SE});
    }
    @Override
    void handleWont(TelnetConnection session) {
    }

    @Override
    void handleParameters(TelnetConnection session, byte[] parameters) {
      if (parameters.length > 0 && parameters[0] == BYTE_IS) {
        String terminalType = new String(parameters, 1, parameters.length - 1);
        session.handler.onTerminalType(terminalType);
      }
    }
  },

  /**
   * Telnet Window Size Option (<a href="https://www.ietf.org/rfc/rfc1073.txt">RFC1073</a>).
   */
  NAWS((byte) 31) {
    @Override
    void handleWill(TelnetConnection session) {
      session.handler.onNAWS(true);
    }
    @Override
    void handleWont(TelnetConnection session) {
      session.handler.onNAWS(false);
    }
    @Override
    void handleParameters(TelnetConnection session, byte[] parameters) {
      if (parameters.length == 4) {
        int width = ((parameters[0] & 0xff) << 8) + (parameters[1] & 0xff);
        int height = ((parameters[2] & 0xff) << 8) + (parameters[3] & 0xff);
        session.handler.onSize(width, height);
      }
    }
  }

  ;

  /**
   * The option code.
   */
  final byte code;

  Option(byte code) {
    this.code = code;
  }

  /**
   * Handle a <code>DO</code> message.
   *
   * @param session the session
   */
  void handleDo(TelnetConnection session) { }

  /**
   * Handle a <code>DON'T</code> message.
   *
   * @param session the session
   */
  void handleDont(TelnetConnection session) { }

  /**
   * Handle a <code>WILL</code> message.
   *
   * @param session the session
   */
  void handleWill(TelnetConnection session) { }

  /**
   * Handle a <code>WON'T</code> message.
   *
   * @param session the session
   */
  void handleWont(TelnetConnection session) { }

  /**
   * Handle a parameters message.
   *
   * @param session the session
   * @param parameters the parameters
   */
  void handleParameters(TelnetConnection session, byte[] parameters) { }

}
