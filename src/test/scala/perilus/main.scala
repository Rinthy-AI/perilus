package com.rinthyAi.perilus.test.main

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import scala.collection.mutable.ArrayBuffer

import com.rinthyAi.perilus.main._
import com.rinthyAi.perilus.test.utils.TestUtils.initMemFile

class PerilusTests extends AnyFunSpec with ChiselSim {
  describe("Perilus") {
    it("executes lw") {
      val rd = 21
      val rs1 = 11
      val base = 0x30
      val imm = 0xa8
      val memAddr = base + imm
      val memData = 0x0f3f9400

      var registerFile = ArrayBuffer.fill(32)(0x00000000)
      registerFile(rs1) = 0x00000030
      var memory = ArrayBuffer.fill(64)(0x00000000)
      memory(0) = 0x0a85aa83 // lw x21, 0xa8(x11)
      memory(memAddr / 4) = memData

      simulate(
        new Perilus(
          initRegs = initMemFile(registerFile),
          initMem = initMemFile(memory),
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          // rs1 contains the base pointer
          perilusDebug.reg.poke(rs1)
          perilusDebug.regData.expect(base)

          // rd is empty
          perilusDebug.reg.poke(rd)
          perilusDebug.regData.expect(0.U)

          // memAddr contains memData
          perilusDebug.memAddr.poke(memAddr)
          perilusDebug.memData.expect(memData)

          // execute the instruction
          perilus.clock.step(5)

          // rd contains memData
          perilusDebug.reg.poke(rd)
          perilusDebug.regData.expect(memData)

          // contents of memAddr have not changed
          perilusDebug.memAddr.poke(memAddr)
          perilusDebug.memData.expect(memData)
        }
      }
    }
    it("executes sw") {
      val rs1 = 9
      val rs2 = 6
      val base = 0x38
      val imm = -4
      val memAddr = base + imm
      val memData = 0x01830169

      var registerFile = ArrayBuffer.fill(32)(0x00000000)
      registerFile(rs1) = base
      registerFile(rs2) = memData
      var memory = ArrayBuffer.fill(64)(0x00000000)
      memory(0) = 0xfe64ae23 // sw x6, -4(x9)

      simulate(
        new Perilus(
          initRegs = initMemFile(registerFile),
          initMem = initMemFile(memory),
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          // rs1 contains the base pointer
          perilusDebug.reg.poke(rs1)
          perilusDebug.regData.expect(base)

          // rs2 contains data to be stored
          perilusDebug.reg.poke(rs2)
          perilusDebug.regData.expect(memData)

          // memAddr doesn't have memData yet
          perilusDebug.memAddr.poke(memAddr)
          assert(perilusDebug.memData.peek().litValue != memData)

          // execute the instruction
          perilus.clock.step(4)

          // contents of memAddr have not changed
          perilusDebug.memAddr.poke(memAddr)
          perilusDebug.memData.expect(memData)
        }
      }
    }
    it("executes beq") {
      val rs1 = 3
      val rs2 = 6
      val rs3 = 9
      val offset = 16.U

      var registerFile = ArrayBuffer.fill(32)(0x00000000)
      registerFile(rs1) = 0xabcdef01
      registerFile(rs2) = 0xabcde012
      registerFile(rs3) = 0xabcde012
      var memory = ArrayBuffer.fill(64)(0x00000000)
      memory(0) = 0x02618563 // beq x3, x6, 0x4
      memory(1) = 0x00930863 // beq x6, x9, 0x4

      simulate(
        new Perilus(
          initRegs = initMemFile(registerFile),
          initMem = initMemFile(memory),
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          // Verify that registers are/aren't equal as expected
          perilusDebug.reg.poke(rs1)
          val rs1_t = perilusDebug.regData.peek()
          perilusDebug.reg.poke(rs2)
          val rs2_t = perilusDebug.regData.peek()
          perilusDebug.reg.poke(rs3)
          val rs3_t = perilusDebug.regData.peek()
          assert(rs1_t.litValue != rs2_t.litValue)
          assert(rs2_t.litValue == rs3_t.litValue)

          var pc_t = perilus.io.pc.peek()
          // execute the first beq instruction:fe64ae23
          //   beq x3, x6, 0x4
          perilus.clock.step(3)

          // pc should be at address 1 because x3 != x6
          val expected_1 = pc_t.litValue + 4
          perilus.io.pc.expect(expected_1.U)
          pc_t = perilus.io.pc.peek()

          // execute the second beq instruction:
          //   beq x6, x9, 0x4
          perilus.clock.step(3)

          // pc should be at address 5 because x6 == x9
          val expected_2 = pc_t.litValue + offset.litValue
          perilus.io.pc.expect(expected_2.U)
        }
      }
    }
  }
}
