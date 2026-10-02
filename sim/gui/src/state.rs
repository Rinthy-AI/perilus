#[derive(Clone, Copy, PartialEq, Eq)]
pub(crate) enum RunState {
    Idle,
    FreeRun,
    RunToPc(u32),
}

impl std::fmt::Display for RunState {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            RunState::Idle => write!(f, "Idle"),
            RunState::FreeRun => write!(f, "Running"),
            RunState::RunToPc(pc) => write!(f, "Run to 0x{pc:08x}"),
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
pub(crate) enum ViewMode {
    Word,
    Byte,
}

/// Which pane receives keyboard editing.
#[derive(Clone, Copy, PartialEq, Eq)]
pub(crate) enum Focus {
    Registers,
    Memory,
}

pub(crate) fn register_alias(index: usize) -> &'static str {
    match index {
        0 => "zero",
        1 => "ra",
        2 => "sp",
        3 => "gp",
        4 => "tp",
        5 => "t0",
        6 => "t1",
        7 => "t2",
        8 => "s0/fp",
        9 => "s1",
        10 => "a0",
        11 => "a1",
        12 => "a2",
        13 => "a3",
        14 => "a4",
        15 => "a5",
        16 => "a6",
        17 => "a7",
        18 => "s2",
        19 => "s3",
        20 => "s4",
        21 => "s5",
        22 => "s6",
        23 => "s7",
        24 => "s8",
        25 => "s9",
        26 => "s10",
        27 => "s11",
        28 => "t3",
        29 => "t4",
        30 => "t5",
        31 => "t6",
        _ => "??",
    }
}

pub(crate) fn printable(byte: u8) -> char {
    if (b' '..=b'~').contains(&byte) {
        byte as char
    } else {
        '.'
    }
}

pub(crate) fn hex_char(nibble: u8) -> char {
    std::char::from_digit(nibble as u32, 16).unwrap_or('?')
}

/// Screen order of the 64 hex digits in one memory row, as
/// `(byte offset within the row, nibble index)` where nibble 0 is the high
/// nibble of the byte and 1 is the low nibble.
///
/// Word view prints each 32-bit word big-endian, so the bytes within a word
/// appear in descending address order. Byte view prints bytes in ascending
/// memory order.
pub(crate) fn row_digits(view: ViewMode) -> [(u32, u8); 64] {
    let mut out = [(0u32, 0u8); 64];
    let mut i = 0;
    match view {
        ViewMode::Word => {
            for word in 0..8u32 {
                for byte in (0..4u32).rev() {
                    out[i] = (word * 4 + byte, 0);
                    i += 1;
                    out[i] = (word * 4 + byte, 1);
                    i += 1;
                }
            }
        }
        ViewMode::Byte => {
            for byte in 0..32u32 {
                out[i] = (byte, 0);
                i += 1;
                out[i] = (byte, 1);
                i += 1;
            }
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn aliases_cover_register_file() {
        assert_eq!(register_alias(0), "zero");
        assert_eq!(register_alias(8), "s0/fp");
        assert_eq!(register_alias(17), "a7");
        assert_eq!(register_alias(31), "t6");
    }

    #[test]
    fn printable_only_decodes_ascii() {
        assert_eq!(printable(b'A'), 'A');
        assert_eq!(printable(b' '), ' ');
        assert_eq!(printable(0), '.');
        assert_eq!(printable(0x7f), '.');
    }

    #[test]
    fn row_digit_order() {
        let word = row_digits(ViewMode::Word);
        assert_eq!(word[0], (3, 0));
        assert_eq!(word[7], (0, 1));
        let byte = row_digits(ViewMode::Byte);
        assert_eq!(byte[0], (0, 0));
        assert_eq!(byte[63], (31, 1));
    }
}
