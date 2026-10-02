use std::{error::Error, path::PathBuf};

use clap::{Parser, Subcommand};
use perilus_core::perilus::Perilus;

#[derive(Parser)]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Run a raw binary program to completion and report pass/fail
    Test { file: PathBuf },
}

const EBREAK_INSTRUCTION: u32 = 0x00100073;
const TEST_MAX_CYCLES: u32 = 1_000_000;
const TEST_PASS_VALUE: u32 = 0xebbb3689;
const TEST_FAIL_VALUE: u32 = 0xd02a545a;

fn main() -> Result<(), Box<dyn Error>> {
    let cli = Cli::parse();
    let perilus = Perilus::init();

    match cli.command {
        Command::Test { file } => {
            perilus.load_file(file)?;
            let mut cycles = 0;
            loop {
                perilus.pulse_clock();
                cycles += 1;
                if cycles > TEST_MAX_CYCLES {
                    return Err("Test execution hit maximum cycle count limit".into());
                }
                if perilus.get_control_unit_state() == 0 {
                    let pc = perilus.get_pc();
                    let instr = perilus.get_memory().get((pc / 4) as usize);
                    if instr.is_none() {
                        return Err(format!(
                            "Program counter diverged to an address outside memory: 0x{pc:08x}"
                        )
                        .into());
                    }
                    if let Some(&EBREAK_INSTRUCTION) = instr {
                        let result = perilus.get_registers()[1];
                        match result {
                            TEST_PASS_VALUE => return Ok(()),
                            TEST_FAIL_VALUE => return Err("Test failed".into()),
                            v => return Err(format!("Got unexpected test result 0x{v:08x}").into()),
                        }
                    }
                }
            }
        }
    }
}
