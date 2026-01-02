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
          initRegs = System.getProperty("user.dir") + "/assets/reg-lw-x32.hex",
          initMem = System.getProperty("user.dir") + "/assets/mem-lw-64x32.hex",
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
    it("executes sw") {
      simulate(
        new Perilus(
          initRegs = System.getProperty("user.dir") + "/assets/reg-sw-x32.hex",
          initMem = System.getProperty("user.dir") + "/assets/mem-sw-64x32.hex",
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          // sw x6, -4(x9)
          // sw rs2, imm(rs1)
          val rs2 = 6
          val rs1 = 9
          val base = 0x3a
          val imm = -4
          val memAddr = (base + imm).U
          val memData = 0x01830169

          // rs1 contains the base pointer
          perilusDebug.reg.poke(rs1)
          perilusDebug.regData.expect(base)

          // rs2 contains data to be stored
          perilusDebug.reg.poke(rs2)
          perilusDebug.regData.expect(memData)

          // execute the instruction
          perilus.clock.step(5)

          // contents of memAddr have not changed
          perilusDebug.memAddr.poke(memAddr)
          perilusDebug.memData.expect(memData)
        }
      }
    }
  }
}
