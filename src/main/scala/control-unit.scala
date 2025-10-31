//> using repository https://central.sonatype.com/repository/maven-snapshots
//> using scala 2.13.17
//> using dep org.chipsalliance::chisel:7.3.0
//> using plugin org.chipsalliance:::chisel-plugin:7.3.0
//> using options -unchecked -deprecation -language:reflectiveCalls -feature -Xcheckinit
//> using options -Xfatal-warnings -Ywarn-dead-code -Ywarn-unused -Ymacro-annotations

import chisel3._
import _root_.circt.stage.ChiselStage

// Figure 7.28 (page 423)
class ControlUnit extends Module {
  val io = IO(new Bundle {
    val branch, pcUpdate, pcWrite, regWrite, memWrite, irWrite, adrSrc = Output(Bool())
    val resultSrc, aluSrcA, aluSrcB, aluOp, immSrc = Output(UInt(2.W))
    val aluControl = Output(UInt(3.W))

    // from datapath
    val op = Input(UInt(7.W))
    val funct3 = Input(UInt(3.W))
    val funct7 = Input(Bool())
  })

  io.branch := 0.U
  io.pcUpdate := 0.U
  io.pcWrite := 0.U
  io.regWrite := 0.U
  io.memWrite := 0.U
  io.irWrite := 0.U
  io.adrSrc := 0.U
  io.resultSrc := 0.U
  io.aluSrcA := 0.U
  io.aluSrcB := 0.U
  io.aluOp := 0.U
  io.immSrc := 0.U
  io.aluControl := 0.U
}

object ControlUnit extends App {
  println(
    ChiselStage.emitSystemVerilogFile(
      new ControlUnit,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
    )
  )
}
