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

  private val AddressShift = log2Ceil(width.get / 8)

  val memory = Mem(numWords, UInt(width))
  if (initMem.nonEmpty) {
    loadMemoryFromFile(memory, initMem)
  }

  io.debug.foreach(d => {
    d.memData := memory.read(d.memAddr >> AddressShift)
  })

  when(io.writeEnable) {
    memory.write(io.address >> AddressShift, io.writeData)
  }

  io.readData := memory.read(io.address >> AddressShift)
}
