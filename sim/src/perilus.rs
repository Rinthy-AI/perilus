use bounded_integer::BoundedU32;
use std::{io, path::PathBuf, slice};

unsafe extern "C" {
    fn perilus_init();
    fn perilus_eval();
    fn perilus_drop();
    fn perilus_increment_time();
    fn perilus_set_clock(new_clock: u32);
    fn perilus_get_pc() -> u32;
    fn perilus_set_pc(new_pc: u32);
    fn perilus_set_reset(new_reset: u32);
    fn perilus_get_control_unit_state() -> u32;
    fn perilus_get_register_file() -> *const u32;
    fn perilus_set_register(reg: u32, value: u32);
    fn perilus_get_memory() -> *const u32;
    fn perilus_set_memory(address: u32, value: u32);
}

pub(crate) type Register = BoundedU32<0, 31>;
pub(crate) type MemoryIndex = BoundedU32<0, { Perilus::MEMORY_SIZE_WORDS as u32 }>;

pub(crate) struct Perilus;

impl Perilus {
    const NUM_REGISTERS: usize = 32;
    const MEMORY_SIZE_WORDS: usize = 1_048_576;

    pub(crate) fn init() -> Perilus {
        unsafe { perilus_init() };
        Perilus
    }
    pub(crate) fn pulse_clock(&self) {
        unsafe {
            perilus_set_clock(0);
            perilus_eval();
            perilus_increment_time();
            perilus_set_clock(1);
            perilus_eval();
            perilus_increment_time();
        }
    }
    pub(crate) fn reset(&self) {
        unsafe {
            perilus_set_reset(1);
            self.pulse_clock();
            perilus_set_reset(0);
        }
    }
    pub(crate) fn get_pc(&self) -> u32 {
        unsafe { perilus_get_pc() }
    }
    pub(crate) fn set_pc(&self, new_pc: u32) {
        unsafe { perilus_set_pc(new_pc) };
    }
    pub(crate) fn get_control_unit_state(&self) -> u32 {
        unsafe { perilus_get_control_unit_state() }
    }
    pub(crate) fn get_registers(&self) -> &'static [u32] {
        unsafe { slice::from_raw_parts(perilus_get_register_file(), Perilus::NUM_REGISTERS) }
    }
    pub(crate) fn set_register(&self, register: Register, value: u32) {
        unsafe { perilus_set_register(register.get(), value) };
    }
    pub(crate) fn get_memory(&self) -> &'static [u32] {
        unsafe { slice::from_raw_parts(perilus_get_memory(), Perilus::MEMORY_SIZE_WORDS) }
    }
    pub(crate) fn set_memory(&self, address: u32, value: u32) {
        unsafe { perilus_set_memory(address, value) };
    }
    pub(crate) fn load_file(&self, file: PathBuf) -> io::Result<()> {
        let bytes = std::fs::read(file)?;
        if bytes.len() > Self::MEMORY_SIZE_WORDS * 4 {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "Provided file is too large to fit in simulated memory",
            ));
        }
        for (address_bytes, value) in bytes.iter().enumerate() {
            let address_words = address_bytes / 4;
            let shift = (address_bytes % 4) * 8;
            let current = self.get_memory()[address_words];
            let mask = !(0xffu32 << shift);
            let new = (current & mask) | ((*value as u32) << shift);
            self.set_memory(address_words as u32, new);
        }
        Ok(())
    }
}

impl Drop for Perilus {
    fn drop(&mut self) {
        unsafe { perilus_drop() };
    }
}
