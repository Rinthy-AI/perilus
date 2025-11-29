package com.rinthyAi.perilus.extendUnit

import chisel3._

class ExtendUnit extends Module {
  val io = IO(new Bundle {
    val input = Input(UInt(21.W))
    val immSrc = Input(UInt(2.W))

    val immExt = Output(UInt(32.W))
  })

  // TODO implement
  io.immExt := 0.U
}
