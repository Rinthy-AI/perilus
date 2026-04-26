#include <VPerilus.h>
#include <VPerilus___024root.h>
#include <verilated.h>

static VerilatedContext* ctx = nullptr;
static VPerilus* perilus = nullptr;

extern "C" {
    void perilus_init() {
        ctx = new VerilatedContext;
        ctx->debug(0);
        ctx->threads(1);
        ctx->randReset(0);
        ctx->randSeed(1);
        ctx->traceEverOn(false);
        perilus = new VPerilus(ctx);
    }
    void perilus_eval() {
        perilus->eval();
    }
    void perilus_drop() {
        perilus->final();
        delete perilus;
        delete ctx;
        perilus = nullptr;
        ctx = nullptr;
    }
    void perilus_increment_time() {
        ctx->timeInc(1);
    }

    // clock
    void perilus_set_clock(uint32_t new_clock) {
        perilus->clock = new_clock;
    }

    // pc
    uint32_t perilus_get_pc() {
        return perilus->rootp->Perilus__DOT__pc;
    }
    void perilus_set_pc(uint32_t new_pc) {
        perilus->rootp->Perilus__DOT__pc = new_pc;
    }

    // reset
    void perilus_set_reset(uint32_t new_reset) {
        perilus->reset = new_reset;
    }

    // control unit state
    uint32_t perilus_get_control_unit_state() {
        return perilus->rootp->Perilus__DOT__controlUnit__DOT__state;
    }
    void perilus_set_control_unit_state(uint32_t new_control_unit_state) {
        perilus->rootp->Perilus__DOT__controlUnit__DOT__state = new_control_unit_state;
    }

    // register file
    const uint32_t* perilus_get_register_file() {
        return &perilus->rootp->Perilus__DOT__io_registerFile__DOT__registerFile_ext__DOT__Memory[0];
    }
    void perilus_set_register(uint32_t reg, uint32_t value) {
        perilus->rootp->Perilus__DOT__io_registerFile__DOT__registerFile_ext__DOT__Memory[reg] = value;
    }

    // memory
    const uint32_t* perilus_get_memory() {
        return &perilus->rootp->Perilus__DOT__io_memory__DOT__memory_ext__DOT__Memory[0];
    }
    void perilus_set_memory(uint32_t address, uint32_t value) {
        perilus->rootp->Perilus__DOT__io_memory__DOT__memory_ext__DOT__Memory[address] = value;
    }
}
