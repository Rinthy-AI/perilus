ci: fmt clean test doc

test: test-chisel test-official

test-chisel:
    sbt test

test-official:
    cd riscv-arch-test && \
        CONFIG_FILES=../tests/perilus/test_config.yaml \
        WORKDIR=../tests \
        make --jobs $(nproc)
    # TODO run the tests in the simulator

doc:
    typst compile -f svg doc/perilus.typ

fmt:
    sbt scalafmtAll
    cd sim && cargo fmt

chisel:
    sbt run

verilate:
    verilator --build --public-flat-rw --Mdir verilated --cc \
        generated/Perilus.sv \
        generated/Memory.sv \
        generated/RegisterFile.sv \
        generated/Alu.sv \
        generated/ControlUnit.sv \
        generated/ExtendUnit.sv \
        generated/AluDecoder.sv \
        generated/InstructionDecoder.sv \
        generated/registerFile_32x32.sv \
        generated/memory_1024x32.sv

simulate:
    cd sim && cargo run

clean:
    sbt clean
    rm -rf build generated verilated
    cd sim && cargo clean
    rm -rf tests/perilus-rv32i tests/stamps
    cd riscv-arch-test && make clean
