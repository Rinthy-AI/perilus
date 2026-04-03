package com.rinthyAi.perilus.test.main

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import java.lang.Integer.{toUnsignedLong => toULong}
import org.scalatest.funspec.AnyFunSpec
import scala.collection.mutable.ArrayBuffer

import com.rinthyAi.perilus.main._
import com.rinthyAi.perilus.test.utils.TestUtils.initMemFile
import com.rinthyAi.perilus.controlUnit.ControlUnitState

class PerilusTests extends AnyFunSpec with ChiselSim {
  describe("Perilus") {
    describe("executes I-type RV32I instructions") {
      def assembleIType(
          funct3: Int,
          funct7_5: Boolean,
          rd: Int,
          rs1: Int,
          rs1Value: Int,
          imm: Int,
          op: Int
      ): Int = {
        val immWithFunct7_5Set = if (funct7_5) imm | (1 << 10) else imm
        val immShifted = (immWithFunct7_5Set & 0xfff) << 20
        val rs1Shifted = (rs1 & 0x1f) << 15
        val funct3Shifted = (funct3 & 0x7) << 12
        val rdShifted = (rd & 0x1f) << 7
        val opMasked = op & 0x7f
        immShifted | rs1Shifted | funct3Shifted | rdShifted | opMasked
      }
      def testIType(
          funct3: Int,
          funct7_5: Boolean,
          rd: Int,
          rs1: Int,
          rs1Value: Int,
          imm: Int,
          operation: (Int, Int) => Int,
          pc: Int = 4,
          opcode: Int = 19
      ): Unit = {
        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        registerFile(rs1) = rs1Value
        val memory = ArrayBuffer.fill(64)(0x00000000)
        val instr = assembleIType(funct3, funct7_5, rd, rs1, rs1Value, imm, opcode)
        memory(0) = instr

        val expected = operation(rs1Value, imm)

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
            perilusDebug.memData.expect(toULong(instr))
            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(toULong(rs1Value))
            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(if (rs1 == rd) toULong(rs1Value) else 0)

            perilus.clock.step(4)
            perilus.io.pc.expect(toULong(pc))

            perilusDebug.memAddr.poke(0)
            perilusDebug.memData.expect(toULong(instr))
            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(if (rs1 == rd) toULong(expected) else toULong(rs1Value))
            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(toULong(expected))
          }
        }
      }
      def testLoad(
          funct3: Int,
          rd: Int,
          rs1: Int,
          rs1Value: Int,
          imm: Int,
          memData: Int,
          dataMask: Int
      ): Unit = {
        val memAddr = rs1Value + imm
        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        registerFile(rs1) = rs1Value
        var memory = ArrayBuffer.fill(64)(0x00000000)
        memory(0) = assembleIType(funct3, false, rd, rs1, rs1Value, imm, 3)
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

            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(toULong(rs1Value))
            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(if (rd == rs1) toULong(rs1Value) else 0)
            perilusDebug.memAddr.poke(memAddr)
            perilusDebug.memData.expect(toULong(memData))

            perilus.clock.step(5)

            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(toULong(memData & dataMask))
            perilusDebug.memAddr.poke(memAddr)
            perilusDebug.memData.expect(toULong(memData))
          }
        }
      }
      it("lb") {
        testLoad(
          funct3 = 0,
          rd = 10,
          rs1 = 10,
          rs1Value = 0x14,
          imm = -0x07,
          memData = 0x5b9ccada,
          dataMask = 0x000000ff
        )
      }
      it("lh") {
        testLoad(
          funct3 = 1,
          rd = 31,
          rs1 = 14,
          rs1Value = 0x0e,
          imm = 0x14,
          memData = 0x8b9d4da4,
          dataMask = 0x0000ffff
        )
      }
      it("lw") {
        testLoad(
          funct3 = 2,
          rd = 21,
          rs1 = 11,
          rs1Value = 0x30,
          imm = 0xa8,
          memData = 0x0f3f9400,
          dataMask = 0xffffffff
        )
      }
      it("lbu") {
        testLoad(
          funct3 = 4,
          rd = 7,
          rs1 = 13,
          rs1Value = 0x3b,
          imm = 0x07,
          memData = 0x55c9a074,
          dataMask = 0x000000ff
        )
      }
      it("lhu") {
        testLoad(
          funct3 = 5,
          rd = 20,
          rs1 = 4,
          rs1Value = 0x25,
          imm = 0x2f,
          memData = 0xb3fecaab,
          dataMask = 0x0000ffff
        )
      }
      it("addi") {
        testIType(
          funct3 = 0,
          funct7_5 = false,
          rd = 17,
          rs1 = 27,
          rs1Value = 0x60f49452,
          imm = 0x3d,
          operation = (rs1, imm) => rs1 + imm
        )
      }
      it("slli") {
        testIType(
          funct3 = 1,
          funct7_5 = false,
          rd = 12,
          rs1 = 20,
          rs1Value = 0x27a091d1,
          imm = 0x009,
          operation = (rs1, imm) => rs1 << imm.min(32)
        )
      }
      it("slti") {
        testIType(
          funct3 = 2,
          funct7_5 = false,
          rd = 27,
          rs1 = 7,
          rs1Value = 0x451753fc,
          imm = 0x24e,
          operation = (rs1, imm) => if (rs1 < imm) 1 else 0
        )
      }
      it("sltiu") {
        testIType(
          funct3 = 3,
          funct7_5 = false,
          rd = 1,
          rs1 = 24,
          rs1Value = 0x9efde28d,
          imm = 0xdf6,
          operation = (rs1, imm) => if (toULong(rs1) < toULong(imm)) 1 else 0
        )
      }
      it("xori") {
        testIType(
          funct3 = 4,
          funct7_5 = false,
          rd = 4,
          rs1 = 4,
          rs1Value = 0x1649835d,
          imm = 0x9fa,
          operation = (rs1, imm) => rs1 ^ (if ((imm & (1 << 11)) != 0) 0xfffff000 | imm else imm)
        )
      }
      it("srli") {
        testIType(
          funct3 = 5,
          funct7_5 = false,
          rd = 2,
          rs1 = 3,
          rs1Value = 0x2d178ee5,
          imm = 26,
          operation = (rs1, imm) => rs1 >>> imm.min(32)
        )
      }
      it("srai") {
        testIType(
          funct3 = 5,
          funct7_5 = true,
          rd = 21,
          rs1 = 19,
          rs1Value = 0xb2cb537c,
          imm = 7,
          operation = (rs1, imm) => rs1 >> imm.min(32)
        )
      }
      it("ori") {
        testIType(
          funct3 = 6,
          funct7_5 = true,
          rd = 28,
          rs1 = 18,
          rs1Value = 0xc5212c23,
          imm = 0xe4,
          operation = (rs1, imm) => rs1 | imm
        )
      }
      it("andi") {
        testIType(
          funct3 = 7,
          funct7_5 = true,
          rd = 12,
          rs1 = 1,
          rs1Value = 0x275157ac,
          imm = 0x692,
          operation = (rs1, imm) => rs1 & imm
        )
      }
      it("jalr") {
        testIType(
          funct3 = 0,
          funct7_5 = false,
          rd = 3,
          rs1 = 8,
          rs1Value = 32,
          imm = 10,
          operation = (_rs1, _imm) => 4,
          pc = 42,
          opcode = 103
        )
      }
    }
    describe("executes U-type RV32I instructions") {
      def testUType(
          imm: Int,
          rd: Int,
          op: Int
      ) = {
        val immMasked = (imm & 0xfffff000)
        val rdShifted = (rd & 0x1f) << 7
        val instr = immMasked | rdShifted | op

        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        var memory = ArrayBuffer.fill(64)(0x00000000)
        val test_pc = 48
        memory(0) = 0x02000863 // beq x0, x0, 48
        memory(test_pc / 4) = instr
        val expected = if (op == 0x17) { immMasked + test_pc }
        else { immMasked }

        simulate(
          new Perilus(
            initRegs = initMemFile(registerFile),
            initMem = initMemFile(memory),
            withDebug = true
          )
        ) { perilus =>
          {
            val perilusDebug = perilus.io.debug.get
            perilusDebug.memAddr.poke(test_pc)
            perilusDebug.memData.expect(toULong(instr))
            perilus.clock.step(6)
            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(toULong(expected))
          }
        }
      }
      it("auipc") {
        testUType(
          imm = 0xf5540000,
          rd = 29,
          op = 0x17
        )
      }
      it("lui") {
        testUType(
          imm = 0xc6fed000,
          rd = 21,
          op = 0x37
        )
      }
    }
    describe("executes S-type RV32I instructions") {
      def testSType(
          funct3: Int,
          rs1: Int,
          rs1Value: Int,
          rs2: Int,
          rs2Value: Int,
          imm: Int,
          dataMask: Int
      ): Unit = {
        val immUpper = (imm & 0xfe0) << 20
        val rs2Shifted = (rs2 & 0x1f) << 20
        val rs1Shifted = (rs1 & 0x1f) << 15
        val funct3Shifted = (funct3 & 0x7) << 12
        val immLower = (imm & 0x1f) << 7
        val op = 0x23
        val instr = immUpper | rs2Shifted | rs1Shifted | funct3Shifted | immLower | op

        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        registerFile(rs1) = rs1Value
        registerFile(rs2) = rs2Value
        var memory = ArrayBuffer.fill(64)(0x00000000)
        memory(0) = instr
        val memAddr = rs1Value + imm

        simulate(
          new Perilus(
            initRegs = initMemFile(registerFile),
            initMem = initMemFile(memory),
            withDebug = true
          )
        ) { perilus =>
          {
            val perilusDebug = perilus.io.debug.get

            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(toULong(rs1Value))
            perilusDebug.reg.poke(rs2)
            perilusDebug.regData.expect(toULong(rs2Value))
            perilusDebug.memAddr.poke(memAddr)
            assert(perilusDebug.memData.peek().litValue != toULong(rs2Value & dataMask))

            perilus.clock.step(4)

            perilusDebug.memAddr.poke(memAddr)
            perilusDebug.memData.expect(toULong(rs2Value & dataMask))
          }
        }
      }
      it("sb") {
        testSType(
          funct3 = 0,
          rs1 = 17,
          rs1Value = 0x1c,
          rs2 = 21,
          rs2Value = 0x52ba341a,
          imm = -0x18,
          dataMask = 0x000000ff
        )
      }
      it("sh") {
        testSType(
          funct3 = 1,
          rs1 = 4,
          rs1Value = 0x21,
          rs2 = 6,
          rs2Value = 0x02d3b5bd,
          imm = -0x9,
          dataMask = 0x0000ffff
        )
      }
      it("sw") {
        testSType(
          funct3 = 2,
          rs1 = 9,
          rs1Value = 0x38,
          rs2 = 6,
          rs2Value = 0x01830169,
          imm = -0x4,
          dataMask = 0xffffffff
        )
      }
    }
    describe("executes R-type RV32I instructions") {
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
            perilusDebug.memData.expect(toULong(instr))
            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(toULong(rs1Value))
            perilusDebug.reg.poke(rs2)
            perilusDebug.regData.expect(toULong(rs2Value))
            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(
              if (rd == rs1) toULong(rs1Value) else if (rd == rs2) toULong(rs2Value) else 0
            )

            perilus.clock.step(4)
            perilus.io.pc.expect(4)

            perilusDebug.memAddr.poke(0)
            perilusDebug.memData.expect(toULong(instr))
            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(if (rd == rs1) toULong(expected) else toULong(rs1Value))
            perilusDebug.reg.poke(rs2)
            perilusDebug.regData.expect(if (rd == rs2) toULong(expected) else toULong(rs2Value))
            perilusDebug.reg.poke(rd)
            perilusDebug.regData.expect(toULong(expected))
          }
        }
      }
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
        testRType(
          funct3 = 1,
          funct7_5 = false,
          rd = 21,
          rs1 = 24,
          rs2 = 25,
          rs1Value = 0xd021eb67,
          rs2Value = 17,
          operation = (rs1, rs2) => rs1 << rs2.min(32)
        )
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
          operation = (rs1, rs2) => if (rs1 < rs2) 1 else 0
        )
      }
      it("sltu") {
        testRType(
          funct3 = 3,
          funct7_5 = false,
          rd = 13,
          rs1 = 25,
          rs2 = 31,
          rs1Value = 0x2ea46594,
          rs2Value = 0xb4258409,
          operation = (rs1, rs2) => if (toULong(rs1) < toULong(rs2)) 1 else 0
        )
      }
      it("xor") {
        testRType(
          funct3 = 4,
          funct7_5 = false,
          rd = 12,
          rs1 = 10,
          rs2 = 18,
          rs1Value = 0xc52f9cac,
          rs2Value = 0xd2a4c0dc,
          operation = (rs1, rs2) => rs1 ^ rs2
        )
      }
      it("srl") {
        testRType(
          funct3 = 5,
          funct7_5 = false,
          rd = 26,
          rs1 = 2,
          rs2 = 25,
          rs1Value = 0x3fa60574,
          rs2Value = 29,
          operation = (rs1, rs2) => rs1 >>> rs2.min(32)
        )
      }
      it("sra") {
        testRType(
          funct3 = 5,
          funct7_5 = true,
          rd = 30,
          rs1 = 19,
          rs2 = 5,
          rs1Value = 0xee91cf2d,
          rs2Value = 10,
          operation = (rs1, rs2) => rs1 >> rs2.min(32)
        )
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
      def testBType(
          funct3: Int,
          rs1: Int,
          rs1Value: Int,
          rs2: Int,
          rs2Value: Int,
          imm: Int,
          takeBranch: Boolean
      ) = {
        val immUpper = (((imm & 0x1000) >> 6) | (imm & 0x7e0) >> 5) << 25
        val rs2Shifted = (rs2 & 0x1f) << 20
        val rs1Shifted = (rs1 & 0x1f) << 15
        val funct3Shifted = (funct3 & 0x7) << 12
        val immLower = ((imm & 0x1e) | ((imm & 0x800) >> 11)) << 7
        val op = 0x63
        val instr = immUpper | rs2Shifted | rs1Shifted | funct3Shifted | immLower | op

        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        registerFile(rs1) = rs1Value
        registerFile(rs2) = rs2Value
        var memory = ArrayBuffer.fill(64)(0x00000000)
        memory(0) = instr

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
            perilusDebug.memData.expect(toULong(instr))
            perilusDebug.reg.poke(rs1)
            perilusDebug.regData.expect(toULong(rs1Value))
            perilusDebug.reg.poke(rs2)
            perilusDebug.regData.expect(toULong(rs2Value))

            perilus.clock.step(3)

            if (takeBranch) {
              perilus.io.pc.expect(toULong(imm))
            } else {
              perilus.io.pc.expect(4.U)
            }
          }
        }
      }
      it("beq") {
        testBType(
          funct3 = 0,
          rs1 = 3,
          rs1Value = 0xe62cca72,
          rs2 = 6,
          rs2Value = 0xe62cca72,
          imm = 0x548,
          takeBranch = true
        )
        testBType(
          funct3 = 0,
          rs1 = 13,
          rs1Value = 0x9eec72e3,
          rs2 = 4,
          rs2Value = 0xb7899861,
          imm = 0x794,
          takeBranch = false
        )
      }
      it("bne") {
        testBType(
          funct3 = 1,
          rs1 = 26,
          rs1Value = 0x980022bb,
          rs2 = 18,
          rs2Value = 0xc9b29fd7,
          imm = 0x272,
          takeBranch = true
        )
        testBType(
          funct3 = 1,
          rs1 = 17,
          rs1Value = 0x9eec72e3,
          rs2 = 18,
          rs2Value = 0x9eec72e3,
          imm = -0x2b4,
          takeBranch = false
        )
      }
      it("blt") {
        testBType(
          funct3 = 4,
          rs1 = 14,
          rs1Value = -0x787a0531,
          rs2 = 25,
          rs2Value = -0x3f9c200f,
          imm = -0x418,
          takeBranch = true
        )
        testBType(
          funct3 = 4,
          rs1 = 4,
          rs1Value = 0x68c0f4c7,
          rs2 = 5,
          rs2Value = 0x4fadf777,
          imm = 0x162,
          takeBranch = false
        )
      }
      it("bge") {
        testBType(
          funct3 = 5,
          rs1 = 1,
          rs1Value = 0x3f18402f,
          rs2 = 4,
          rs2Value = -0x5f292dce,
          imm = 0x6a8,
          takeBranch = true
        )
        testBType(
          funct3 = 5,
          rs1 = 4,
          rs1Value = -0x6c409e00,
          rs2 = 30,
          rs2Value = 0x7572f2ef,
          imm = 0x364,
          takeBranch = false
        )
      }
      it("bltu") {
        testBType(
          funct3 = 6,
          rs1 = 28,
          rs1Value = 0xc881d8e2,
          rs2 = 15,
          rs2Value = 0xfbad6f5e,
          imm = -0x3b8,
          takeBranch = true
        )
        testBType(
          funct3 = 6,
          rs1 = 28,
          rs1Value = 0xf85eb707,
          rs2 = 1,
          rs2Value = 0x97f574ad,
          imm = -0x1b0,
          takeBranch = false
        )
      }
      it("bgeu") {
        testBType(
          funct3 = 7,
          rs1 = 7,
          rs1Value = 0xcac58493,
          rs2 = 19,
          rs2Value = 0xbbaaac85,
          imm = -0x4dc,
          takeBranch = true
        )
        testBType(
          funct3 = 7,
          rs1 = 7,
          rs1Value = 0x8530cd57,
          rs2 = 19,
          rs2Value = 0x90f720e9,
          imm = 0x756,
          takeBranch = false
        )
      }
    }
    describe("skips environment instructions") {
      def testNop(instruction: Int): Unit = {
        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        var memory = ArrayBuffer.fill(64)(0x00000000)
        memory(0) = instruction
        simulate(
          new Perilus(
            initRegs = initMemFile(registerFile),
            initMem = initMemFile(memory),
            withDebug = true
          )
        ) { perilus =>
          {
            val perilusDebug = perilus.io.debug.get
            perilus.io.pc.expect(0.U)
            perilusDebug.state.expect(ControlUnitState.fetch)
            perilus.clock.step(2)
            perilus.io.pc.expect(4.U)
            perilusDebug.state.expect(ControlUnitState.fetch)
          }
        }
      }
      it("ecall") {
        testNop(0x00000073)
      }
      it("ebreak") {
        testNop(0x00100073)
      }
      it("fence") {
        testNop(0x0000000f)
      }
      it("fence.tso") {
        testNop(0x8330000f)
      }
      it("pause") {
        testNop(0x0100000f)
      }
    }
    it("executes jal") {
      var registerFile = ArrayBuffer.fill(32)(0x00000000)
      var memory = ArrayBuffer.fill(64)(0x00000000)
      memory(0) = 0x01e003ef // jal x7, 0x1e
      simulate(
        new Perilus(
          initRegs = initMemFile(registerFile),
          initMem = initMemFile(memory),
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          perilus.io.pc.expect(0)
          perilusDebug.reg.poke(7)
          perilusDebug.regData.expect(0)

          perilus.clock.step(4)

          perilus.io.pc.expect(0x1e)
          perilusDebug.reg.poke(7)
          perilusDebug.regData.expect(4)
        }
      }
    }
    describe("runs small test programs") {
      it("20th Fibonacci number") {
        var registerFile = ArrayBuffer.fill(32)(0x00000000)
        registerFile(10) = 20
        var memory = ArrayBuffer.fill(64)(0x00000000)
        // compute the nth Fibonacci number, for 2 <= n <= 47
        // a0 => n (argument), output (return)
        // t0 => counter (n -> 0)
        // t1 => a
        // t2 => b
        memory(0) = 0x000502b3 // add t0, a0, zero     # counter = n
        memory(1) = 0xffe28293 // addi t0, t0, -2      # counter -= 2
        memory(2) = 0x00100313 // addi t1, zero, 1     # a = 1
        memory(3) = 0x000003b3 // add t2, zero, zero   # b = 0
        memory(4) = 0x00028c63 // beq t0, zero, 24     # break if counter == 0
        memory(5) = 0x00730533 // add a0, t1, t2       # output = a + b
        memory(6) = 0x000303b3 // add t2, t1, zero     # b = a
        memory(7) = 0x00050333 // add t1, a0, zero     # a = output
        memory(8) = 0xfff28293 // addi t0, t0, -1      # counter -= 1
        memory(9) = 0xfe0006e3 // beq zero, zero, -20  # loop
        memory(10) = 0x000002b3 // add t0, zero, zero   # done

        simulate(
          new Perilus(
            initRegs = initMemFile(registerFile),
            initMem = initMemFile(memory),
            withDebug = true
          )
        ) { perilus =>
          {
            val perilusDebug = perilus.io.debug.get
            while (perilus.io.pc.peek().litValue != 44) { perilus.clock.step() }
            perilusDebug.reg.poke(10)
            perilusDebug.regData.expect(4181)
          }
        }
      }
    }
  }
}
