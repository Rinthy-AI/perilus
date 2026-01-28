package com.rinthyAi.perilus.test.memory

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import scala.collection.mutable.ArrayBuffer

import com.rinthyAi.perilus.memory._
import com.rinthyAi.perilus.test.utils.TestUtils.initMemFile

class MemoryTests extends AnyFunSpec with ChiselSim {
  describe("Memory") {
    it("stores and retrieves data") {
      val numWords = 8
      simulate(new Memory(numWords, 32.W, initMemFile(ArrayBuffer.fill(numWords)(0x00000000)))) {
        memory =>
          {
            val addr1 = "h4".U
            val addr2 = "h18".U
            val data1 = "he7d857de".U
            val data2 = "h38cc122f".U

            memory.io.address.poke(addr1)
            memory.io.writeData.poke(data1)
            memory.io.writeEnable.poke(true.B)
            memory.io.readData.expect(0.U)
            memory.clock.step(1)
            memory.io.readData.expect(data1)

            memory.io.address.poke(addr2)
            memory.io.writeData.poke(data2)
            memory.io.writeEnable.poke(true.B)
            memory.io.readData.expect(0.U)
            memory.clock.step(1)
            memory.io.readData.expect(data2)

            memory.io.writeData.poke(0.U)
            memory.io.writeEnable.poke(false.B)
            memory.clock.step(1)
            memory.io.address.poke(addr1)
            memory.io.readData.expect(data1)
            memory.io.address.poke(addr2)
            memory.io.readData.expect(data2)
            memory.io.address.poke(0.U)
            memory.io.readData.expect(0.U)
          }
      }
    }
  }
}
