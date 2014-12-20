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

import io.modsh.core.io.BinaryDecoder;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class TelnetSession implements Consumer<byte[]> {

  static final byte BYTE_IAC = (byte)  0xFF;
  static final byte BYTE_DONT = (byte) 0xFE;
  static final byte BYTE_DO = (byte)   0xFD;
  static final byte BYTE_WONT = (byte) 0xFC;
  static final byte BYTE_WILL = (byte) 0xFB;
  static final byte BYTE_SB = (byte)   0xFA;
  static final byte BYTE_SE = (byte)   0xF0;
  static Charset UTF_8 = Charset.forName("UTF-8");
  Status status;
  HashMap<Byte, Boolean> options;
  Byte paramsOptionCode;
  byte[] paramsBuffer;
  int paramsLength;
  boolean paramsIac;
  CharsetEncoder encoder;
  BinaryDecoder decoder;
  final Consumer<byte[]> output;

  public TelnetSession(Consumer<byte[]> output) {
    this.status = Status.DATA;
    this.options = new HashMap<>();
    this.paramsOptionCode = null;
    this.paramsBuffer = null;
    this.paramsIac = false;
    this.encoder = null;
    this.decoder = null;
    this.output = output;
  }

  private void appendToParams(byte b) {
    while (paramsLength >= paramsBuffer.length) {
      paramsBuffer = Arrays.copyOf(paramsBuffer, paramsBuffer.length + 100);
    }
    paramsBuffer[paramsLength++] = b;
  }

  public void init() {
    output.accept(new byte[]{BYTE_IAC, BYTE_WILL, Option.ECHO.code});
    output.accept(new byte[]{BYTE_IAC, BYTE_WILL, Option.SGA.code});
    output.accept(new byte[]{BYTE_IAC, BYTE_DO, Option.NAWS.code});
    output.accept(new byte[]{BYTE_IAC, BYTE_DO, Option.BINARY.code});
    output.accept(new byte[]{BYTE_IAC, BYTE_WILL, Option.BINARY.code});
    output.accept(new byte[]{BYTE_IAC, BYTE_DO, Option.TERMINAL_TYPE.code});
    onOpen();
  }

  @Override
  public void accept(byte[] data) {
    for (int i = 0;i < data.length;i++) {
      status.handle(this, data[i]);
    }
  }

  protected void onByte(byte b) {
    if (decoder != null) {
      decoder.onByte(b);
    } else {
      onChar((char) b);
    }
  }

  public void close() {
    onClose();
  }

  protected void onOpen() {}
  protected void onClose() {}
  protected void onSize(int width, int height) {}
  protected void onTerminalType(String terminalType) {}
  protected void onCommand(byte command) {}
  protected void onNAWS(boolean naws) {}
  protected void onEcho(boolean echo) {}
  protected void onSGA(boolean sga) {}
  protected void onChar(int c) {}
  protected void onOptionWill(byte optionCode) {}
  protected void onOptionWont(byte optionCode) {}
  protected void onOptionDo(byte optionCode) {}
  protected void onOptionDont(byte optionCode) {}
  protected void onOptionParameters(byte optionCode, byte[] parameters) {}

  enum Option {

    BINARY((byte) 0) {

      @Override
      void handleDo(TelnetSession session) {
        session.encoder = UTF_8.newEncoder();
      }

      @Override
      void handleWill(TelnetSession session) {
        session.decoder = new BinaryDecoder(UTF_8, (int c) -> session.onChar((char) c));
      }
    },

    ECHO((byte) 1) {
      @Override
      void handleDo(TelnetSession session) { session.onEcho(true); }
      void handleDont(TelnetSession session) { session.onEcho(false); }
    },

    SGA((byte) 3) {
      void handleDo(TelnetSession session) { session.onSGA(true); }
      void handleDont(TelnetSession session) { session.onSGA(false); }
    },

    TERMINAL_TYPE((byte) 24) {

      final byte BYTE_IS = 0, BYTE_SEND = 1;

      @Override
      void handleWill(TelnetSession session) {
        session.output.accept(new byte[]{BYTE_IAC, BYTE_SB, code, BYTE_SEND, BYTE_IAC, BYTE_SE});
      }
      @Override
      void handleWont(TelnetSession session) {
      }

      @Override
      void handleParameters(TelnetSession session, byte[] parameters) {
        if (parameters.length > 0 && parameters[0] == BYTE_IS) {
          String terminalType = new String(parameters, 1, parameters.length - 1);
          session.onTerminalType(terminalType);
        }
      }
    },

    NAWS((byte) 31) {
      @Override
      void handleWill(TelnetSession session) {
        session.onNAWS(true);
      }
      @Override
      void handleWont(TelnetSession session) {
        session.onNAWS(false);
      }
      @Override
      void handleParameters(TelnetSession session, byte[] parameters) {
        if (parameters.length == 4) {
          int width = (parameters[0] << 8) + parameters[1];
          int height = (parameters[2] << 8) + parameters[3];
          session.onSize(width, height);
        }
      }
    }

    ;

    final byte code;

    Option(byte code) {
      this.code = code;
    }

    void handleDo(TelnetSession session) { }
    void handleDont(TelnetSession session) { }
    void handleWill(TelnetSession session) { }
    void handleWont(TelnetSession session) { }
    void handleParameters(TelnetSession session, byte[] parameters) { }

  }

  enum Status {

    DATA() {
      @Override
      void handle(TelnetSession session, byte b) {
        if (b == BYTE_IAC) {
          session.status = session.decoder == null ? IAC : ESC;
        } else {
          if (session.decoder == null) {
            if ((b & 0x80) != 0) {
              System.out.println("Unimplemented " + b);
            } else {
              session.onByte(b);
            }
          } else {
            session.onByte(b);
          }
        }
      }
    },

    ESC() {
      @Override
      void handle(TelnetSession session, byte b) {
        if (b == BYTE_IAC) {
          session.onByte((byte) - 1);
        } else {
          IAC.handle(session, b);
        }
      }
    },

    IAC() {
      @Override
      void handle(TelnetSession session, byte b) {
        if (b == BYTE_DO) {
          session.status = DO;
        } else if (b == BYTE_DONT) {
          session.status = DONT;
        } else if (b == BYTE_WILL) {
          session.status = WILL;
        } else if (b == BYTE_WONT) {
          session.status = WONT;
        } else if (b == BYTE_SB) {
          session.paramsBuffer = new byte[100];
          session.paramsLength = 0;
          session.status = SB;
        } else {
          session.onCommand(b);
          session.status = DATA;
        }
      }
    },

    SB() {
      @Override
      void handle(TelnetSession session, byte b) {
        if (session.paramsOptionCode == null) {
          session.paramsOptionCode = b;
        } else {
          if (session.paramsIac) {
            session.paramsIac = false;
            if (b == BYTE_SE) {
              try {
                for (Option option : Option.values()) {
                  if (option.code == session.paramsOptionCode) {
                    option.handleParameters(session, Arrays.copyOf(session.paramsBuffer, session.paramsLength));
                    return;
                  }
                }
                session.onOptionParameters(session.paramsOptionCode, Arrays.copyOf(session.paramsBuffer, session.paramsLength));
              } finally {
                session.paramsOptionCode = null;
                session.paramsBuffer = null;
                session.status = DATA;
              }
            } else if (b == BYTE_IAC) {
              session.appendToParams((byte) -1);
            }
          } else {
            if (b == BYTE_IAC) {
              session.paramsIac = true;
            } else {
              session.appendToParams(b);
            }
          }
        }
      }
    },

    DO() {
      @Override
      void handle(TelnetSession session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleDo(session);
              return;
            }
          }
          session.onOptionDo(b);
          session.accept(new byte[]{BYTE_IAC,BYTE_WONT,b});
        } finally {
          session.status = DATA;
        }
      }
    },

    DONT() {
      @Override
      void handle(TelnetSession session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleDont(session);
              return;
            }
          }
          session.onOptionDont(b);
        } finally {
          session.status = DATA;
        }
      }
    },

    WILL() {
      @Override
      void handle(TelnetSession session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleWill(session);
              return;
            }
          }
          session.onOptionWill(b);
          session.accept(new byte[]{BYTE_IAC,BYTE_DONT,b});
        } finally {
          session.status = DATA;
        }
      }
    },

    WONT() {
      @Override
      void handle(TelnetSession session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleWont(session);
              return;
            }
          }
          session.onOptionWont(b);
        } finally {
          session.status = DATA;
        }
      }
    },

    ;

    abstract void handle(TelnetSession session, byte b);
  }
}
