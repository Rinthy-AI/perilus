package com.rinthyAi.perilus.test.main

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import java.lang.Integer.{toUnsignedLong => toULong}
import org.scalatest.funspec.AnyFunSpec
import scala.collection.mutable.ArrayBuffer

import com.rinthyAi.perilus.main._
import com.rinthyAi.perilus.test.utils.TestUtils.initMemFile

class PerilusTests extends AnyFunSpec with ChiselSim {
  describe("Perilus") {
    describe("executes I-type RV32I instructions") {
      it("lb") {
        cancel("Not yet implemented")
      }
      it("lh") {
        cancel("Not yet implemented")
      }
      it("lw") {
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
      it("lbu") {
        cancel("Not yet implemented")
      }
      it("lhu") {
        cancel("Not yet implemented")
      }
      it("addi") {
        cancel("Not yet implemented")
      }
      it("slli") {
        cancel("Not yet implemented")
      }
      it("slti") {
        cancel("Not yet implemented")
      }
      it("sltiu") {
        cancel("Not yet implemented")
      }
      it("xori") {
        cancel("Not yet implemented")
      }
      it("srli") {
        cancel("Not yet implemented")
      }
      it("srai") {
        cancel("Not yet implemented")
      }
      it("ori") {
        cancel("Not yet implemented")
      }
      it("andi") {
        cancel("Not yet implemented")
      }
      it("jalr") {
        cancel("Not yet implemented")
      }
    }
    describe("executes U-type RV32I instructions") {
      it("auipc") {
        cancel("Not yet implemented")
      }
      it("lui") {
        cancel("Not yet implemented")
      }
    }
    describe("executes S-type RV32I instructions") {
      it("sb") {
        cancel("Not yet implemented")
      }
      it("sh") {
        cancel("Not yet implemented")
      }
      it("sw") {
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
    }
    describe("executes R-type RV32I instructions") {
      it("add") {
        testRType(
          funct3 = 0,
          funct7_5 = false,
          rd = 7,
          rs1 = 27,
          rs2 = 17,
          rs1Value = 0xb8505d59,
          rs2Value = 0x6719e7a9,
          operation = (rs1, rs2) => rs1 + rs2
        )
      }
      it("sub") {
        testRType(
          funct3 = 0,
          funct7_5 = true,
          rd = 18,
          rs1 = 14,
          rs2 = 15,
          rs1Value = 0xdd729ea2,
          rs2Value = 0x2da48860,
          operation = (rs1, rs2) => rs1 - rs2
        )
      }
      it("sll") {
        cancel("Not yet implemented")
      }
      it("slt") {
        testRType(
          funct3 = 2,
          funct7_5 = false,
          rd = 20,
          rs1 = 31,
          rs2 = 11,
          rs1Value = 0xe680ac63,
          rs2Value = 0x33afcd8f,
          operation = (rs1, rs2) => if (toULong(rs1) < toULong(rs2)) 1 else 0
        )
      }
      it("sltu") {
        cancel("Not yet implemented")
      }
      it("xor") {
        cancel("Not yet implemented")
      }
      it("srl") {
        cancel("Not yet implemented")
      }
      it("sra") {
        cancel("Not yet implemented")
      }
      it("or") {
        testRType(
          funct3 = 6,
          funct7_5 = false,
          rd = 8,
          rs1 = 21,
          rs2 = 12,
          rs1Value = 0x4e39fafd,
          rs2Value = 0xf8df0e20,
          operation = (rs1, rs2) => rs1 | rs2
        )
      }
      it("and") {
        testRType(
          funct3 = 7,
          funct7_5 = false,
          rd = 9,
          rs1 = 6,
          rs2 = 10,
          rs1Value = 0xe9ac164c,
          rs2Value = 0xe279cb88,
          operation = (rs1, rs2) => rs1 & rs2
        )
      }
    }
    describe("executes B-type RV32I instructions") {
      it("beq") {
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
      it("bne") {
        cancel("Not yet implemented")
      }
      it("blt") {
        cancel("Not yet implemented")
      }
      it("bge") {
        cancel("Not yet implemented")
      }
      it("bltu") {
        cancel("Not yet implemented")
      }
      it("bgeu") {
        cancel("Not yet implemented")
      }
    }
    it("executes jal") {
      cancel("Not yet implemented")
    }
  }
  def testRType(
      funct3: Int,
      funct7_5: Boolean,
      rd: Int,
      rs1: Int,
      rs2: Int,
      rs1Value: Int,
      rs2Value: Int,
      operation: (Int, Int) => Int
  ): Unit = {
    val funct7 = (if (funct7_5) 1 << 5 else 0) << 25
    val rs2Shifted = (rs2 & 0x1f) << 20
    val rs1Shifted = (rs1 & 0x1f) << 15
    val funct3Shifted = (funct3 & 0x7) << 12
    val rdShifted = (rd & 0x1f) << 7
    val op = 51
    val instr = funct7 | rs2Shifted | rs1Shifted | funct3Shifted | rdShifted | op

    var registerFile = ArrayBuffer.fill(32)(0x00000000)
    registerFile(rs1) = rs1Value
    registerFile(rs2) = rs2Value
    var memory = ArrayBuffer.fill(64)(0x00000000)
    memory(0) = instr

    val expected = operation(rs1Value, rs2Value)

    simulate(
      new Perilus(
        initRegs = initMemFile(registerFile),
        initMem = initMemFile(memory),
        withDebug = true
      )
    ) { perilus =>
      {
        val perilusDebug = perilus.io.debug.get

        perilusDebug.memAddr.poke(0)
        perilusDebug.memData.expect(instr)
        perilusDebug.reg.poke(rs1)
        perilusDebug.regData.expect(toULong(rs1Value))
        perilusDebug.reg.poke(rs2)
        perilusDebug.regData.expect(toULong(rs2Value))
        perilusDebug.reg.poke(rd)
        perilusDebug.regData.expect(0)

        perilus.clock.step(4)
        perilus.io.pc.expect(4)

        perilusDebug.memAddr.poke(0)
        perilusDebug.memData.expect(instr)
        perilusDebug.reg.poke(rs1)
        perilusDebug.regData.expect(toULong(rs1Value))
        perilusDebug.reg.poke(rs2)
        perilusDebug.regData.expect(toULong(rs2Value))
        perilusDebug.reg.poke(rd)
        perilusDebug.regData.expect(toULong(expected))
      }
    }
  }
}
