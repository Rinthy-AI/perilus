package com.rinthyAi.perilus.controlUnit

import chisel3._
import chisel3.util._

import com.rinthyAi.perilus.alu._
import com.rinthyAi.perilus.extendUnit._

class ControlUnit(withDebug: Boolean = false) extends Module {
  // Figure 7.28 (page 423)
  val io = IO(new Bundle {
    val op = Input(Opcode())
    val funct3 = Input(UInt(3.W))
    val funct7_5 = Input(Bool())
    val zero = Input(Bool())
    val adrSrc, irWrite, memWrite, pcWrite, regWrite = Output(Bool())
    val aluSrcA = Output(AluSrcA())
    val aluSrcB = Output(AluSrcB())
    val resultSrc = Output(ResultSrc())
    val immSrc = Output(ImmSrc())
    val aluControl = Output(AluControl())
    val debug = if (withDebug) Some(Output(new Bundle {
      val state = ControlUnitState()
    }))
    else None
  })

  val aluDecoder = Module(new AluDecoder())
  val instrDecoder = Module(new InstructionDecoder())

  val state = RegInit(ControlUnitState.fetch)

  io.debug.foreach(d => {
    d.state := state
  })

  val aluOp = WireDefault(AluOp.memory)
  val branch, pcUpdate = WireDefault(false.B)

  io.adrSrc := false.B
  io.aluSrcA := AluSrcA.pc
  io.aluSrcB := AluSrcB.rd2
  io.irWrite := false.B
  io.memWrite := false.B
  io.regWrite := false.B
  io.resultSrc := ResultSrc.aluOutBuf

  // Main FSM (Figure 7.45, page 436)
  printf(cf"state = $state\n")
  switch(state) {
    is(ControlUnitState.fetch) {
      io.adrSrc := false.B
      io.irWrite := true.B
      io.aluSrcA := AluSrcA.pc
      io.aluSrcB := AluSrcB.four
      aluOp := AluOp.memory
      io.resultSrc := ResultSrc.aluResult
      pcUpdate := true.B
      state := ControlUnitState.decode
    }
    is(ControlUnitState.decode) {
      io.aluSrcA := AluSrcA.oldPc
      io.aluSrcB := AluSrcB.immExt
      aluOp := AluOp.memory
      when(io.op === Opcode.load || io.op === Opcode.store) {
        state := ControlUnitState.memAdr
      }.elsewhen(io.op === Opcode.rType) {
        state := ControlUnitState.executeR
      }.elsewhen(io.op === Opcode.immediate) {
        state := ControlUnitState.executeI
      }.elsewhen(io.op === Opcode.jal) {
        state := ControlUnitState.jal
      }.elsewhen(io.op === Opcode.branch) {
        state := ControlUnitState.beq
      }
    }
    is(ControlUnitState.memAdr) {
      io.aluSrcA := AluSrcA.rd1
      io.aluSrcB := AluSrcB.immExt
      aluOp := AluOp.memory
      switch(io.op) {
        is(Opcode.load) {
          state := ControlUnitState.memRead
        }
        is(Opcode.store) {
          state := ControlUnitState.memWrite
        }
      }
    }
    is(ControlUnitState.memRead) {
      io.resultSrc := ResultSrc.aluOutBuf
      io.adrSrc := true.B
      state := ControlUnitState.memWb
    }
    is(ControlUnitState.memWb) {
      io.resultSrc := ResultSrc.readDataBuf
      io.regWrite := true.B
      state := ControlUnitState.fetch
    }
    is(ControlUnitState.memWrite) {
      io.resultSrc := ResultSrc.aluOutBuf
      io.adrSrc := true.B
      io.memWrite := true.B
      state := ControlUnitState.fetch
    }
    is(ControlUnitState.executeR) {
      io.aluSrcA := AluSrcA.rd1
      io.aluSrcB := AluSrcB.rd2
      aluOp := AluOp.arithmetic
      state := ControlUnitState.aluWb
    }
    is(ControlUnitState.aluWb) {
      io.resultSrc := ResultSrc.aluOutBuf
      io.regWrite := true.B
      state := ControlUnitState.fetch
    }
    is(ControlUnitState.executeI) {
      io.aluSrcA := AluSrcA.rd1
      io.aluSrcB := AluSrcB.immExt
      aluOp := AluOp.arithmetic
      state := ControlUnitState.aluWb
    }
    is(ControlUnitState.jal) {
      io.aluSrcA := AluSrcA.oldPc
      io.aluSrcB := AluSrcB.four
      io.resultSrc := ResultSrc.aluOutBuf
      aluOp := AluOp.memory
      pcUpdate := true.B
      state := ControlUnitState.aluWb
    }
    is(ControlUnitState.beq) {
      io.aluSrcA := AluSrcA.rd1
      io.aluSrcB := AluSrcB.rd2
      io.resultSrc := ResultSrc.aluOutBuf
      aluOp := AluOp.branch
      branch := true.B
      state := ControlUnitState.fetch
    }
  }

  io.pcWrite := (io.zero && branch) || pcUpdate

  aluDecoder.io.aluOp := aluOp
  aluDecoder.io.funct3 := io.funct3
  aluDecoder.io.funct7_5 := io.funct7_5
  aluDecoder.io.op := io.op
  io.aluControl := aluDecoder.io.aluControl

  instrDecoder.io.op := io.op
  io.immSrc := instrDecoder.io.immSrc
}

// ALU Decoder (Table 7.3, page 409)
class AluDecoder extends Module {
  val io = IO(new Bundle {
    val aluOp = Input(AluOp())
    val funct3 = Input(UInt(3.W))
    val funct7_5 = Input(Bool())
    val op = Input(Opcode())
    val aluControl = Output(AluControl())
  })

  io.aluControl := AluControl.add

  switch(io.aluOp) {
    is(AluOp.memory) {
      io.aluControl := AluControl.add
    }
    is(AluOp.branch) {
      io.aluControl := AluControl.sub
    }
    is(AluOp.arithmetic) {
      when(
        io.funct3 === "b000".U && ((!io.op.asUInt(5) && !io.funct7_5) || (!io.op.asUInt(
          5
        ) && io.funct7_5) || (io.op.asUInt(5) && !io.funct7_5))
      ) {
        io.aluControl := AluControl.add
      }.elsewhen(io.funct3 === "b000".U && io.op.asUInt(5) && io.funct7_5) {
        io.aluControl := AluControl.sub
      }.elsewhen(io.funct3 === "b010".U) {
        io.aluControl := AluControl.slt
      }.elsewhen(io.funct3 === "b110".U) {
        io.aluControl := AluControl.or
      }.elsewhen(io.funct3 === "b111".U) {
        io.aluControl := AluControl.and
      }
    }
  }
}

// Instruction Decoder (Table 7.6, page 413)
class InstructionDecoder extends Module {
  val io = IO(new Bundle {
    val op = Input(Opcode())
    val immSrc = Output(ImmSrc())
  })

  io.immSrc := ImmSrc.iType

  switch(io.op) {
    is(Opcode.load) {
      io.immSrc := ImmSrc.iType
    }
    is(Opcode.store) {
      io.immSrc := ImmSrc.sType
    }
    is(Opcode.branch) {
      io.immSrc := ImmSrc.bType
    }
    is(Opcode.immediate) {
      io.immSrc := ImmSrc.iType
    }
    is(Opcode.jal) {
      io.immSrc := ImmSrc.jType
    }
  }
}

object ControlUnitState extends ChiselEnum {
  val fetch, decode, memAdr, memRead, memWb, memWrite, executeR, aluWb, executeI, jal, beq = Value
}

object Opcode extends ChiselEnum {
  val load = Value("b0000011".U)
  val immediate = Value("b0010011".U)
  val auipc = Value("b0010111".U)
  val store = Value("b0100011".U)
  val rType = Value("b0110011".U)
  val lui = Value("b0110111".U)
  val branch = Value("b1100011".U)
  val jalr = Value("b1100111".U)
  val jal = Value("b1101111".U)
  val env = Value("b1110011".U)
}
