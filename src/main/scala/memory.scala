package com.rinthyAi.perilus.memory

import chisel3._

class Memory extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val writeEnable = Input(Bool())

    val readData = Output(UInt(32.W))
  })

  // TODO implement
  io.readData := 0.U
}
