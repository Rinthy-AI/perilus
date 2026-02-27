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
    val dataMask = Input(DataMask())
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
    memory.write(io.address >> AddressShift, io.writeData & io.dataMask.asUInt)
  }

  io.readData := memory.read(io.address >> AddressShift) & io.dataMask.asUInt
}

object DataMask extends ChiselEnum {
  val byte = Value("h000000ff".U)
  val half = Value("h0000ffff".U)
  val word = Value("hffffffff".U)
}
