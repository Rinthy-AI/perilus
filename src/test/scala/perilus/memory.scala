import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

import com.rinthyAi.perilus.memory._

class MemoryTests extends AnyFunSpec with ChiselSim {
  describe("Memory") {
    it("stores and retrieves data") {
      simulate(new Memory(8, 32.W, System.getProperty("user.dir") + "/assets/mem-zeros-8x32.hex")) {
        memory =>
          {
            val addr1 = "h12e".U
            val addr2 = "h0d7".U
            val data1 = "he7d857de".U
            val data2 = "h38cc122f".U

            memory.io.address.poke(addr1)
            memory.io.writeData.poke(data1)
            memory.io.writeEnable.poke(true.B)
            memory.io.readData.expect(0.U)
            memory.clock.step(1)
            memory.io.readData.expect(data1)

            memory.io.address.poke(addr2)
            memory.io.writeData.poke(data2)
            memory.io.writeEnable.poke(true.B)
            memory.io.readData.expect(0.U)
            memory.clock.step(1)
            memory.io.readData.expect(data2)

            memory.io.writeData.poke(0.U)
            memory.io.writeEnable.poke(false.B)
            memory.clock.step(1)
            memory.io.address.poke(addr1)
            memory.io.readData.expect(data1)
            memory.io.address.poke(addr2)
            memory.io.readData.expect(data2)
            memory.io.address.poke(0.U)
            memory.io.readData.expect(0.U)
          }
      }
    }
  }
}
