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

             // lw x21, 0xa8(x11)
             val rd = 21
             val rs1 = 11
             val base = 0x30
             val imm = 0xa8
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
             val base = 0x38
             val imm = -4
             val memAddr = (base + imm).U
             val memData = 0x01830169

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
      simulate(
        new Perilus(
          initRegs = System.getProperty("user.dir") + "/assets/reg-beq-x32.hex",
          initMem = System.getProperty("user.dir") + "/assets/mem-beq-64x32.hex",
          withDebug = true
        )
      ) { perilus =>
        {
          val perilusDebug = perilus.io.debug.get

          val rs1 = 3
          val rs2 = 6
          val rs3 = 9
          val offset = 16.U

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
