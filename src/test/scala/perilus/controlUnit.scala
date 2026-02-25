package com.rinthyAi.perilus.test.controlUnit

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

import com.rinthyAi.perilus.alu._
import com.rinthyAi.perilus.controlUnit._
import com.rinthyAi.perilus.extendUnit._

class ControlUnitTests extends AnyFunSpec with ChiselSim {
  describe("AluDecoder") {
    it("decodes correctly") {
      simulate(new AluDecoder) { aluDecoder =>
        {
          aluDecoder.io.funct3.poke("b000".U)
          aluDecoder.io.funct7_5.poke(false.B)
          aluDecoder.io.op.poke(Opcode.rType)

          aluDecoder.io.aluOp.poke(AluOp.memory)
          aluDecoder.io.aluControl.expect(AluControl.add)

          aluDecoder.io.aluOp.poke(AluOp.branch)
          aluDecoder.io.aluControl.expect(AluControl.sub)

          aluDecoder.io.aluOp.poke(AluOp.arithmetic)
          aluDecoder.io.op.poke(Opcode.immediate)
          aluDecoder.io.aluControl.expect(AluControl.add)
          aluDecoder.io.op.poke(Opcode.rType)
          aluDecoder.io.aluControl.expect(AluControl.add)
          aluDecoder.io.funct7_5.poke(true.B)
          aluDecoder.io.aluControl.expect(AluControl.sub)
          aluDecoder.io.funct3.poke("b001".U)
          aluDecoder.io.aluControl.expect(AluControl.sll)
          aluDecoder.io.funct3.poke("b010".U)
          aluDecoder.io.aluControl.expect(AluControl.slt)
          aluDecoder.io.funct3.poke("b011".U)
          aluDecoder.io.aluControl.expect(AluControl.sltu)
          aluDecoder.io.funct3.poke("b100".U)
          aluDecoder.io.aluControl.expect(AluControl.xor)
          aluDecoder.io.funct3.poke("b101".U)
          aluDecoder.io.aluControl.expect(AluControl.sra)
          aluDecoder.io.funct7_5.poke(false.B)
          aluDecoder.io.aluControl.expect(AluControl.srl)
          aluDecoder.io.funct3.poke("b110".U)
          aluDecoder.io.aluControl.expect(AluControl.or)
          aluDecoder.io.funct3.poke("b111".U)
          aluDecoder.io.aluControl.expect(AluControl.and)
        }
      }
    }
  }
  describe("ControlUnit") {
    it("advances from Fetch to Decode") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          val controlUnitDebug = controlUnit.io.debug.get
          controlUnitDebug.state.expect(ControlUnitState.fetch)
          controlUnit.clock.step()
          controlUnitDebug.state.expect(ControlUnitState.decode)
        }
      }
    }
    it("advances from Decode to MemAdr for loads and stores") {
      cancel("Not yet implemented")
    }
    it("advances from Decode to ExecuteR for R-types") {
      cancel("Not yet implemented")
    }
    it("advances from Decode to ExecuteI for I-types") {
      cancel("Not yet implemented")
    }
    it("advances from Decode to Jal for jal instruction") {
      cancel("Not yet implemented")
    }
    it("advances from Decode to Branch for B-types") {
      cancel("Not yet implemented")
    }
    it("advances from MemAdr to MemRead for loads") {
      cancel("Not yet implemented")
    }
    it("advances from MemAdr to MemWrite for stores") {
      cancel("Not yet implemented")
    }
    it("advances from MemRead to MemWb") {
      cancel("Not yet implemented")
    }
    it("advances from MemWb to Fetch") {
      cancel("Not yet implemented")
    }
    it("advances from MemWrite to Fetch") {
      cancel("Not yet implemented")
    }
    it("advances from ExecuteR to AluWb") {
      cancel("Not yet implemented")
    }
    it("advances from AluWb to Fetch") {
      cancel("Not yet implemented")
    }
    it("advances from ExecuteI to AluWb") {
      cancel("Not yet implemented")
    }
    it("advances from Jal to AluWb") {
      cancel("Not yet implemented")
    }
    it("advances from Branch to Fetch") {
      cancel("Not yet implemented")
    }
  }
  describe("InstructionDecoder") {
    it("decodes correctly") {
      simulate(new InstructionDecoder) { instrDecoder =>
        {
          instrDecoder.io.op.poke(Opcode.load)
          instrDecoder.io.immSrc.expect(ImmSrc.iType)
          instrDecoder.io.op.poke(Opcode.store)
          instrDecoder.io.immSrc.expect(ImmSrc.sType)
          instrDecoder.io.op.poke(Opcode.branch)
          instrDecoder.io.immSrc.expect(ImmSrc.bType)
          instrDecoder.io.op.poke(Opcode.immediate)
          instrDecoder.io.immSrc.expect(ImmSrc.iType)
          instrDecoder.io.op.poke(Opcode.jal)
          instrDecoder.io.immSrc.expect(ImmSrc.jType)
        }
      }
    }
  }
}
