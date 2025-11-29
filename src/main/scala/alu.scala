package com.rinthyAi.perilus.alu

import chisel3._

class Alu extends Module {
  val io = IO(new Bundle {
    val aluControl = Input(UInt(3.W))
    val srcA, srcB = Input(UInt(32.W))

    val aluResult = Output(UInt(32.W))
  })

  when(io.aluControl === "b000".U) {
    io.aluResult := io.srcA + io.srcB
  }.elsewhen(io.aluControl === "b001".U) {
    io.aluResult := io.srcA - io.srcB
  }.elsewhen(io.aluControl === "b010".U) {
    io.aluResult := io.srcA & io.srcB
  }.elsewhen(io.aluControl === "b011".U) {
    io.aluResult := io.srcA | io.srcB
  }.elsewhen(io.aluControl === "b101".U) {
    io.aluResult := io.srcA < io.srcB
  }.otherwise {
    io.aluResult := 0.U
  }
}
