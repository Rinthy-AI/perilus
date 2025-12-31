import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

import com.rinthyAi.perilus.alu._
import com.rinthyAi.perilus.controlUnit._
import com.rinthyAi.perilus.extendUnit._

class ControlUnitTests extends AnyFunSpec with ChiselSim {
  def checkFsmStates(dut: ControlUnit, states: List[ControlUnitState.Type]) = {
    val debug = dut.io.debug.get
    debug.state.expect(states.head)
    states.tail.foreach(state => {
      dut.clock.step(1)
      debug.state.expect(state)
    })
    dut.clock.step(1)
    debug.state.expect(states.head)
  }
  describe("AluDecoder") {
    it("decodes correctly") {
      simulate(new AluDecoder) { aluDecoder =>
        {
          aluDecoder.io.funct3.poke(0.U)
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
          aluDecoder.io.funct7_5.poke(true.B)
          aluDecoder.io.aluControl.expect(AluControl.sub)
          aluDecoder.io.funct3.poke("b010".U)
          aluDecoder.io.aluControl.expect(AluControl.slt)
          aluDecoder.io.funct3.poke("b110".U)
          aluDecoder.io.aluControl.expect(AluControl.or)
          aluDecoder.io.funct3.poke("b111".U)
          aluDecoder.io.aluControl.expect(AluControl.and)
        }
      }
    }
  }
  describe("ControlUnit") {
    it("controls the beq instruction") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          controlUnit.io.op.poke(Opcode.branch)
          controlUnit.io.funct3.poke(0.U)
          controlUnit.io.funct7_5.poke(false.B)
          controlUnit.io.zero.poke(false.B)

          val states = List(
            ControlUnitState.fetch,
            ControlUnitState.decode,
            ControlUnitState.beq
          )

          checkFsmStates(controlUnit, states)
        }
      }
    }
    it("controls the jal instruction") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          controlUnit.io.op.poke(Opcode.jal)
          controlUnit.io.funct3.poke(0.U)
          controlUnit.io.funct7_5.poke(false.B)
          controlUnit.io.zero.poke(false.B)

          val states = List(
            ControlUnitState.fetch,
            ControlUnitState.decode,
            ControlUnitState.jal,
            ControlUnitState.aluWb
          )

          checkFsmStates(controlUnit, states)
        }
      }
    }
    it("controls the lw instruction") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          controlUnit.io.op.poke(Opcode.load)
          controlUnit.io.funct3.poke(2.U)
          controlUnit.io.funct7_5.poke(false.B)
          controlUnit.io.zero.poke(false.B)

          val states = List(
            ControlUnitState.fetch,
            ControlUnitState.decode,
            ControlUnitState.memAdr,
            ControlUnitState.memRead,
            ControlUnitState.memWb
          )

          checkFsmStates(controlUnit, states)
        }
      }
    }
    it("controls the or instruction") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          controlUnit.io.op.poke(Opcode.rType)
          controlUnit.io.funct3.poke(6.U)
          controlUnit.io.funct7_5.poke(false.B)
          controlUnit.io.zero.poke(false.B)

          val states = List(
            ControlUnitState.fetch,
            ControlUnitState.decode,
            ControlUnitState.executeR,
            ControlUnitState.aluWb
          )

          checkFsmStates(controlUnit, states)
        }
      }
    }
    it("controls the slti instruction") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          controlUnit.io.op.poke(Opcode.immediate)
          controlUnit.io.funct3.poke(2.U)
          controlUnit.io.funct7_5.poke(false.B)
          controlUnit.io.zero.poke(false.B)

          val states = List(
            ControlUnitState.fetch,
            ControlUnitState.decode,
            ControlUnitState.executeI,
            ControlUnitState.aluWb
          )

          checkFsmStates(controlUnit, states)
        }
      }
    }
    it("controls the sw instruction") {
      simulate(new ControlUnit(withDebug = true)) { controlUnit =>
        {
          controlUnit.io.op.poke(Opcode.store)
          controlUnit.io.funct3.poke(2.U)
          controlUnit.io.funct7_5.poke(false.B)
          controlUnit.io.zero.poke(false.B)

          val states = List(
            ControlUnitState.fetch,
            ControlUnitState.decode,
            ControlUnitState.memAdr,
            ControlUnitState.memWrite
          )

          checkFsmStates(controlUnit, states)
        }
      }
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
