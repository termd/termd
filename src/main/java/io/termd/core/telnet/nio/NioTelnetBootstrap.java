package io.termd.core.telnet.nio;

import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Telnet bootstrap implemented with NIO channels.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NioTelnetBootstrap extends TelnetBootstrap {

  public static void main(String[] args) {
    new NioTelnetBootstrap().setPort(8080).setHost("localhost").start(null, err -> {
      err.printStackTrace();
    });
  }

  private AtomicBoolean closed = new AtomicBoolean(false);
  private Selector selector;
  private ServerSocketChannel serverChannel;
  private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
  private ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();
  private Map<SocketChannel, NioTelnetConnection> connectionMap = new HashMap<>();
  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private CompletableFuture<Void> closeFuture = new CompletableFuture<>();

  @Override
  public void start(Supplier<TelnetHandler> factory, Consumer<Throwable> doneHandler) {
    new Thread() {
      @Override
      public void run() {
        try {
          bind();
        } catch (Exception e) {
          doneHandler.accept(e);
          return;
        }
        doneHandler.accept(null);
        try {
          NioTelnetBootstrap.this.run(factory);
        } finally {
          close();
        }
      }
    }.start();
  }

  public void bind() throws Exception {
    selector = SelectorProvider.provider().openSelector();
    try {
      serverChannel = ServerSocketChannel.open();
      serverChannel.configureBlocking(false);
      serverChannel.socket().bind(new InetSocketAddress(InetAddress.getByName(getHost()), getPort()));
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (Exception e1) {
      selector.close();
      throw e1;
    }
  }

  public void run(Supplier<TelnetHandler> factory) {

    while (!closed.get()) {

      // Process the tasks
      while (tasks.size() > 0) {
        Runnable task = tasks.removeFirst();
        task.run();
      }

      // Check writes
      for (NioTelnetConnection conn : connectionMap.values()) {
        synchronized (conn) {
          SelectionKey key = conn.channel.keyFor(this.selector);
          if (conn.pendingWrites.size() > 0 && key.interestOps() != SelectionKey.OP_WRITE) {
            key.interestOps(SelectionKey.OP_WRITE);
          }
        }
      }

      try {
        selector.select();
        Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
        while (selectedKeys.hasNext()) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();
          if (key.isValid()) {
            if (key.isAcceptable()) {
              SocketChannel socketChannel = serverChannel.accept();
              socketChannel.configureBlocking(false);
              socketChannel.register(selector, SelectionKey.OP_READ);
              NioTelnetConnection conn = new NioTelnetConnection(factory.get(), Thread.currentThread(), key, socketChannel);
              connectionMap.put(socketChannel, conn);
              conn.onInit();
            } else if (key.isReadable()) {
              SocketChannel socketChannel = (SocketChannel) key.channel();
              NioTelnetConnection conn = connectionMap.get(socketChannel);
              if (conn != null) {
                readBuffer.clear();
                int len;
                try {
                  len = socketChannel.read(this.readBuffer);
                } catch (IOException e) {
                  conn.doClose();
                  continue;
                }
                if (len == -1) {
                  conn.doClose();
                  continue;
                }
                byte[] bytes = readBuffer.array();
                conn.receive(bytes, len);
              }
            } else if (key.isWritable()) {
              SocketChannel socketChannel = (SocketChannel) key.channel();
              NioTelnetConnection conn = connectionMap.get(socketChannel);
              if (conn != null) {
                synchronized (conn) {
                  while (conn.pendingWrites.size() > 0) {
                    ByteBuffer buffer = conn.pendingWrites.peekFirst();
                    conn.channel.write(buffer);
                    if (buffer.remaining() > 0) {
                      break;
                    }
                    conn.pendingWrites.removeFirst();
                  }
                  if (conn.pendingWrites.isEmpty()) {
                    key.interestOps(SelectionKey.OP_READ);
                  }
                }
              }
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void close() {
    try {
      new ArrayList<>(connectionMap.values()).forEach(NioTelnetConnection::doClose);
      try {
        serverChannel.socket().close();
      } catch (Exception ignore) {
      }
      try {
        serverChannel.close();
      } catch (Exception ignore) {
      }
      try {
        selector.close();
      } catch (Exception ignore) {
      }
      executor.shutdown();
    } finally {
      closeFuture.complete(null);
    }
  }

  @Override
  public void stop(Consumer<Throwable> doneHandler) {
    closed.compareAndSet(false, true);
    selector.wakeup();
    closeFuture.thenAccept(v -> {
      doneHandler.accept(null);
    });
  }

  class NioTelnetConnection extends TelnetConnection {

    final SelectionKey key;
    final SocketChannel channel;
    final Thread ioThread;
    final ArrayDeque<ByteBuffer> pendingWrites = new ArrayDeque<>();
    final AtomicBoolean wantClose = new AtomicBoolean();

    public NioTelnetConnection(TelnetHandler handler, Thread ioThread, SelectionKey key, SocketChannel channel) {
      super(handler);
      this.key = key;
      this.ioThread = ioThread;
      this.channel = channel;
    }

    void receive(byte[] bytes, int len) {
      receive(Arrays.copyOf(bytes, len));
    }

    @Override
    public void close() {
      if (wantClose.compareAndSet(false, true)) {
        tasks.add(this::doClose);
        if (Thread.currentThread() != ioThread) {
          selector.wakeup();
        }
      }
    }

    private void doClose() {
      if (connectionMap.remove(channel) != null) {
        key.cancel();
        try {
          key.channel().close();
        } catch (Exception ignore) {
        }
        onClose();
      }
    }

    @Override
    protected void execute(Runnable task) {
      if (!wantClose.get()) {
        tasks.add(task);
        if (Thread.currentThread() != ioThread) {
          selector.wakeup();
        }
      }
    }

    @Override
    protected void schedule(Runnable task, long delay, TimeUnit unit) {
      if (!wantClose.get()) {
        executor.schedule(() -> {
          tasks.add(task);
          selector.wakeup();
        }, delay, unit);
      }
    }

    @Override
    protected void send(byte[] data) {
      if (!wantClose.get() && data.length > 0) {
        synchronized (this) {
          pendingWrites.addLast(ByteBuffer.wrap(data.clone()));
        }
        if (Thread.currentThread() != ioThread) {
          selector.wakeup();
        }
      }
    }
  }
}
