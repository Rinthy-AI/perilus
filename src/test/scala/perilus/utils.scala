package com.rinthyAi.perilus.test.utils

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardOpenOption}
import scala.collection.mutable.ArrayBuffer

object TestUtils {
  def initMemFile(data: ArrayBuffer[Int]): String = {
    val tmp: Path = Files.createTempFile("mem_", ".hex")

    val sb = new StringBuilder
    var i = 0
    while (i < data.length) {
      sb.append(String.format("%08x", java.lang.Integer.toUnsignedLong(data(i)))).append('\n')
      i += 1
    }

    Files.write(
      tmp,
      sb.toString.getBytes(StandardCharsets.US_ASCII),
      StandardOpenOption.TRUNCATE_EXISTING
    )

    tmp.toAbsolutePath.toString
  }
}
