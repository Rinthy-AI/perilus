use std::{error::Error, process::Command};

fn main() -> Result<(), Box<dyn Error>> {
    let verilator_command_output = Command::new("verilator")
        .arg("--getenv")
        .arg("VERILATOR_ROOT")
        .output()?;
    if !verilator_command_output.status.success() {
        return Err("Failed to get VERILATOR_ROOT. Is Verilator installed?".into());
    }
    let verilator_root = match String::from_utf8(verilator_command_output.stdout) {
        Ok(s) => s.trim().to_string(),
        Err(e) => {
            return Err(format!("Failed to parse Verilator command output as UTF-8: {e}").into());
        }
    };

    cc::Build::new()
        .cpp(true)
        .include(format!("{verilator_root}/include"))
        .include(format!("{verilator_root}/include/vltstd"))
        .include("../verilated")
        .warnings(false)
        .file("src/wrapper.cpp")
        .compile("wrapper");

    println!("cargo:rustc-link-search=native=../verilated");
    println!("cargo:rustc-link-lib=static=VPerilus");
    println!("cargo:rustc-link-lib=static=verilated");
    println!("cargo:rustc-link-lib=dylib=stdc++");

    println!("cargo::rerun-if-changed=./src/wrapper.cpp");
    println!("cargo::rerun-if-changed=../verilated/libverilated.a");
    println!("cargo::rerun-if-changed=../verilated/libVPerilus.a");

    Ok(())
}
