package com.rinthyAi.perilus.registerFile

import chisel3._

class RegisterFile(width: Width) extends Module {
  val io = IO(new Bundle {
    val a1, a2, a3 = Input(UInt(5.W))
    val writeData3 = Input(UInt(width))
    val writeEnable3 = Input(Bool())

    val rd1, rd2 = Output(UInt(width))
  })

  val registerFile = RegInit(VecInit(Seq.fill(32)(0.U(width))))

  io.rd1 := registerFile(io.a1)
  io.rd2 := registerFile(io.a2)

  when(io.writeEnable3 && io.a3 =/= 0.U) {
    registerFile(io.a3) := io.writeData3
  }
}
