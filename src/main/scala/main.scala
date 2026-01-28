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
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import com.rinthyAi.perilus.alu._
import com.rinthyAi.perilus.extendUnit._
import com.rinthyAi.perilus.memory._
import com.rinthyAi.perilus.registerFile._
import com.rinthyAi.perilus.controlUnit._

class Perilus(
    initRegs: String = "",
    initMem: String = "",
    withDebug: Boolean = false
) extends Module {
  val width = 32.W
  val memorySizeWords = if (initMem.nonEmpty) {
    Source.fromFile(initMem).getLines().size
  } else { 1024 }

  val io = IO(new Bundle {
    val memory = Module(new Memory(memorySizeWords, width, initMem, withDebug))
    val registerFile = Module(new RegisterFile(width, initRegs, withDebug))
    val pc = Output(UInt(width))
    val debug =
      if (withDebug) Some(new Bundle {
        val reg = Input(UInt(5.W))
        val regData = Output(UInt(width))
        val memAddr = Input(UInt(width))
        val memData = Output(UInt(width))
      })
      else None
  })

  io.registerFile.io.debug.foreach(r => {
    r.reg := 0.U
  })
  io.memory.io.debug.foreach(m => {
    m.memAddr := 0.U
  })
  io.debug.foreach(d => {
    io.registerFile.io.debug.foreach(r => {
      r.reg := d.reg
      d.regData := r.regData
    })
    io.memory.io.debug.foreach(m => {
      m.memAddr := d.memAddr
      d.memData := m.memData
    })
  })

  val alu = Module(new Alu())
  val controlUnit = Module(new ControlUnit())
  val extendUnit = Module(new ExtendUnit())

  val pc = RegInit(0.U(width))
  val pcNext = WireDefault(UInt(width), 0.U)
  when(controlUnit.io.pcWrite) {
    pc := pcNext
  }

  io.pc := pc

  val oldPc = RegInit(0.U(width))
  when(controlUnit.io.irWrite) {
    oldPc := pc
  }

  val instr = RegInit(0.U(width))
  when(controlUnit.io.irWrite) {
    instr := io.memory.io.readData
  }

  val readDataBuf = RegInit(0.U(width))
  readDataBuf := io.memory.io.readData

  val rd1Buf = RegInit(0.U(width))
  rd1Buf := io.registerFile.io.rd1
  val rd2Buf = RegInit(0.U(width))
  rd2Buf := io.registerFile.io.rd2

  val aluOutBuf = RegInit(0.U(width))
  aluOutBuf := alu.io.aluResult

  val result = WireDefault(0.U(width))
  pcNext := result
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
  alu.io.srcA := 0.U
  alu.io.srcB := 0.U
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
  controlUnit.io.op := opcode
  controlUnit.io.funct3 := instr(14, 12)
  controlUnit.io.funct7_5 := instr(30)

  extendUnit.io.input := instr(31, 7)
  extendUnit.io.immSrc := controlUnit.io.immSrc

  when(controlUnit.io.adrSrc) {
    io.memory.io.address := result
  }.otherwise {
    io.memory.io.address := pc
  }
  io.memory.io.writeData := rd2Buf
  io.memory.io.writeEnable := controlUnit.io.memWrite

  io.registerFile.io.a1 := instr(19, 15)
  io.registerFile.io.a2 := instr(24, 20)
  io.registerFile.io.a3 := instr(11, 7)
  io.registerFile.io.writeData3 := result
  io.registerFile.io.writeEnable3 := controlUnit.io.regWrite
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
