package com.rinthyAi.perilus.test.registerFile

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import scala.collection.mutable.ArrayBuffer

import com.rinthyAi.perilus.registerFile._
import com.rinthyAi.perilus.test.utils.TestUtils.initMemFile

class RegisterFileTests extends AnyFunSpec with ChiselSim {
  describe("RegisterFile") {
    it("stores and retrieves data") {
      simulate(
        new RegisterFile(32.W, initMemFile(ArrayBuffer.fill(32)(0x00000000)))
      ) { registerFile =>
        {
          val reg1 = 5.U
          val reg2 = 19.U
          val reg3 = 16.U
          val data1 = "ha342aa0a".U
          val data2 = "h3361a296".U

          registerFile.io.a1.poke(reg1)
          registerFile.io.a2.poke(reg2)
          registerFile.io.a3.poke(reg3)

          registerFile.io.rd1.expect(0.U)
          registerFile.io.rd2.expect(0.U)

          registerFile.io.writeData3.poke(data1)
          registerFile.io.writeEnable3.poke(true.B)

          registerFile.clock.step(1)

          registerFile.io.a1.poke(reg3)
          registerFile.io.rd1.expect(data1)
          registerFile.io.a3.poke(reg2)
          registerFile.io.writeData3.poke(data2)

          registerFile.clock.step(1)

          registerFile.io.a2.poke(reg2)
          registerFile.io.rd2.expect(data2)

          registerFile.io.writeEnable3.poke(false.B)
          registerFile.clock.step(1)
          registerFile.io.rd2.expect(data2)
        }
      }
    }
    it("doesn't write to x0") {
      simulate(
        new RegisterFile(32.W, initMemFile(ArrayBuffer.fill(32)(0x00000000)))
      ) { registerFile =>
        {
          registerFile.io.a3.poke(0.U)
          registerFile.io.writeData3.poke("h500bc902".U)
          registerFile.io.writeEnable3.poke(true.B)
          registerFile.clock.step(1)
          registerFile.io.a1.poke(0.U)
          registerFile.io.rd1.expect(0.U)
        }
      }
    }
  }
}
