ci: fmt clean test-chisel doc

test: test-chisel test-act

test-chisel:
    sbt test

test-act: build-sim
    cd riscv-arch-test && \
        CONFIG_FILES=../tests/perilus/test_config.yaml \
        WORKDIR=../tests \
        make --jobs $(nproc)
    cd tests && ./run.sh

doc:
    typst compile -f svg doc/perilus.typ

fmt:
    sbt scalafmtAll
    cd sim && cargo fmt && cargo clippy

[script]
[arg("force", long="force", value="1")]
chisel force='':
    if [ -n "{{force}}" ] || [ $(find src/main/ -name "**.scala" -newer "generated/.timestamp" 2>&1 | wc -l) != 0 ]; then
        sbt run
        touch generated/.timestamp
    else
        echo "Chisel generated output appears to be up to date, skipping"
    fi

[script]
[arg("force", long="force", value="1")]
verilate force='':
    if [ -n "{{force}}" ] || [ $(find generated/ -name "**.sv" -newer "verilated/.timestamp" 2>&1 | wc -l) != 0 ]; then
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
            generated/memory_1048576x32.sv
        touch verilated/.timestamp
    else
        echo "Verilated output appears to be up to date, skipping"
    fi

build-sim: chisel verilate
    cd sim && cargo build --release

simulate: chisel verilate
    cd sim && cargo run --release

clean:
    sbt clean
    rm -rf build generated verilated
    cd sim && cargo clean
    rm -rf tests/perilus-rv32i tests/stamps
    cd riscv-arch-test && make clean
