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

package io.termd.core.tty;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import io.termd.core.TestBase;
import io.termd.core.ssh.SshSession;
import io.termd.core.ssh.TtyCommand;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class SshTtyTestBase extends TtyTestBase {

  private SshSession session;

  @Override
  protected void assertConnect(String term) throws Exception {
    if (session != null) {
      throw failure("Already a session");
    }
    session = new SshSession();
    session.termType(term);
    session.connect();
  }

  @Override
  public boolean checkDisconnected() {
    return session.checkDisconnect();
  }

  @Override
  protected void assertDisconnect(boolean clean) throws Exception {
    if (clean) {
      session.disconnect();
    } else {
      throw new UnsupportedOperationException("Must use a proxy");
    }
  }

  @Override
  protected void resize(int width, int height) {
//    channel.setPtyWidth(width * 8);
//    channel.setPtyHeight(height * 8);
//    channel.setPtyColumns(width);
//    channel.setPtyLines(height);
    throw new UnsupportedOperationException();
  }

  @Override
  protected void assertWrite(String s) throws Exception {
    session.write(s.getBytes(charset));
  }

  @Override
  protected String assertReadString(int len) throws Exception {
    byte[] buf = session.read(len);
    return new String(buf, charset);
  }

  @Override
  protected void assertWriteln(String s) throws Exception {
    assertWrite((s + "\r"));
  }

  private SshServer sshd;

  protected abstract SshServer createServer();

  protected TtyCommand createConnection(Consumer<TtyConnection> onConnect) {
    return new TtyCommand(charset, onConnect);
  }

  @Override
  protected void server(Consumer<TtyConnection> onConnect) {
    if (sshd != null) {
      throw failure("Already a server");
    }
    try {
      sshd = createServer();
      sshd.setPort(5000);
      sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
      sshd.setPasswordAuthenticator((username, password, session) -> true);
      sshd.setShellFactory(channel -> createConnection(onConnect));
      sshd.start();
    } catch (Exception e) {
      throw failure(e);
    }
  }

  @Before
  public void before() {
    sshd = null;
    session = null;
  }

  @Test
  public void testExitCode() throws Exception {
    server(conn -> {
      conn.setStdinHandler(bytes -> {
        conn.close(25);
      });
    });
    assertConnect();
    assertWrite("whatever");
    long timeout = System.currentTimeMillis() + 5000;
    while (!session.isClosed()) {
      assertTrue(System.currentTimeMillis() < timeout);
      Thread.sleep(10);
    }
    assertEquals(25, session.exitStatus());
  }

  @After
  public void after() throws Exception {
    if (session != null) {
      session.close();
      session = null;
    }
    if (sshd != null && !sshd.isClosed()) {
      try {
        sshd.close();
      } catch (Exception ignore) {
      }
    }
  }

  @Override
  public void testResize() throws Exception {
    // Cannot be tested with this client that does not support resize
  }
}
