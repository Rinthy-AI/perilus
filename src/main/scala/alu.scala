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

  val srcBShift = WireInit(0.U)
  when(io.srcB(5)) {
    srcBShift := 32.U
  }.otherwise {
    srcBShift := io.srcB(4, 0)
  }

  switch(io.aluControl) {
    is(AluControl.add) {
      io.aluResult := io.srcA + io.srcB
    }
    is(AluControl.sub) {
      io.aluResult := io.srcA - io.srcB
    }
    is(AluControl.sll) {
      io.aluResult := io.srcA << srcBShift
    }
    is(AluControl.slt) {
      io.aluResult := (io.srcA.asSInt < io.srcB.asSInt).asUInt
    }
    is(AluControl.sltu) {
      io.aluResult := io.srcA < io.srcB
    }
    is(AluControl.xor) {
      io.aluResult := io.srcA ^ io.srcB
    }
    is(AluControl.srl) {
      io.aluResult := io.srcA >> srcBShift
    }
    is(AluControl.sra) {
      io.aluResult := (io.srcA.asSInt >> srcBShift).asUInt
    }
    is(AluControl.or) {
      io.aluResult := io.srcA | io.srcB
    }
    is(AluControl.and) {
      io.aluResult := io.srcA & io.srcB
    }
  }

  io.zero := io.aluResult === 0.U
}

object AluControl extends ChiselEnum {
  val add, sub, sll, slt, sltu, xor, srl, sra, or, and = Value
}

object AluSrcA extends ChiselEnum {
  val pc, oldPc, rd1 = Value
}

object AluSrcB extends ChiselEnum {
  val rd2, immExt, four = Value
}

object AluOp extends ChiselEnum {
  val memory, branch, arithmetic = Value
}

object ResultSrc extends ChiselEnum {
  val aluOutBuf, readDataBuf, aluResult = Value
}
