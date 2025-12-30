//> using repository https://central.sonatype.com/repository/maven-snapshots
//> using scala 2.13.17
//> using dep org.chipsalliance::chisel:7.3.0
//> using plugin org.chipsalliance:::chisel-plugin:7.3.0
//> using options -unchecked -deprecation -language:reflectiveCalls -feature -Xcheckinit
//> using options -Xfatal-warnings -Ywarn-dead-code -Ywarn-unused -Ymacro-annotations

package com.rinthyAi.perilus.main

import chisel3._
import chisel3.util._
import _root_.circt.stage.ChiselStage

import com.rinthyAi.perilus.alu._
import com.rinthyAi.perilus.extendUnit._
import com.rinthyAi.perilus.memory._
import com.rinthyAi.perilus.registerFile._
import com.rinthyAi.perilus.controlUnit._

class Perilus extends Module {
  val io = IO(new Bundle {})

  val width = 32.W
  val memorySizeWords = 1024

  val alu = Module(new Alu())
  val controlUnit = Module(new ControlUnit())
  val extendUnit = Module(new ExtendUnit())
  val memory = Module(new Memory(memorySizeWords, width))
  val registerFile = Module(new RegisterFile(width))

  val pc = RegInit(0.U(width))
  val pcNext = WireDefault(UInt(width), 0.U)
  when(controlUnit.io.pcWrite) {
    pc := pcNext
  }

  val oldPc = RegInit(0.U(width))
  when(controlUnit.io.irWrite) {
    oldPc := pc
  }

  val instr = RegInit(0.U(width))
  when(controlUnit.io.irWrite) {
    instr := memory.io.readData
  }

  val readDataBuf = RegInit(0.U(width))
  readDataBuf := memory.io.readData

  val rd1Buf = RegInit(0.U(width))
  rd1Buf := registerFile.io.rd1
  val rd2Buf = RegInit(0.U(width))
  rd2Buf := registerFile.io.rd2

  val aluOutBuf = RegInit(0.U(width))
  aluOutBuf := alu.io.aluResult

  val result = WireDefault(0.U(width))
  switch(controlUnit.io.resultSrc) {
    is(ResultSrc.aluOutBuf) {
      result := aluOutBuf
    }
    is(ResultSrc.readDataBuf) {
      result := readDataBuf
    }
    is(ResultSrc.aluResult) {
      result := alu.io.aluResult
    }
  }

  alu.io.aluControl := controlUnit.io.aluControl
  alu.io.srcA := DontCare
  alu.io.srcB := DontCare
  switch(controlUnit.io.aluSrcA) {
    is(AluSrcA.pc) {
      alu.io.srcA := pc
    }
    is(AluSrcA.oldPc) {
      alu.io.srcA := oldPc
    }
    is(AluSrcA.rd1) {
      alu.io.srcA := rd1Buf
    }
  }
  switch(controlUnit.io.aluSrcB) {
    is(AluSrcB.rd2) {
      alu.io.srcB := rd2Buf
    }
    is(AluSrcB.immExt) {
      alu.io.srcB := extendUnit.io.immExt
    }
    is(AluSrcB.four) {
      alu.io.srcB := 4.U
    }
  }
  controlUnit.io.zero := alu.io.zero

  val (opcode, opcodeValid) = Opcode.safe(instr(6, 0))
  assert(opcodeValid, "Got invalid opcode: 0b%b", instr(6, 0))
  controlUnit.io.op := opcode
  controlUnit.io.funct3 := instr(14, 12)
  controlUnit.io.funct7_5 := instr(30)

  extendUnit.io.input := instr(31, 7)
  extendUnit.io.immSrc := controlUnit.io.immSrc

  when(controlUnit.io.adrSrc) {
    memory.io.address := result
  }.otherwise {
    memory.io.address := pc
  }
  memory.io.writeData := rd2Buf
  memory.io.writeEnable := controlUnit.io.memWrite

  registerFile.io.a1 := instr(19, 15)
  registerFile.io.a2 := instr(24, 20)
  registerFile.io.a3 := instr(11, 7)
  registerFile.io.writeData3 := result
  registerFile.io.writeEnable3 := controlUnit.io.regWrite
}

object Perilus extends App {
  println(
    ChiselStage.emitSystemVerilogFile(
      new Perilus,
      firtoolOpts = Array(
        "-disable-all-randomization",
        "-strip-debug-info",
        "-default-layer-specialization=enable"
      )
    )
  )
}
