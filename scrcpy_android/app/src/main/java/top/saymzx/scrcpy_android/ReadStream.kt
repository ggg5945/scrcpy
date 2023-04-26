package top.saymzx.scrcpy_android

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer

// 读取音视频数据
class ReadStream(val socket: DatagramSocket) {
  // 数据存储
  private val byteBuffer = ByteBuffer.allocate(1048576)

  // 单个包
  private val buffer = ByteArray(1500)
  private val packet = DatagramPacket(buffer, buffer.size)

  init {
    byteBuffer.flip()
  }

  // 读取整数
  fun readInt(): Int {
    byteBuffer.compact()
    while (byteBuffer.position() < 4) readData()
    byteBuffer.flip()
    return byteBuffer.int
  }

  // 读取Long
  fun readLong(): Long {
    byteBuffer.compact()
    while (byteBuffer.position() < 8) readData()
    byteBuffer.flip()
    return byteBuffer.long
  }

  // 读取数据帧
  fun readFrame(): ByteArray {
    readLong()
    val size = readInt()
    val buffer = ByteArray(size)
    byteBuffer.compact()
    while (byteBuffer.position() < size) readData()
    byteBuffer.flip()
    byteBuffer.get(buffer, 0, size)
    return buffer
  }

  // 收包
  private fun readData() {
    socket.receive(packet)
    Log.e("sss", packet.length.toString())
    Log.e("read", byteBuffer.remaining().toString())
    byteBuffer.put(buffer, 0, packet.length)
  }

  // 关闭
  fun close() {
    socket.close()
  }
}