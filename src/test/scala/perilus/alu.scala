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
    it("shifts left") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.sll)

          alu.io.srcA.poke("h3baf038e".U)
          alu.io.srcB.poke(19.U)
          alu.io.aluResult.expect("h1c700000".U)

          alu.io.srcA.poke("h1d6f9fae".U)
          alu.io.srcB.poke(7.U)
          alu.io.aluResult.expect("hb7cfd700".U)

          alu.io.srcA.poke("h21c69d4f".U)
          alu.io.srcB.poke(29.U)
          alu.io.aluResult.expect("he0000000".U)
        }
      }
    }
    it("computes set if less than (SLT)") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.slt)

          alu.io.srcA.poke("h8a831306".U)
          alu.io.srcB.poke("h6e7ec46f".U)
          alu.io.aluResult.expect(1.U)

          alu.io.srcA.poke("hcd30f9c7".U)
          alu.io.srcB.poke("hb15d24bd".U)
          alu.io.aluResult.expect(0.U)

          alu.io.srcA.poke("h33b6db74".U)
          alu.io.srcB.poke("hd6faaa4d".U)
          alu.io.aluResult.expect(0.U)
        }
      }
    }
    it("computes set if less than unsigned (SLTU)") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.sltu)

          alu.io.srcA.poke("h3c22f6f3".U)
          alu.io.srcB.poke("ha2fec497".U)
          alu.io.aluResult.expect(1.U)

          alu.io.srcA.poke("hbdb58b94".U)
          alu.io.srcB.poke("h2d3d7d3e".U)
          alu.io.aluResult.expect(0.U)

          alu.io.srcA.poke("hed304348".U)
          alu.io.srcB.poke("hc5770573".U)
          alu.io.aluResult.expect(0.U)
        }
      }
    }
    it("computes XOR") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.xor)

          alu.io.srcA.poke("h55e732a9".U)
          alu.io.srcB.poke("h5ba7c1f3".U)
          alu.io.aluResult.expect("h0e40f35a".U)

          alu.io.srcA.poke("he3bf6c67".U)
          alu.io.srcB.poke("he040b522".U)
          alu.io.aluResult.expect("h03ffd945".U)

          alu.io.srcA.poke("h335724c7".U)
          alu.io.srcB.poke("h60b5dfe0".U)
          alu.io.aluResult.expect("h53e2fb27".U)
        }
      }
    }
    it("shifts right (logical)") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.srl)

          alu.io.srcA.poke("ha428d136".U)
          alu.io.srcB.poke(21.U)
          alu.io.aluResult.expect("h00000521".U)

          alu.io.srcA.poke("hada24983".U)
          alu.io.srcB.poke(3.U)
          alu.io.aluResult.expect("h15b44930".U)

          alu.io.srcA.poke("h9c393ad6".U)
          alu.io.srcB.poke(8.U)
          alu.io.aluResult.expect("h009c393a".U)
        }
      }
    }
    it("shifts right (arithmetic)") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.sra)

          alu.io.srcA.poke("h7d803c74".U)
          alu.io.srcB.poke(26.U)
          alu.io.aluResult.expect("h0000001f".U)

          alu.io.srcA.poke("haaecbd03".U)
          alu.io.srcB.poke(9.U)
          alu.io.aluResult.expect("hffd5765e".U)

          alu.io.srcA.poke("ha5113a05".U)
          alu.io.srcB.poke(26.U)
          alu.io.aluResult.expect("hffffffe9".U)
        }
      }
    }
    it("computes OR") {
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
    it("computes AND") {
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
    it("sets the zero flag") {
      simulate(new Alu) { alu =>
        {
          alu.io.aluControl.poke(AluControl.sub)

          alu.io.srcA.poke("h3dd9e4a3".U)
          alu.io.srcB.poke("hb4a64831".U)
          alu.io.zero.expect(false.B)

          alu.io.srcA.poke("h157aa03a".U)
          alu.io.srcB.poke("h157aa03a".U)
          alu.io.zero.expect(true.B)
        }
      }
    }
  }
}
