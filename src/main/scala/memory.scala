package com.rinthyAi.perilus.memory

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.experimental.loadMemoryFromFile

class Memory(
    numWords: Int,
    width: Width,
    initMem: String = "",
    withDebug: Boolean = false
) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(width))
    val writeData = Input(UInt(width))
    val writeEnable = Input(Bool())
    val readData = Output(UInt(width))
    val debug =
      if (withDebug) Some(new Bundle {
        val memAddr = Input(UInt(width))
        val memData = Output(UInt(width))
      })
      else None
  })

  val memory = Mem(numWords, UInt(width))
  if (initMem.nonEmpty) {
    loadMemoryFromFile(memory, initMem)
  }

  // this fixes warnings related to width being too high or low to index memory
  // see https://www.chisel-lang.org/docs/cookbooks/cookbook#how-do-i-resolve-dynamic-index--is-too-widenarrow-for-extractee-
  val correctWidth = log2Ceil(numWords)
  val correctWidthAddress = io.address.pad(correctWidth)(correctWidth - 1, 0)

  io.debug.foreach(d => {
    val correctWidthDebugAddress = d.memAddr.pad(correctWidth)(correctWidth - 1, 0)
    d.memData := memory.read(correctWidthDebugAddress)
  })

  when(io.writeEnable) {
    memory.write(correctWidthAddress, io.writeData)
  }

  io.readData := memory.read(correctWidthAddress)
}
