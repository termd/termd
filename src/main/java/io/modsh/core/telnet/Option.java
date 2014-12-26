package io.modsh.core.telnet;

import io.modsh.core.io.BinaryDecoder;
import io.modsh.core.io.BinaryEncoder;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public enum Option {

  BINARY((byte) 0) {

    @Override
    void handleDo(TelnetSession session) {
      session.onSendBinary(true);
    }

    @Override
    void handleDont(TelnetSession session) {
      session.onSendBinary(false);
    }

    @Override
    void handleWill(TelnetSession session) {
      session.onReceiveBinary(true);
    }

    @Override
    void handleWont(TelnetSession session) {
      session.onReceiveBinary(false);
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
      session.output.accept(new byte[]{TelnetSession.BYTE_IAC, TelnetSession.BYTE_SB, code, BYTE_SEND, TelnetSession.BYTE_IAC, TelnetSession.BYTE_SE});
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
