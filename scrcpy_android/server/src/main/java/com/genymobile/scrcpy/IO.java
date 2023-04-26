package com.genymobile.scrcpy;

import android.system.ErrnoException;
import android.system.OsConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Scanner;

public final class IO {
  private IO() {
    // not instantiable
  }

  public static void writeFully(DatagramSocket fd, InetAddress cilentIP, int port, ByteBuffer from) throws IOException {
    // ByteBuffer position is not updated as expected by Os.write() on old Android versions, so
    // count the remaining bytes manually.
    // See <https://github.com/Genymobile/scrcpy/issues/291>.
    int length = from.remaining();
    int po = 0;
    byte[] array = new byte[length];
    from.get(array, 0, length);
    while (length > 1400) {
      length -= 1400;
      fd.send(new DatagramPacket(array, po, 1400, cilentIP, port));
      po += 1400;
    }
    if (length>0)fd.send(new DatagramPacket(array, po, length, cilentIP, port));
  }

  public static void writeFully(DatagramSocket fd, InetAddress cilentIP, int port, byte[] buffer, int offset, int len) throws IOException {
    writeFully(fd, cilentIP, port, ByteBuffer.wrap(buffer, offset, len));
  }

  public static String toString(InputStream inputStream) {
    StringBuilder builder = new StringBuilder();
    Scanner scanner = new Scanner(inputStream);
    while (scanner.hasNextLine()) {
      builder.append(scanner.nextLine()).append('\n');
    }
    return builder.toString();
  }

  public static boolean isBrokenPipe(IOException e) {
    Throwable cause = e.getCause();
    return cause instanceof ErrnoException && ((ErrnoException) cause).errno == OsConstants.EPIPE;
  }
}
