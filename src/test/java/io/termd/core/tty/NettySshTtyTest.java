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

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.termd.core.ssh.TtyCommand;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.netty.NettyIoServiceFactoryFactory;
import org.apache.sshd.netty.NettyIoSession;
import org.apache.sshd.server.SshServer;
import org.junit.After;
import org.junit.Before;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettySshTtyTest extends SshTtyTestBase {

  private EventLoopGroup eventLoopGroup;

  @Before
  public void before() {
    eventLoopGroup = new NioEventLoopGroup();
  }

  @After
  public void after() throws Exception {
    eventLoopGroup.shutdownGracefully();
  }

  @Override
  protected SshServer createServer() {
    SshServer sshd = SshServer.setUpDefaultServer();
    sshd.setIoServiceFactoryFactory(new NettyIoServiceFactoryFactory(eventLoopGroup));
    return sshd;
  }

  @Override
  protected TtyCommand createConnection(Consumer<TtyConnection> onConnect) {
    return new TtyCommand(charset, onConnect) {
      @Override
      public void execute(Runnable task) {
        // Need this trick now since we cannot get the event loop from the session
        for (EventExecutor eventExecutor : eventLoopGroup) {
            EventLoop el = (EventLoop) eventExecutor;
            if (el.inEventLoop()) {
                el.execute(task);
                break;
            }
        }
      }
    };
  }

  @Override
  protected void assertThreading(Thread connThread, Thread schedulerThread) throws Exception {
    assertTrue(connThread.getName().startsWith("nioEventLoopGroup"));
    assertEquals(connThread, schedulerThread);
  }
}
