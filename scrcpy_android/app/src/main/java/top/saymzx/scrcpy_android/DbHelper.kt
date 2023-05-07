package top.saymzx.scrcpy_android

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(
  context: Context?,
  name: String?,
  version: Int
) : SQLiteOpenHelper(context, name, null, version) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE DevicesDb (\n" +
          "\t name text PRIMARY KEY,\n" +
          "\t address text,\n" +
          "\t port integer,\n" +
          "\t remoteSocketPort integer,\n" +
          "\t videoCodec text,\n" +
          "\t resolution integer,\n" +
          "\t fps integer,\n" +
          "\t videoBit integer)"
    )
  }

  @SuppressLint("Range")
  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) {
      // 新建列
      db!!.execSQL("alter table DevicesDb add column fps integer")
      db.execSQL("alter table DevicesDb add column videoBit integer")
      // 更新旧数据的默认值
      val cursor =
        db.query("DevicesDb", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val name = cursor.getString(cursor.getColumnIndex("name"))
          db.execSQL("update DevicesDb set fps='60',videoBit='16000000' where name=='$name'")
        } while (cursor.moveToNext())
      }
      cursor.close()
    }
    // 修改列名，增加端口列
    if (oldVersion < 3) {
      // 修改表名
      db!!.execSQL("alter table DevicesDb rename to DevicesDbOld")
      // 新建新表
      db.execSQL(
        "CREATE TABLE DevicesDb (\n" +
            "\t name text PRIMARY KEY,\n" +
            "\t address text,\n" +
            "\t port integer,\n" +
            "\t remoteSocketPort integer,\n" +
            "\t videoCodec text,\n" +
            "\t resolution integer,\n" +
            "\t fps integer,\n" +
            "\t videoBit integer)"
      )
      // 将数据搬移至新表
      val cursor =
        db.query("DevicesDbOld", null, null, null, null, null, null)
      if (cursor.moveToFirst()) {
        do {
          val values = ContentValues().apply {
            put("name", cursor.getString(cursor.getColumnIndex("name")))
            put("address", cursor.getString(cursor.getColumnIndex("ip")))
            put("port", 5555)
            put("videoCodec", cursor.getString(cursor.getColumnIndex("videoCodec")))
            put("resolution", cursor.getString(cursor.getColumnIndex("resolution")))
            put("fps", cursor.getString(cursor.getColumnIndex("fps")))
            put("videoBit", cursor.getString(cursor.getColumnIndex("videoBit")))
          }
          db.insert("DevicesDb", null, values)
        } while (cursor.moveToNext())
      }
      cursor.close()
      // 删除旧表
      db.execSQL("drop table DevicesDbOld")
    }
  }
}