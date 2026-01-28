package com.rinthyAi.perilus.test.alu

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

import com.rinthyAi.perilus.alu._

class AluTests extends AnyFunSpec with ChiselSim {
  describe("Alu") {
    it("adds") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.add)

          alu.io.srcA.poke("hb589e285".U)
          alu.io.srcB.poke("hc4289503".U)
          alu.io.aluResult.expect("h79b27788".U)

          alu.io.srcA.poke("hfa66f344".U)
          alu.io.srcB.poke("h7ff47b44".U)
          alu.io.aluResult.expect("h7a5b6e88".U)

          alu.io.srcA.poke("h644a658e".U)
          alu.io.srcB.poke("hb90e62f7".U)
          alu.io.aluResult.expect("h1d58c885".U)
        }
      }
    }
    it("subtracts") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.sub)

          alu.io.srcA.poke("hceab249".U)
          alu.io.srcB.poke("h124a55c3".U)
          alu.io.aluResult.expect("hfaa05c86".U)

          alu.io.srcA.poke("h819518ce".U)
          alu.io.srcB.poke("h9e8302de".U)
          alu.io.aluResult.expect("he31215f0".U)

          alu.io.srcA.poke("h5d49ddb8".U)
          alu.io.srcB.poke("h6f715aa2".U)
          alu.io.aluResult.expect("hedd88316".U)
        }
      }
    }
    it("computes bitwise AND") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.and)

          alu.io.srcA.poke("h9ae8ddd".U)
          alu.io.srcB.poke("ha8387b0c".U)
          alu.io.aluResult.expect("h828090c".U)

          alu.io.srcA.poke("h96f36a94".U)
          alu.io.srcB.poke("h52e96078".U)
          alu.io.aluResult.expect("h12e16010".U)

          alu.io.srcA.poke("hf9209575".U)
          alu.io.srcB.poke("h931544ba".U)
          alu.io.aluResult.expect("h91000430".U)
        }
      }
    }
    it("computes bitwise OR") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.or)

          alu.io.srcA.poke("hf4f16d52".U)
          alu.io.srcB.poke("h2f6464a1".U)
          alu.io.aluResult.expect("hfff56df3".U)

          alu.io.srcA.poke("h4ab77cc3".U)
          alu.io.srcB.poke("ha04edae1".U)
          alu.io.aluResult.expect("heafffee3".U)

          alu.io.srcA.poke("hcab1596a".U)
          alu.io.srcB.poke("hf5251288".U)
          alu.io.aluResult.expect("hffb55bea".U)
        }
      }
    }
    it("computes set if less than (SLT)") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.slt)

          alu.io.srcA.poke("h8a831306".U)
          alu.io.srcB.poke("h6e7ec46f".U)
          alu.io.aluResult.expect(0.U)

          alu.io.srcA.poke("hcd30f9c7".U)
          alu.io.srcB.poke("hb15d24bd".U)
          alu.io.aluResult.expect(0.U)

          alu.io.srcA.poke("h33b6db74".U)
          alu.io.srcB.poke("hd6faaa4d".U)
          alu.io.aluResult.expect(1.U)
        }
      }
    }
  }
}
