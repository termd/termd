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

package io.termd.core.ssh.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.termd.core.ssh.TtyCommand;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.netty.NettyIoServiceFactoryFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettySshTtyBootstrap {

  private String host;
  private int port;
  private Charset charset;
  private EventLoopGroup parentGroup;
  private EventLoopGroup childGroup;
  private SshServer server;
  private KeyPairProvider keyPairProvider;
  private PasswordAuthenticator passwordAuthenticator;

  public NettySshTtyBootstrap() {
    this.host = "localhost";
    this.port = 5000;
    this.charset = StandardCharsets.UTF_8;
    this.parentGroup = new NioEventLoopGroup(1);
    this.childGroup = new NioEventLoopGroup();
    this.keyPairProvider = new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath());
    this.passwordAuthenticator = (username, password, session) -> true;
  }

  public String getHost() {
    return host;
  }

  public NettySshTtyBootstrap setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public NettySshTtyBootstrap setPort(int port) {
    this.port = port;
    return this;
  }

  public CompletableFuture<Void> start(Consumer<TtyConnection> handler) throws Exception {
    CompletableFuture<Void> fut = new CompletableFuture<>();
    start(handler, io.termd.core.util.Helper.startedHandler(fut));
    return fut;
  }

  public KeyPairProvider getKeyPairProvider() {
    return keyPairProvider;
  }

  public NettySshTtyBootstrap setKeyPairProvider(KeyPairProvider keyPairProvider) {
    this.keyPairProvider = keyPairProvider;
    return this;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  public void start(Consumer<TtyConnection> factory, Consumer<Throwable> doneHandler) {
    server = SshServer.setUpDefaultServer();
    server.setIoServiceFactoryFactory(new NettyIoServiceFactoryFactory(childGroup));
    server.setPort(port);
    server.setHost(host);
    server.setKeyPairProvider(keyPairProvider);
    server.setPasswordAuthenticator(passwordAuthenticator);
    server.setShellFactory(channelSession -> new TtyCommand(charset, factory));
    try {
      server.start();
    } catch (Exception e) {
      doneHandler.accept(e);
      return;
    }
    doneHandler.accept(null);
  }

  public CompletableFuture<Void> stop() throws InterruptedException {
    CompletableFuture<Void> fut = new CompletableFuture<>();
    stop(Helper.stoppedHandler(fut));
    return fut;
  }

  public void stop(Consumer<Throwable> doneHandler) {
    if (server != null) {
      try {
        server.stop();
      } catch (IOException e) {
        doneHandler.accept(e);
        return;
      }
      doneHandler.accept(null);
    } else {
      doneHandler.accept(new IllegalStateException("Server not started"));
    }
  }
}
