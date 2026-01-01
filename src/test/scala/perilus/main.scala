import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec
import scala.collection.mutable.ArrayBuffer

import com.rinthyAi.perilus.main._

class PerilusTests extends AnyFunSpec with ChiselSim {
  describe("Perilus") {
    it("executes lw") {
      simulate(
        new Perilus(
          initRegs = "/home/bradley/Projects/perilus/assets/reg-lw-x32.hex",
          initMem = "/home/bradley/Projects/perilus/assets/mem-lw-64x32.hex",
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          // lw x21, 0x2a(x11)
          val rd = 21
          val rs1 = 11
          val base = 0xc
          val imm = 0x2a
          val memAddr = (base + imm).U
          val memData = 0x0f3f9400

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
          perilus.clock.step(6)

          // rd contains memData
          perilusDebug.reg.poke(rd)
          perilusDebug.regData.expect(memData)

          // contents of memAddr have not changed
          perilusDebug.memAddr.poke(memAddr)
          perilusDebug.memData.expect(memData)
        }
      }
    }
  }
}
