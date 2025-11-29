//> using repository https://central.sonatype.com/repository/maven-snapshots
//> using scala 2.13.17
//> using dep org.chipsalliance::chisel:7.3.0
//> using plugin org.chipsalliance:::chisel-plugin:7.3.0
//> using options -unchecked -deprecation -language:reflectiveCalls -feature -Xcheckinit
//> using options -Xfatal-warnings -Ywarn-dead-code -Ywarn-unused -Ymacro-annotations

package com.rinthyAi.perilus.main

import chisel3._
import _root_.circt.stage.ChiselStage

import com.rinthyAi.perilus.alu._
import com.rinthyAi.perilus.extendUnit._
import com.rinthyAi.perilus.memory._
import com.rinthyAi.perilus.registerFile._
import com.rinthyAi.perilus.controlUnit._

class Perilus extends Module {
  val io = IO(new Bundle {})

  val alu = Module(new Alu())
  val controlUnit = Module(new ControlUnit())
  val extendUnit = Module(new ExtendUnit())
  val memory = Module(new Memory())
  val registerFile = Module(new RegisterFile())

  // TODO connect inputs and outputs

  alu.io.aluControl := 0.U
  alu.io.srcA := 0.U
  alu.io.srcB := 0.U

  controlUnit.io.op := 0.U
  controlUnit.io.funct3 := 0.U
  controlUnit.io.funct7 := 0.U

  extendUnit.io.input := 0.U
  extendUnit.io.immSrc := 0.U

  memory.io.address := 0.U
  memory.io.writeData := 0.U
  memory.io.writeEnable := 0.U

  registerFile.io.a1 := 0.U
  registerFile.io.a2 := 0.U
  registerFile.io.a3 := 0.U
  registerFile.io.writeData3 := 0.U
  registerFile.io.writeEnable3 := false.B
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
