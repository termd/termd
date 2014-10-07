package io.modsh.core.system;

import io.modsh.core.io.BinaryDecoder;
import io.modsh.core.readline.Action;
import io.modsh.core.readline.Reader;
import io.modsh.core.readline.ReadlineBootstrap;

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
    Reader reader = new Reader(inputrc);
    BinaryDecoder decoder = new BinaryDecoder(Charset.forName("UTF-8"), (int c) -> {
      reader.append(c);
      while (true) {
        Action action = reader.reduceOnce().popKey();
        if (action != null) {
          System.out.println("Read " + action);
        } else {
          break;
        }
      }
    });

    FileChannel channel = new FileInputStream(FileDescriptor.in).getChannel();
    ByteBuffer buff = ByteBuffer.allocate(100);

    while (true) {
      channel.read(buff);
      buff.flip();

      while (buff.hasRemaining()) {
        byte b = buff.get();
        decoder.onByte(b);
      }

      buff.compact();
    }



  }


}
