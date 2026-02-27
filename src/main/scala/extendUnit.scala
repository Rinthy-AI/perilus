package com.rinthyAi.perilus.extendUnit

import chisel3._
import chisel3.util._

class ExtendUnit extends Module {
  val io = IO(new Bundle {
    val input = Input(UInt(25.W))
    val immSrc = Input(ImmSrc())
    val immExt = Output(UInt(32.W))
  })

  io.immExt := 0.U

  // Table 7.5 (page 412)
  switch(io.immSrc) {
    is(ImmSrc.iTypeSigned) {
      io.immExt := Cat(Fill(20, io.input(24)), io.input(24, 13))
    }
    is(ImmSrc.iTypeUnsigned) {
      io.immExt := Cat(Fill(20, 0.U), io.input(24, 13))
    }
    is(ImmSrc.sType) {
      io.immExt := Cat(Fill(20, io.input(24)), io.input(24, 18), io.input(4, 0))
    }
    is(ImmSrc.bType) {
      io.immExt := Cat(
        Fill(20, io.input(24)),
        io.input(0),
        io.input(23, 18),
        io.input(4, 1),
        0.U(1.W)
      )
    }
    is(ImmSrc.jType) {
      io.immExt := Cat(
        Fill(12, io.input(24)),
        io.input(12, 5),
        io.input(13),
        io.input(23, 14),
        0.U(1.W)
      )
    }
  }
}

object ImmSrc extends ChiselEnum {
  val iTypeSigned, iTypeUnsigned, sType, bType, jType = Value
}
