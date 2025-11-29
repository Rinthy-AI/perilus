import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

import com.rinthyAi.perilus.alu._

class AluTests extends AnyFunSpec with ChiselSim {
  describe("Alu") {
    it("adds") {
      simulate(new Alu) { extendUnit =>
        {
          extendUnit.io.aluControl.poke("b000".U)

          extendUnit.io.srcA.poke("hb589e285".U)
          extendUnit.io.srcB.poke("hc4289503".U)
          extendUnit.io.aluResult.expect("h79b27788".U)

          extendUnit.io.srcA.poke("hfa66f344".U)
          extendUnit.io.srcB.poke("h7ff47b44".U)
          extendUnit.io.aluResult.expect("h7a5b6e88".U)

          extendUnit.io.srcA.poke("h644a658e".U)
          extendUnit.io.srcB.poke("hb90e62f7".U)
          extendUnit.io.aluResult.expect("h1d58c885".U)
        }
      }
    }
    it("subtracts") {
      simulate(new Alu) { extendUnit =>
        {
          extendUnit.io.aluControl.poke("b001".U)

          extendUnit.io.srcA.poke("hceab249".U)
          extendUnit.io.srcB.poke("h124a55c3".U)
          extendUnit.io.aluResult.expect("hfaa05c86".U)

          extendUnit.io.srcA.poke("h819518ce".U)
          extendUnit.io.srcB.poke("h9e8302de".U)
          extendUnit.io.aluResult.expect("he31215f0".U)

          extendUnit.io.srcA.poke("h5d49ddb8".U)
          extendUnit.io.srcB.poke("h6f715aa2".U)
          extendUnit.io.aluResult.expect("hedd88316".U)
        }
      }
    }
    it("computes bitwise AND") {
      simulate(new Alu) { extendUnit =>
        {
          extendUnit.io.aluControl.poke("b010".U)

          extendUnit.io.srcA.poke("h9ae8ddd".U)
          extendUnit.io.srcB.poke("ha8387b0c".U)
          extendUnit.io.aluResult.expect("h828090c".U)

          extendUnit.io.srcA.poke("h96f36a94".U)
          extendUnit.io.srcB.poke("h52e96078".U)
          extendUnit.io.aluResult.expect("h12e16010".U)

          extendUnit.io.srcA.poke("hf9209575".U)
          extendUnit.io.srcB.poke("h931544ba".U)
          extendUnit.io.aluResult.expect("h91000430".U)
        }
      }
    }
    it("computes bitwise OR") {
      simulate(new Alu) { extendUnit =>
        {
          extendUnit.io.aluControl.poke("b011".U)

          extendUnit.io.srcA.poke("hf4f16d52".U)
          extendUnit.io.srcB.poke("h2f6464a1".U)
          extendUnit.io.aluResult.expect("hfff56df3".U)

          extendUnit.io.srcA.poke("h4ab77cc3".U)
          extendUnit.io.srcB.poke("ha04edae1".U)
          extendUnit.io.aluResult.expect("heafffee3".U)

          extendUnit.io.srcA.poke("hcab1596a".U)
          extendUnit.io.srcB.poke("hf5251288".U)
          extendUnit.io.aluResult.expect("hffb55bea".U)
        }
      }
    }
    it("computes set if less than (SLT)") {
      simulate(new Alu) { extendUnit =>
        {
          extendUnit.io.aluControl.poke("b101".U)

          extendUnit.io.srcA.poke("h8a831306".U)
          extendUnit.io.srcB.poke("h6e7ec46f".U)
          extendUnit.io.aluResult.expect(0.U)

          extendUnit.io.srcA.poke("hcd30f9c7".U)
          extendUnit.io.srcB.poke("hb15d24bd".U)
          extendUnit.io.aluResult.expect(0.U)

          extendUnit.io.srcA.poke("h33b6db74".U)
          extendUnit.io.srcB.poke("hd6faaa4d".U)
          extendUnit.io.aluResult.expect(1.U)
        }
      }
    }
    it("outputs zero when aluControl is invalid") {
      simulate(new Alu) { extendUnit =>
        {
          extendUnit.io.aluControl.poke("b100".U)

          extendUnit.io.srcA.poke("hbe4fb61f".U)
          extendUnit.io.srcB.poke("h1ea6db5b".U)
          extendUnit.io.aluResult.expect(0.U)

          extendUnit.io.aluControl.poke("b110".U)

          extendUnit.io.srcA.poke("h9ba3e41f".U)
          extendUnit.io.srcB.poke("h33abf507".U)
          extendUnit.io.aluResult.expect(0.U)

          extendUnit.io.aluControl.poke("b111".U)

          extendUnit.io.srcA.poke("h6cd9a65b".U)
          extendUnit.io.srcB.poke("h104e5235".U)
          extendUnit.io.aluResult.expect(0.U)
        }
      }
    }
  }
}
