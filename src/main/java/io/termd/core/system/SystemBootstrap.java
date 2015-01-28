package io.termd.core.system;

import io.termd.core.Handler;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.readline.EventQueue;
import io.termd.core.telnet.netty.ReadlineBootstrap;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SystemBootstrap {

  public static void main(String[] args) throws IOException {

    InputStream inputrc = ReadlineBootstrap.class.getResourceAsStream("inputrc");
    final EventQueue eventMapper = new EventQueue(inputrc);
    BinaryDecoder decoder = new BinaryDecoder(Charset.forName("UTF-8"), new Handler<int[]>() {
      @Override
      public void handle(int[] data) {
        eventMapper.append(data);
/*
        while (eventMapper.hasNext()) {
          Event event = eventMapper.next();
          System.out.println("Read " + event);
        }
*/
      }
    });

    FileChannel channel = new FileInputStream(FileDescriptor.in).getChannel();
    ByteBuffer buff = ByteBuffer.allocate(100);

    while (true) {
      channel.read(buff);
      buff.flip();
      byte[] bytes = new byte[buff.limit()];
      buff.get(bytes);
      decoder.write(bytes);
      buff.compact();
    }
  }
}
