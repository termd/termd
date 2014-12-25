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

import io.modsh.core.telnet.vertx.TelnetHandler;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;
import org.apache.mina.util.byteaccess.ByteArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.net.NetServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * See <a href="http://commons.apache.org/proper/commons-net/examples/telnet/TelnetClientExample.java>for more possibilities</a>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandlerTest extends TestBase {

  private Vertx vertx;
  private NetServer server;
  private TelnetClient client;

  @Before
  public void before() throws InterruptedException {
    vertx = VertxFactory.newVertx();
  }

  private void server(Function<Consumer<byte[]>, TelnetSession> factory) {
    server = vertx.createNetServer().connectHandler(new TelnetHandler(factory));
    BlockingQueue<AsyncResult<NetServer>> latch = new ArrayBlockingQueue<>(1);
    server.listen(4000, "localhost", latch::add);
    AsyncResult<NetServer> result;
    try {
      result = latch.poll(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw failure(e);
    }
    if (result.failed()) {
      throw failure(result.cause());
    }
  }

  @After
  public void after() {
    if (server != null) {
      server.close();
    }
    vertx.stop();
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException ignore) {
      }
    }
  }

  private void testOptionValue(Function<Consumer<byte[]>, TelnetSession> factory, TelnetOptionHandler optionHandler) throws Exception {
    server(factory);
    TelnetClient client = new TelnetClient();
    try {
      client.addOptionHandler(optionHandler);
      client.connect("localhost", 4000);
      await();
    } finally {
      client.disconnect();
    }
  }

  @Test
  public void testRejectEcho() throws Exception {
    AtomicReference<Boolean> serverValue = new AtomicReference<>();
    EchoOptionHandler optionHandler = new EchoOptionHandler(false, false, false, false);
    testOptionValue(socket -> new TelnetSession(socket) {
      @Override
      protected void onEcho(boolean echo) {
        serverValue.set(echo);
        testComplete();
      }
    }, optionHandler);
    assertEquals(false, serverValue.get());
    assertEquals(false, optionHandler.getAcceptRemote());
  }

  @Test
  public void testAcceptEcho() throws Exception {
    AtomicReference<Boolean> serverValue = new AtomicReference<>();
    EchoOptionHandler optionHandler = new EchoOptionHandler(false, false, false, true);
    testOptionValue(socket -> new TelnetSession(socket) {
      @Override
      protected void onEcho(boolean echo) {
        serverValue.set(echo);
        testComplete();
      }
    }, optionHandler);
    assertEquals(true, serverValue.get());
    assertEquals(true, optionHandler.getAcceptRemote());
  }

  @Test
  public void testRejectSGA() throws Exception {
    AtomicReference<Boolean> serverValue = new AtomicReference<>();
    SuppressGAOptionHandler optionHandler = new SuppressGAOptionHandler(false, false, false, false);
    testOptionValue(socket -> new TelnetSession(socket) {
      @Override
      protected void onSGA(boolean sga) {
        serverValue.set(sga);
        testComplete();
      }
    }, optionHandler);
    assertEquals(false, serverValue.get());
    assertEquals(false, optionHandler.getAcceptRemote());
  }

  @Test
  public void testAcceptSGA() throws Exception {
    AtomicReference<Boolean> serverValue = new AtomicReference<>();
    SuppressGAOptionHandler optionHandler = new SuppressGAOptionHandler(false, false, false, true);
    testOptionValue(socket -> new TelnetSession(socket) {
      @Override
      protected void onSGA(boolean sga) {
        serverValue.set(sga);
        testComplete();
      }
    }, optionHandler);
    assertEquals(true, serverValue.get());
    assertEquals(true, optionHandler.getAcceptRemote());
  }

  @Test
  public void testRejectNAWS() throws Exception {
    AtomicReference<Boolean> serverValue = new AtomicReference<>();
    WindowSizeOptionHandler optionHandler = new WindowSizeOptionHandler(20, 10, false, false, false, false);
    testOptionValue(socket -> new TelnetSession(socket) {
      @Override
      protected void onNAWS(boolean naws) {
        serverValue.set(naws);
        testComplete();
      }

      @Override
      protected void onSize(int width, int height) {
        super.onSize(width, height);
      }
    }, optionHandler);
    assertEquals(false, serverValue.get());
    assertEquals(false, optionHandler.getAcceptLocal());
  }

  @Test
  public void testAcceptNAWS() throws Exception {
    AtomicReference<Boolean> serverValue = new AtomicReference<>();
    AtomicReference<int[]> size = new AtomicReference<>();
    WindowSizeOptionHandler optionHandler = new WindowSizeOptionHandler(20, 10, false, false, true, false);
    testOptionValue(socket -> new TelnetSession(socket) {
      @Override
      protected void onNAWS(boolean naws) {
        serverValue.set(naws);
      }
      @Override
      protected void onSize(int width, int height) {
        size.set(new int[]{width, height});
        testComplete();
      }
    }, optionHandler);
    assertEquals(true, serverValue.get());
    assertEquals(true, optionHandler.getAcceptLocal());
    assertEquals(2, size.get().length);
    assertEquals(20, size.get()[0]);
    assertEquals(10, size.get()[1]);
  }

  @Test
  public void testOpen() throws Exception {
    server(socket -> new TelnetSession(socket) {
      @Override
      protected void onOpen() {
        testComplete();
      }
    });
    TelnetClient client = new TelnetClient();
    try {
      client.connect("localhost", 4000);
    } finally {
      client.disconnect();
    }
    await();
  }

  @Test
  public void testClose() throws Exception {
    server(socket -> new TelnetSession(socket) {
      @Override
      protected void onClose() {
        testComplete();
      }
    });
    TelnetClient client = new TelnetClient();
    try {
      client.connect("localhost", 4000);
    } finally {
      client.disconnect();
    }
    await();
  }

  @Test
  public void testWillUnknownOption() throws Exception {
    server(TelnetSession::new);
    TelnetClient client = new TelnetClient();
    client.connect("localhost", 4000);
    client.registerNotifHandler((negotiation_code, option_code) -> {
      if (option_code == 47) {
        assertEquals(TelnetNotificationHandler.RECEIVED_DONT, negotiation_code);
        testComplete();
      }
    });
    client.addOptionHandler(new SimpleOptionHandler(47, true, false, false, false));
    await();
  }

  @Test
  public void testDoUnknownOption() throws Exception {
    server(TelnetSession::new);
    TelnetClient client = new TelnetClient();
    client.connect("localhost", 4000);
    client.registerNotifHandler((negotiation_code, option_code) -> {
      if (option_code == 47) {
        assertEquals(TelnetNotificationHandler.RECEIVED_WONT, negotiation_code);
        testComplete();
      }
    });
    client.addOptionHandler(new SimpleOptionHandler(47, false, true, false, false));
    await();
  }

  // Todo : need to test sending -1
  @Test
  public void testClientSendBinary() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    server(socket -> new TelnetSession(socket) {
      @Override
      protected void onSendOptions() {
        writeDoOption(Option.BINARY);
      }
      @Override
      protected void onOptionWill(byte optionCode) {
        super.onOptionWill(optionCode);
        if (optionCode == 0) {
          latch.countDown();
        }
      }
      @Override
      protected void onOptionDo(byte optionCode) {
        super.onOptionDo(optionCode);
        if (optionCode == 0) {
          fail("Was not expecting a won't for binary option");
        }
      }
      @Override
      protected void onOptionWont(byte optionCode) {
        super.onOptionWont(optionCode);
        if (optionCode == 0) {
          fail("Was not expecting a won't for binary option");
        }
      }
      @Override
      protected void onChar(int c) {
        assertEquals((int)'\u20AC', c);
        testComplete();
      }
    });
    AtomicReference<OutputStream> out = new AtomicReference<>();
    client = new TelnetClient() {
      @Override
      protected void _connectAction_() throws IOException {
        super._connectAction_();
        out.set(_output_);
      }
    };
    client.addOptionHandler(new SimpleOptionHandler(0, false, false, true, false));
    client.connect("localhost", 4000);
    latch.await();
    new OutputStreamWriter(out.get()).append('\u20AC').flush();
    await();
  }

  @Test
  public void testServerSendBinary() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    server(socket -> new TelnetSession(socket) {
      @Override
      protected void onSendOptions() {
        writeWillOption(Option.BINARY);
      }
      @Override
      protected void onOptionWill(byte optionCode) {
        super.onOptionWill(optionCode);
        if (optionCode == 0) {
          fail("Was not expecting a won't for binary option");
        }
      }
      @Override
      protected void onOptionDo(byte optionCode) {
        super.onOptionDo(optionCode);
        if (optionCode == 0) {
          write(new byte[]{'h', 'e', 'l', 'l', 'o', -1});
          latch.countDown();
        }
      }
      @Override
      protected void onOptionDont(byte optionCode) {
        super.onOptionWont(optionCode);
        if (optionCode == 0) {
          fail("Was not expecting a won't for binary option");
        }
      }
    });
    AtomicReference<InputStream> out = new AtomicReference<>();
    client = new TelnetClient() {
      @Override
      protected void _connectAction_() throws IOException {
        super._connectAction_();
        out.set(_input_);
      }
    };
    client.addOptionHandler(new SimpleOptionHandler(0, false, false, false, true));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    client.registerSpyStream(baos);
    client.connect("localhost", 4000);
    latch.await();
    Reader reader = new InputStreamReader(client.getInputStream());
    char[] hello = new char[5];
    int num = reader.read(hello);
    assertEquals(5, num);
    assertEquals("hello", new String(hello));
    byte[] data = baos.toByteArray();
    assertEquals(10, data.length);
    assertEquals((byte)'h', data[3]);
    assertEquals((byte)'e', data[4]);
    assertEquals((byte)'l', data[5]);
    assertEquals((byte)'l', data[6]);
    assertEquals((byte)'o', data[7]);
    assertEquals((byte)-1, data[8]);
    assertEquals((byte)-1, data[9]);
  }
}
