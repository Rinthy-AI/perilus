package com.rinthyAi.perilus.alu

import chisel3._

class Alu extends Module {
  val io = IO(new Bundle {
    val aluControl = Input(UInt(3.W))
    val srcA, srcB = Input(UInt(32.W))

    val aluResult = Output(UInt(32.W))
  })

  // TODO implement
  io.aluResult := 0.U
}
