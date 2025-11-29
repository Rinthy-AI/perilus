package com.rinthyAi.perilus.controlUnit

import chisel3._

// Figure 7.28 (page 423)
class ControlUnit extends Module {
  val io = IO(new Bundle {
    val op = Input(UInt(7.W))
    val funct3 = Input(UInt(3.W))
    val funct7 = Input(Bool())

    val adrSrc, branch, irWrite, memWrite, pcUpdate, pcWrite, regWrite = Output(Bool())
    val aluSrcA, aluSrcB, aluOp, immSrc, resultSrc = Output(UInt(2.W))
    val aluControl = Output(UInt(3.W))
  })

  io.adrSrc := 0.U
  io.aluControl := 0.U
  io.aluOp := 0.U
  io.aluSrcA := 0.U
  io.aluSrcB := 0.U
  io.branch := 0.U
  io.immSrc := 0.U
  io.irWrite := 0.U
  io.memWrite := 0.U
  io.pcUpdate := 0.U
  io.pcWrite := 0.U
  io.regWrite := 0.U
  io.resultSrc := 0.U
}
