package com.rinthyAi.perilus.alu

import chisel3._
import chisel3.util._

class Alu extends Module {
  val io = IO(new Bundle {
    val aluControl = Input(AluControl())
    val srcA, srcB = Input(UInt(32.W))
    val aluResult = Output(UInt(32.W))
    val zero = Output(Bool())
  })

  io.aluResult := 0.U

  switch(io.aluControl) {
    is(AluControl.add) {
      io.aluResult := io.srcA + io.srcB
    }
    is(AluControl.sub) {
      io.aluResult := io.srcA - io.srcB
    }
    is(AluControl.and) {
      io.aluResult := io.srcA & io.srcB
    }
    is(AluControl.or) {
      io.aluResult := io.srcA | io.srcB
    }
    is(AluControl.slt) {
      io.aluResult := io.srcA < io.srcB
    }
  }

  io.zero := io.aluResult === 0.U
}

object AluControl extends ChiselEnum {
  val add = Value("b000".U)
  val sub = Value("b001".U)
  val and = Value("b010".U)
  val or = Value("b011".U)
  val slt = Value("b101".U)
}

object AluSrcA extends ChiselEnum {
  val pc = Value("b00".U)
  val oldPc = Value("b01".U)
  val rd1 = Value("b10".U)
}

object AluSrcB extends ChiselEnum {
  val rd2 = Value("b00".U)
  val immExt = Value("b01".U)
  val four = Value("b10".U)
}

object AluOp extends ChiselEnum {
  val memory = Value("b00".U)
  val branch = Value("b01".U)
  val arithmetic = Value("b10".U)
}

object ResultSrc extends ChiselEnum {
  val aluOutBuf = Value("b00".U)
  val readDataBuf = Value("b01".U)
  val aluResult = Value("b10".U)
}
