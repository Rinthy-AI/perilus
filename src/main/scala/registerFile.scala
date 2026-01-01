package com.rinthyAi.perilus.registerFile

import chisel3._
import chisel3.util.experimental.loadMemoryFromFile

class RegisterFile(width: Width, initRegs: String = "", withDebug: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val a1, a2, a3 = Input(UInt(5.W))
    val writeData3 = Input(UInt(width))
    val writeEnable3 = Input(Bool())
    val rd1, rd2 = Output(UInt(width))
    val debug =
      if (withDebug) Some(new Bundle {
        val reg = Input(UInt(5.W))
        val regData = Output(UInt(width))
      })
      else None
  })

  val registerFile = Mem(32, UInt(width))
  if (initRegs.nonEmpty) {
    loadMemoryFromFile(registerFile, initRegs)
  }

  io.debug.foreach(d => {
    d.regData := registerFile.read(d.reg)
  })

  io.rd1 := registerFile.read(io.a1)
  io.rd2 := registerFile.read(io.a2)

  when(io.writeEnable3 && io.a3 =/= 0.U) {
    registerFile.write(io.a3, io.writeData3)
  }
}
