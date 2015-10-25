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

import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetClientRule extends ExternalResource {

  private Socket socket;
  private OutputStream directOutput;
  public TelnetClient client;

  public String assertReadString(int length) throws Exception {
    return new String(assertReadBytes(length), 0, length, "UTF-8");
  }

  private void checkNotConnected() {
    if (client.isConnected()) {
      throw TestBase.failure("Already connected");
    }
  }

  public TelnetClientRule registerNotifHandler(BiConsumer<Integer, Integer> handler) {
    checkNotConnected();
    client.registerNotifHandler(handler::accept);
    return this;
  }

  public void setOptionHandler(TelnetOptionHandler handler) throws IOException, InvalidTelnetOptionException {
    checkNotConnected();
    client.addOptionHandler(handler);
  }

  public TelnetClientRule connect(String host, int port) throws IOException {
    checkNotConnected();
    client.connect(host, port);
    return this;
  }

  public void disconnect() throws IOException {
    disconnect(true);
  }

  /**
   * Check if the client is disconnected, this affects the input stream of the socket by reading bytes from it.
   *
   * @return if the client is disconnected
   */
  public boolean checkDisconnected() {
    try {
      return socket != null && socket.getInputStream().read() == -1;
    } catch (IOException e) {
      throw TestBase.failure(e);
    }
  }

  public void disconnect(boolean clean) throws IOException {
    if (client.isConnected()) {
      if (clean) {
        client.disconnect();
      } else {
        socket.close();
      }
    }
  }

  public byte[] assertReadBytes(int length) throws Exception {
    byte[] bytes = new byte[length];
    while (length > 0) {
      int i = client.getInputStream().read(bytes, bytes.length - length, length);
      if (i == -1) {
        throw TestBase.failure("Closed");
      }
      length -= i;
    }
    return bytes;
  }

  public TelnetClientRule write(byte... bytes) throws IOException {
    client.getOutputStream().write(bytes);
    return this;
  }

  public TelnetClientRule flush() throws IOException {
    client.getOutputStream().flush();
    return this;
  }

  public void writeDirect(byte... bytes) throws IOException {
    synchronized (client) {
      directOutput.write(bytes);
    }
  }

  public void writeDirectAndFlush(byte... bytes) throws IOException {
    synchronized (client) {
      directOutput.write(bytes);
      directOutput.flush();
    }
  }

  @Override
  protected void before() throws Throwable {
    client = new TelnetClient() {
      @Override
      protected void _connectAction_() throws IOException {
        super._connectAction_();
        socket = _socket_;
        directOutput = _output_;
        setKeepAlive(false);
      }
    };
  }

  @Override
  protected void after() {
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (Exception ignore) {
      } finally {
        client = null;
      }
    }
  }
}
