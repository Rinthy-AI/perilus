# `perilus`

A [RISC-V RV32I] processor written with [Chisel] as a learning exercise. Basically a copy of the
multi-cycle processor from [Harris and Harris].

[RISC-V RV32I]: https://docs.riscv.org/reference/isa/unpriv/rv32.html
[Chisel]: https://www.chisel-lang.org/docs
[Harris and Harris]: https://www.bookfinder.com/search/?ac=sl&st=sl&ref=bf_s2_a1_t1_1&qi=zIR9Gre1QrCSsNJWIGHNSdC.6mo_1761953030_1:42:250&bq=author%3Dsarah%2520harris%253B%2520david%2520harris%26title%3Ddigital%2520design%2520and%2520computer%2520architecture%252C%2520risc%2Dv%2520edition%2520risc%2Dv

## Usage

> [!NOTE]
> The processor doesn't work yet, but some of the parts do.

1. Install dependencies[^nix]
    - [Scala 2] (**not 3** because Chisel isn't compatible with it)
    - [`sbt`][sbt]
    - [Verilator]
1. `sbt test`

[Scala 2]: https://www.scala-lang.org/download/all.html
[sbt]: https://www.scala-sbt.org/
[Verilator]: https://verilator.org/guide/latest/overview.html

[^nix]: If you use the `nix` package manager, you can use the `shell.nix` file in the project root
    with `nix-shell`.
