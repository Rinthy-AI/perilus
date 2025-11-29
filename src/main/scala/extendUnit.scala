package com.rinthyAi.perilus.extendUnit

import chisel3._
import chisel3.util._

class ExtendUnit extends Module {
  val io = IO(new Bundle {
    val input = Input(UInt(25.W))
    val immSrc = Input(UInt(2.W))

    val immExt = Output(UInt(32.W))
  })

  // Table 7.1 (page 406)
  when(io.immSrc === "b00".U) {
    // I-type instruction
    io.immExt := Cat(Fill(20, io.input(24)), io.input(24, 13))
  }.elsewhen(io.immSrc === "b01".U) {
    // S-type instruction
    io.immExt := Cat(Fill(20, io.input(24)), io.input(24, 18), io.input(4, 0))
  }.elsewhen(io.immSrc === "b10".U) {
    // B-type instruction
    io.immExt := Cat(
      Fill(20, io.input(24)),
      io.input(0),
      io.input(23, 18),
      io.input(4, 1),
      0.U(1.W)
    )
  }.otherwise {
    io.immExt := 0.U
  }
}
