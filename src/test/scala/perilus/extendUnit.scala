import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.funspec.AnyFunSpec

import com.rinthyAi.perilus.extendUnit._

class ExtendUnitTests extends AnyFunSpec with ChiselSim {
  describe("ExtendUnit") {
    it("correctly extends I-type immediates") {
      simulate(new ExtendUnit) { extendUnit =>
        {
          extendUnit.io.immSrc.poke("b00".U)

          extendUnit.io.input.poke("hdc4691".U) // immediate value is 0xe9b
          extendUnit.io.immExt.expect("h6e2".U)

          extendUnit.io.input.poke("h1ff8946".U) // immediate value is 0xffc
          extendUnit.io.immExt.expect("hfffffffc".U)
        }
      }
    }
    it("correctly extends S-type immediates") {
      simulate(new ExtendUnit) { extendUnit =>
        {
          extendUnit.io.immSrc.poke("b01".U)

          extendUnit.io.input.poke("h65b22a".U) // immediate value is 0x32a
          extendUnit.io.immExt.expect("h32a".U)

          extendUnit.io.input.poke("h1de532a".U) // immediate value is 0xeea
          extendUnit.io.immExt.expect("hfffffeea".U)
        }
      }
    }
    it("correctly extends B-type immediates") {
      simulate(new ExtendUnit) { extendUnit =>
        {
          extendUnit.io.immSrc.poke("b10".U)

          extendUnit.io.input.poke("hcbee9".U) // immediate value is 0x868
          extendUnit.io.immExt.expect("h868".U)

          extendUnit.io.input.poke("h144ae93".U) // immediate value is 0x1a32
          extendUnit.io.immExt.expect("hfffffa32".U)
        }
      }
    }
    it("outputs zero when immSrc is invalid") {
      simulate(new ExtendUnit) { extendUnit =>
        {
          extendUnit.io.immSrc.poke("b11".U)

          extendUnit.io.input.poke("hb2a945".U)
          extendUnit.io.immExt.expect(0.U)

          extendUnit.io.input.poke("h1f800d2".U)
          extendUnit.io.immExt.expect(0.U)
        }
      }
    }
  }
}
