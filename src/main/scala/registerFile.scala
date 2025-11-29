package com.rinthyAi.perilus.registerFile

import chisel3._

class RegisterFile extends Module {
  val io = IO(new Bundle {
    val a1, a2, a3 = Input(UInt(5.W))
    val writeData3 = Input(UInt(32.W))
    val writeEnable3 = Input(Bool())

    val rd1, rd2 = Output(UInt(32.W))
  })

  // TODO implement
  io.rd1 := 0.U
  io.rd2 := 0.U
}
