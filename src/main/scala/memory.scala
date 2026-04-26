package com.rinthyAi.perilus.memory

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class Memory(
    numWords: Int,
    width: Width,
    initMem: String = "",
    withDebug: Boolean = false
) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(width))
    val writeData = Input(UInt(width))
    val writeEnable = Input(Bool())
    val readData = Output(UInt(width))
    val dataMask = Input(DataMask())
    val signExtData = Input(Bool())
    val debug =
      if (withDebug) Some(new Bundle {
        val memAddr = Input(UInt(width))
        val memData = Output(Vec(width.get / 8, UInt(8.W)))
      })
      else None
  })

  private val BytesPerWord = width.get / 8
  private val AddressShift = log2Ceil(BytesPerWord)

  // TODO: change this into a SyncReadMem, which is apparently more easily mapped to hardware
  // but will probably require changes to the control FSM due to synchronous ops
  val memory = Mem(numWords, Vec(BytesPerWord, UInt(8.W)))
  if (initMem.nonEmpty) {
    loadMemoryFromFile(memory, initMem)
  }

  io.debug.foreach(d => {
    d.memData := memory.read(d.memAddr >> AddressShift)
  })

  val byteOffset = Wire(UInt(2.W))
  byteOffset := io.address & 3.U

  // TODO unaligned accesses should raise an exception or something...
  // for now they just round down to the nearest boundary
  val maskValue =
    WireDefault(Vec(BytesPerWord, Bool()), VecInit(false.B, false.B, false.B, false.B))
  val writeData = WireDefault(Vec(BytesPerWord, UInt(8.W)), VecInit(0.U, 0.U, 0.U, 0.U))
  when(io.dataMask === DataMask.byte) {
    when(byteOffset === 0.U) {
      maskValue(0) := true.B
      writeData(0) := io.writeData(7, 0)
    }.elsewhen(byteOffset === 1.U) {
      maskValue(1) := true.B
      writeData(1) := io.writeData(7, 0)
    }.elsewhen(byteOffset === 2.U) {
      maskValue(2) := true.B
      writeData(2) := io.writeData(7, 0)
    }.elsewhen(byteOffset === 3.U) {
      maskValue(3) := true.B
      writeData(3) := io.writeData(7, 0)
    }
  }.elsewhen(io.dataMask === DataMask.half) {
    when(byteOffset === 0.U || byteOffset === 1.U) {
      maskValue(0) := true.B
      maskValue(1) := true.B
      writeData(0) := io.writeData(7, 0)
      writeData(1) := io.writeData(15, 8)
    }.elsewhen(byteOffset === 2.U || byteOffset === 3.U) {
      maskValue(2) := true.B
      maskValue(3) := true.B
      writeData(2) := io.writeData(7, 0)
      writeData(3) := io.writeData(15, 8)
    }
  }.elsewhen(io.dataMask === DataMask.word) {
    maskValue(0) := true.B
    maskValue(1) := true.B
    maskValue(2) := true.B
    maskValue(3) := true.B
    writeData(0) := io.writeData(7, 0)
    writeData(1) := io.writeData(15, 8)
    writeData(2) := io.writeData(23, 16)
    writeData(3) := io.writeData(31, 24)
  }

  when(io.writeEnable && !reset.asBool) {
    memory.write(io.address >> AddressShift, writeData, maskValue)
  }

  val readBytes = Wire(Vec(BytesPerWord, UInt(8.W)))
  readBytes := memory.read(io.address >> AddressShift)
  val returnByte = WireDefault(UInt(8.W), 0.U)
  val halfOffset = Wire(UInt(1.W))
  halfOffset := io.address(1)
  val returnHalf = WireDefault(UInt(16.W), 0.U)
  when(io.dataMask === DataMask.byte) {
    when(byteOffset === 3.U) {
      returnByte := readBytes(3)
    }.elsewhen(byteOffset === 2.U) {
      returnByte := readBytes(2)
    }.elsewhen(byteOffset === 1.U) {
      returnByte := readBytes(1)
    }.otherwise {
      returnByte := readBytes(0)
    }
    when(io.signExtData) {
      io.readData := Cat(Fill(24, returnByte(7)), returnByte)
    }.otherwise {
      io.readData := returnByte
    }
  }.elsewhen(io.dataMask === DataMask.half) {
    when(halfOffset === 1.U) {
      returnHalf := Cat(readBytes(3), readBytes(2))
    }.otherwise {
      returnHalf := Cat(readBytes(1), readBytes(0))
    }
    when(io.signExtData) {
      io.readData := Cat(Fill(16, returnHalf(15)), returnHalf(15, 8), returnHalf(7, 0))
    }.otherwise {
      io.readData := Cat(returnHalf(15, 8), returnHalf(7, 0))
    }
  }.otherwise {
    io.readData := Cat(readBytes(3), readBytes(2), readBytes(1), readBytes(0))
  }
}

object DataMask extends ChiselEnum {
  val byte, half, word = Value
}
