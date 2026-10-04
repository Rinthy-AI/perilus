use bounded_integer::BoundedU32;
use std::{ffi::CString, io, path::PathBuf, slice};

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
    unsafe fn start_vcd(filename: *const std::ffi::c_char);
    unsafe fn stop_vcd();
}

pub type Register = BoundedU32<0, 31>;
pub type MemoryIndex = BoundedU32<0, { Perilus::MEMORY_SIZE_WORDS as u32 }>;

pub struct Perilus;

impl Perilus {
    pub const NUM_REGISTERS: usize = 32;
    pub const MEMORY_SIZE_WORDS: usize = 1_048_576;
    pub const MEMORY_SIZE_BYTES: usize = Self::MEMORY_SIZE_WORDS * 4;

    pub fn init() -> Perilus {
        unsafe { perilus_init() };
        Perilus
    }
    pub fn pulse_clock(&self) {
        unsafe {
            perilus_set_clock(0);
            perilus_eval();
            perilus_increment_time();
            perilus_set_clock(1);
            perilus_eval();
            perilus_increment_time();
        }
    }
    pub fn reset(&self) {
        unsafe {
            perilus_set_reset(1);
            self.pulse_clock();
            perilus_set_reset(0);
        }
    }
    pub fn get_pc(&self) -> u32 {
        unsafe { perilus_get_pc() }
    }
    pub fn set_pc(&self, new_pc: u32) {
        unsafe { perilus_set_pc(new_pc) };
    }
    pub fn get_control_unit_state(&self) -> u32 {
        unsafe { perilus_get_control_unit_state() }
    }
    pub fn get_registers(&self) -> &'static [u32] {
        unsafe { slice::from_raw_parts(perilus_get_register_file(), Perilus::NUM_REGISTERS) }
    }
    pub fn set_register(&self, register: Register, value: u32) {
        unsafe { perilus_set_register(register.get(), value) };
    }
    pub fn get_memory(&self) -> &'static [u32] {
        unsafe { slice::from_raw_parts(perilus_get_memory(), Perilus::MEMORY_SIZE_WORDS) }
    }
    pub fn set_memory(&self, address: u32, value: u32) {
        unsafe { perilus_set_memory(address, value) };
    }
    pub fn get_memory_byte(&self, address_bytes: u32) -> u8 {
        let word = self.get_memory()[(address_bytes / 4) as usize];
        (word >> ((address_bytes % 4) * 8)) as u8
    }
    pub fn set_memory_byte(&self, address_bytes: u32, value: u8) {
        let address_words = address_bytes / 4;
        let shift = (address_bytes % 4) * 8;
        let current = self.get_memory()[address_words as usize];
        let mask = !(0xffu32 << shift);
        self.set_memory(address_words, (current & mask) | ((value as u32) << shift));
    }
    pub fn memory_bytes(&self) -> Vec<u8> {
        self.get_memory()
            .iter()
            .flat_map(|word| word.to_le_bytes())
            .collect()
    }
    pub fn load_bytes(&self, bytes: &[u8]) -> io::Result<()> {
        if bytes.len() > Self::MEMORY_SIZE_BYTES {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "Provided file is too large to fit in simulated memory",
            ));
        }
        for (address_bytes, value) in bytes.iter().enumerate() {
            self.set_memory_byte(address_bytes as u32, *value);
        }
        Ok(())
    }
    pub fn load_file(&self, file: PathBuf) -> io::Result<()> {
        self.load_bytes(&std::fs::read(file)?)
    }
    pub fn save_file(&self, file: PathBuf) -> io::Result<()> {
        std::fs::write(file, self.memory_bytes())
    }
    pub fn start_trace(&self, file: PathBuf) -> io::Result<()> {
        // Ensure the parent directory exists (VCD file will be created by Verilator)
        if let Some(parent) = file.parent() {
            std::fs::create_dir_all(parent)?;
        }
        let c_string = CString::new(file.to_string_lossy().as_bytes())
            .map_err(|e| io::Error::new(io::ErrorKind::InvalidInput, e))?;
        unsafe {
            start_vcd(c_string.as_ptr());
        }
        Ok(())
    }
    pub fn stop_trace(&self) {
        unsafe {
            stop_vcd();
        }
    }
}

impl Drop for Perilus {
    fn drop(&mut self) {
        unsafe { perilus_drop() };
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn memory_roundtrip() {
        let perilus = Perilus::init();
        let data = [0xde, 0xad, 0xbe, 0xef, 0x01, 0x02, 0x03];
        perilus.load_bytes(&data).unwrap();
        assert_eq!(&perilus.memory_bytes()[..data.len()], &data);

        perilus.set_memory_byte(0, 0xaa);
        assert_eq!(perilus.get_memory_byte(0), 0xaa);

        let oversized = vec![0u8; Perilus::MEMORY_SIZE_BYTES + 1];
        assert!(perilus.load_bytes(&oversized).is_err());
    }
}
