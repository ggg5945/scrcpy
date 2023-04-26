package com.genymobile.scrcpy;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class DesktopConnection implements Closeable {

  private static final int DEVICE_NAME_FIELD_LENGTH = 64;

  private static final String SOCKET_NAME_PREFIX = "scrcpy";

  private final DatagramSocket videoSocket;
  private final DatagramSocket audioSocket;
  private final Socket controlSocket;
  private final InputStream controlInputStream;
  private final OutputStream controlOutputStream;

  private final InetAddress cilentIp;

  private final ControlMessageReader reader = new ControlMessageReader();
  private final DeviceMessageWriter writer = new DeviceMessageWriter();

  private DesktopConnection(DatagramSocket videoSocket, DatagramSocket audioSocket, Socket controlSocket) throws IOException {
    this.videoSocket = videoSocket;
    this.controlSocket = controlSocket;
    this.audioSocket = audioSocket;
    if (controlSocket != null) {
      controlInputStream = controlSocket.getInputStream();
      controlOutputStream = controlSocket.getOutputStream();
    } else {
      controlInputStream = null;
      controlOutputStream = null;
    }
    cilentIp = controlSocket.getInetAddress();
  }

  private static LocalSocket connect(String abstractName) throws IOException {
    LocalSocket localSocket = new LocalSocket();
    localSocket.connect(new LocalSocketAddress(abstractName));
    return localSocket;
  }

  private static String getSocketName(int scid) {
    if (scid == -1) {
      // If no SCID is set, use "scrcpy" to simplify using scrcpy-server alone
      return SOCKET_NAME_PREFIX;
    }

    return SOCKET_NAME_PREFIX + String.format("_%08x", scid);
  }

  public static DesktopConnection open(int scid, boolean tunnelForward, boolean audio, boolean control, boolean sendDummyByte) throws IOException {
    DatagramSocket videoSocket = null;
    DatagramSocket audioSocket = null;
    Socket controlSocket = null;
    try {
      if (tunnelForward) {
        videoSocket = new DatagramSocket();
        audioSocket = new DatagramSocket();
        try (ServerSocket localServerSocket = new ServerSocket(6006)) {
          if (control) {
            controlSocket = localServerSocket.accept();
          }
        }
      }
    } catch (IOException | RuntimeException e) {
      if (videoSocket != null) {
        videoSocket.close();
      }
      if (audioSocket != null) {
        audioSocket.close();
      }
      if (controlSocket != null) {
        controlSocket.close();
      }
      throw e;
    }

    return new DesktopConnection(videoSocket, audioSocket, controlSocket);
  }

  public void close() throws IOException {
    if (controlSocket != null) {
      controlSocket.shutdownInput();
      controlSocket.shutdownOutput();
      controlSocket.close();
    }
  }

  public DatagramSocket getVideoFd() {
    return videoSocket;
  }

  public DatagramSocket getAudioFd() {
    return audioSocket;
  }

  public InetAddress getCilentIp() {
    return cilentIp;
  }

  public ControlMessage receiveControlMessage() throws IOException {
    ControlMessage msg = reader.next();
    while (msg == null) {
      reader.readFrom(controlInputStream);
      msg = reader.next();
    }
    return msg;
  }

  public void sendDeviceMessage(DeviceMessage msg) throws IOException {
    writer.writeTo(msg, controlOutputStream);
  }
}
