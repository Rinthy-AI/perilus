package com.rinthyAi.perilus.memory

import chisel3._
import chisel3.util.log2Ceil

class Memory(numWords: Int, width: Width) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(width))
    val writeData = Input(UInt(width))
    val writeEnable = Input(Bool())

    val readData = Output(UInt(width))
  })

  val memory = RegInit(VecInit(Seq.fill(numWords)(0.U(width))))

  // this fixes warnings related to width being too high or low to index memory
  // see https://www.chisel-lang.org/docs/cookbooks/cookbook#how-do-i-resolve-dynamic-index--is-too-widenarrow-for-extractee-
  val correctWidth = log2Ceil(numWords)
  val correctWidthAddress = io.address.pad(correctWidth)(correctWidth - 1, 0)

  when(io.writeEnable) {
    memory(correctWidthAddress) := io.writeData
  }

  io.readData := memory(correctWidthAddress)
}
