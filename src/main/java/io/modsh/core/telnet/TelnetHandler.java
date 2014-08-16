package io.modsh.core.telnet;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler implements Handler<NetSocket> {

  static Charset UTF_8 = Charset.forName("UTF-8");

  @Override
  public void handle(NetSocket socket) {


    Session session = new Session(socket);
    socket.dataHandler(session);
    socket.closeHandler(v -> session.destroy());
    session.init();


  }

  static final byte BYTE_IAC = (byte) 255, BYTE_DONT = (byte) 254, BYTE_DO = (byte) 253, BYTE_WONT = (byte) 252,
      BYTE_WILL = (byte) 251, BYTE_SB = (byte) 250, BYTE_SE = (byte) 240;

  private class Session implements Handler<Buffer> {

    final NetSocket socket;
    Status status;
    HashMap<Byte, Boolean> options;
    Byte paramsOptionCode;
    Buffer paramsBuffer;
    boolean paramsIac;
    boolean echo;
    boolean sga;
    CharsetEncoder encoder;
    CharsetDecoder decoder;
    ByteBuffer bBuf;
    CharBuffer cBuf;

    private Session(NetSocket socket) {
      this.socket = socket;
      this.status = Status.DATA;
      this.options = new HashMap<>();
      this.echo = false;
      this.sga = false;
      this.paramsOptionCode = null;
      this.paramsBuffer = null;
      this.paramsIac = false;
      this.encoder = null;
      this.decoder = null;
    }

    void init() {
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_WILL, Option.ECHO.code}));
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_WILL, Option.SGA.code}));
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_DO, Option.NAWS.code}));
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_DO, Option.NAWS.code}));
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_DO, Option.BINARY.code}));
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_WILL, Option.BINARY.code}));
      socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_DO, Option.TERMINAL_TYPE.code}));
    }

    @Override
    public void handle(Buffer data) {
      for (int i = 0;i < data.length();i++) {
        status.handle(this, data.getByte(i));
      }
    }

    void onSize(int width, int height) {
      System.out.println("Size changed " + width + " " + height);
    }

    void onTerminalType(String terminalType) {
      System.out.println("Terminal type " + terminalType);
    }

    void onByte(byte b) {
      System.out.println("on byte " + b);
      if (decoder != null) {
        bBuf.put(b);
        bBuf.flip();
        decoder.decode(bBuf, cBuf, false);
        cBuf.flip();
        while (cBuf.hasRemaining()) {
          char c = cBuf.get();
          onChar(c);
        }
        bBuf.compact();
        cBuf.compact();
      } else {
        onChar((char) b);
      }
    }

    void onChar(char c) {
      System.out.println("Should handle char " + c);
    }

    void destroy() {
    }
  }

  private enum Option {

    BINARY((byte) 0) {

      @Override
      void handleDo(Session session) {
        session.encoder = UTF_8.newEncoder();
      }

      @Override
      void handleWill(Session session) {
        session.decoder = UTF_8.newDecoder();
        session.bBuf = ByteBuffer.allocate(4);
        session.cBuf = CharBuffer.allocate(1);
      }
    },

    ECHO((byte) 1) {
      @Override
      void handleDo(Session session) { session.echo = true; }
      void handleDont(Session session) { session.echo = false; }
    },

    SGA((byte) 3) {
      void handleDo(Session session) { session.sga = true; }
      void handleDont(Session session) { session.sga = false; }
    },

    TERMINAL_TYPE((byte) 24) {

      final byte BYTE_IS = 0, BYTE_SEND = 1;

      @Override
      void handleWill(Session session) {
        session.socket.write(new Buffer(new byte[]{BYTE_IAC, BYTE_SB, code, BYTE_SEND, BYTE_IAC, BYTE_SE}));
      }
      @Override
      void handleWont(Session session) {
      }

      @Override
      void handleParameters(Session session, Buffer parameters) {
        if (parameters.length() > 0 && parameters.getByte(0) == BYTE_IS) {
          String terminalType = new String(parameters.getBytes(1, parameters.length()));
          session.onTerminalType(terminalType);
        }
      }
    },

    NAWS((byte) 31) {
      @Override
      void handleWill(Session session) {
      }
      @Override
      void handleWont(Session session) {
      }
      @Override
      void handleParameters(Session session, Buffer parameters) {
        if (parameters.length() == 4) {
          int width = parameters.getShort(0);
          int height = parameters.getShort(2);
          session.onSize(width, height);
        }
      }
    }

    ;

    final byte code;

    Option(byte code) {
      this.code = code;
    }

    void handleDo(Session session) { System.out.println("Option " + name() + " handleDo not implemented "); }
    void handleDont(Session session) { System.out.println("Option " + name() + " handleDont not implemented "); }
    void handleWill(Session session) { System.out.println("Option " + name() + " handleWill not implemented "); }
    void handleWont(Session session) { System.out.println("Option " + name() + " handleWont not implemented "); }
    void handleParameters(Session session, Buffer parameters) { System.out.println("Option " + name() + " handlerParameters not implemented " + Arrays.toString(parameters.getBytes())); }

  }

  private enum Status {

    DATA() {
      @Override
      void handle(Session session, byte b) {
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
      void handle(Session session, byte b) {
        if (b == BYTE_IAC) {
          session.onByte((byte) - 1);
        } else {
          IAC.handle(session, b);
        }
      }
    },

    IAC() {
      @Override
      void handle(Session session, byte b) {
        if (b == BYTE_DO) {
          session.status = DO;
        } else if (b == BYTE_DONT) {
          session.status = DONT;
        } else if (b == BYTE_WILL) {
          session.status = WILL;
        } else if (b == BYTE_WONT) {
          session.status = WONT;
        } else if (b == BYTE_SB) {
          session.paramsBuffer = new Buffer(100);
          session.status = SB;
        } else {
          super.handle(session, b);
        }
      }
    },

    SB() {
      @Override
      void handle(Session session, byte b) {
        if (session.paramsOptionCode == null) {
          session.paramsOptionCode = b;
        } else {
          if (session.paramsIac) {
            session.paramsIac = false;
            if (b == BYTE_SE) {
              try {
                for (Option option : Option.values()) {
                  if (option.code == session.paramsOptionCode) {
                    option.handleParameters(session, session.paramsBuffer);
                    return;
                  }
                }
                System.out.println("No option " + session.paramsOptionCode + " for parameters " + Arrays.toString(session.paramsBuffer.getBytes()));
              } finally {
                session.paramsOptionCode = null;
                session.paramsBuffer = null;
                session.status = DATA;
              }
            } else if (b == BYTE_IAC) {
              session.paramsBuffer.appendByte((byte) -1);
            }
          } else {
            if (b == BYTE_IAC) {
              session.paramsIac = true;
            } else {
              session.paramsBuffer.appendByte(b);
            }
          }
        }
      }
    },

    DO() {
      @Override
      void handle(Session session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleDo(session);
              return;
            }
          }
          session.handle(new Buffer(new byte[]{BYTE_IAC,BYTE_WONT,b}));
        } finally {
          session.status = DATA;
        }
      }
    },

    DONT() {
      @Override
      void handle(Session session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleDont(session);
              return;
            }
          }
        } finally {
          session.status = DATA;
        }
      }
    },

    WILL() {
      @Override
      void handle(Session session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleWill(session);
              return;
            }
          }
          session.handle(new Buffer(new byte[]{BYTE_IAC,BYTE_DONT,b}));
        } finally {
          session.status = DATA;
        }
      }
    },

    WONT() {
      @Override
      void handle(Session session, byte b) {
        try {
          for (Option option : Option.values()) {
            if (option.code == b) {
              option.handleWont(session);
              return;
            }
          }
        } finally {
          session.status = DATA;
        }
      }
    },

    ;

    void handle(Session session, byte b) {
      System.out.println(name() + ":" + b + " not implemented");
    }
  }
}
