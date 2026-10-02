use std::ops::Range;
use std::time::{Duration, Instant};

use eframe::egui;
use egui::{Color32, FontId, Key, Modifiers};
use perilus_core::perilus::{Perilus, Register};

use crate::state::{Focus, RunState, ViewMode, row_digits};

pub(crate) const FONT_SIZE: f32 = 16.0;
pub(crate) const ROW_H: f32 = 22.0;
pub(crate) const GROUP_GAP: f32 = 10.0;
pub(crate) const ADDR_W: f32 = 76.0;
pub(crate) const DIVIDER_GAP: f32 = 9.0;
pub(crate) const REG_LABEL_W: f32 = 58.0;
pub(crate) const REG_COL_GAP: f32 = 20.0;
pub(crate) const BYTES_PER_ROW: usize = 32;
pub(crate) const WORDS_PER_ROW: usize = 8;
pub(crate) const REG_COLS: usize = 4;
pub(crate) const REG_ROWS: usize = 8;
pub(crate) const NUM_CELLS: usize = REG_COLS * REG_ROWS;
pub(crate) const PC_COLOR: Color32 = Color32::from_rgb(40, 200, 40);

const MAX_RUN_CYCLES: u64 = 50_000_000;
const FREE_RUN_BUDGET: Duration = Duration::from_millis(8);
const CTRL_SHIFT: Modifiers = Modifiers {
    ctrl: true,
    shift: true,
    alt: false,
    mac_cmd: false,
    command: false,
};

// Hex keys for the nibble editor.
const HEX_KEYS: [(Key, u8); 16] = [
    (Key::Num0, 0),
    (Key::Num1, 1),
    (Key::Num2, 2),
    (Key::Num3, 3),
    (Key::Num4, 4),
    (Key::Num5, 5),
    (Key::Num6, 6),
    (Key::Num7, 7),
    (Key::Num8, 8),
    (Key::Num9, 9),
    (Key::A, 10),
    (Key::B, 11),
    (Key::C, 12),
    (Key::D, 13),
    (Key::E, 14),
    (Key::F, 15),
];

#[derive(Clone, Copy)]
pub(crate) enum Direction {
    Left,
    Right,
    Up,
    Down,
}

#[derive(Default)]
struct Actions {
    step: bool,
    next_instruction: bool,
    reset: bool,
    free_run: bool,
    run_to_cursor: bool,
    stop: bool,
    focus_registers: bool,
    focus_memory: bool,
    focus_address: bool,
    follow: bool,
    names: bool,
    view: bool,
    set_pc_cursor: bool,
    clear: bool,
    group_left: bool,
    group_right: bool,
    page_up: bool,
    page_down: bool,
    left: bool,
    right: bool,
    up: bool,
    down: bool,
    backspace: bool,
    nibble: Option<u8>,
}

pub(crate) struct SimApp {
    pub(crate) perilus: Perilus,
    pub(crate) current_pc: u32,
    pub(crate) run_state: RunState,
    pub(crate) view: ViewMode,
    pub(crate) show_aliases: bool,
    pub(crate) focus: Focus,
    /// Memory cursor: byte address and nibble (0 = high, 1 = low).
    pub(crate) mem_addr: u32,
    pub(crate) mem_nib: u8,
    /// Register-grid cursor: cell index (0 = PC, 1..=31 = x1..x31) and nibble.
    pub(crate) reg_index: u8,
    pub(crate) reg_nib: u8,
    pub(crate) address_input: String,
    pub(crate) follow_pc: bool,
    pub(crate) follow_pc_last: u32,
    pub(crate) focus_address: bool,
    pub(crate) scroll_to_row: Option<usize>,
    pub(crate) mem_viewport: Range<usize>,
    pub(crate) status: String,
    run_cycles: u64,
}

impl SimApp {
    pub(crate) fn new() -> SimApp {
        let perilus = Perilus::init();
        let current_pc = perilus.get_pc();
        SimApp {
            perilus,
            current_pc,
            run_state: RunState::Idle,
            view: ViewMode::Word,
            show_aliases: true,
            focus: Focus::Registers,
            mem_addr: 3,
            mem_nib: 0,
            reg_index: 0,
            reg_nib: 0,
            address_input: String::new(),
            follow_pc: false,
            follow_pc_last: current_pc,
            focus_address: false,
            scroll_to_row: None,
            mem_viewport: 0..0,
            status: "Ready".into(),
            run_cycles: 0,
        }
    }

    /// Captures the model PC once the control unit is back at an instruction boundary.
    pub(crate) fn sync_pc(&mut self) {
        if self.perilus.get_control_unit_state() == 0 {
            self.current_pc = self.perilus.get_pc();
        }
    }

    pub(crate) fn reset(&mut self) {
        self.perilus.reset();
        self.run_state = RunState::Idle;
        self.sync_pc();
        self.status = "Reset".into();
    }

    /// Pulse the clock exactly once.
    pub(crate) fn step_cycle(&mut self) {
        self.perilus.pulse_clock();
        self.sync_pc();
        self.status = "Stepped one cycle".into();
    }

    /// Run until the control unit returns to an instruction boundary.
    pub(crate) fn next_instruction(&mut self) {
        const MAX_STATES_PER_INSTRUCTION: u32 = 64;
        self.perilus.pulse_clock();
        let mut states = 1;
        while self.perilus.get_control_unit_state() != 0 && states < MAX_STATES_PER_INSTRUCTION {
            self.perilus.pulse_clock();
            states += 1;
        }
        self.sync_pc();
        if self.perilus.get_control_unit_state() == 0 {
            self.status = "Stepped one instruction".into();
        } else {
            self.status = "Stopped: control unit did not return to fetch".into();
        }
    }

    pub(crate) fn run_to_pc(&mut self, target: u32) {
        self.run_cycles = 0;
        self.run_state = RunState::RunToPc(target);
    }

    pub(crate) fn run_to_input_pc(&mut self) {
        match parse_hex(&self.address_input) {
            Some(target) => self.run_to_pc(target),
            None => self.status = format!("Invalid PC target {:?}", self.address_input),
        }
    }

    pub(crate) fn toggle_free_run(&mut self) {
        self.run_state = match self.run_state {
            RunState::FreeRun => RunState::Idle,
            _ => RunState::FreeRun,
        };
    }

    pub(crate) fn toggle_follow(&mut self) {
        self.follow_pc = !self.follow_pc;
        self.follow_pc_last = self.current_pc;
        self.status = format!("Follow PC {}", if self.follow_pc { "on" } else { "off" });
    }

    pub(crate) fn stop(&mut self) {
        if self.run_state != RunState::Idle {
            self.status = "Stopped".into();
        }
        self.run_state = RunState::Idle;
    }

    pub(crate) fn save_memory(&mut self) {
        if let Some(path) = rfd::FileDialog::new()
            .set_file_name("memory.bin")
            .save_file()
        {
            self.status = match self.perilus.save_file(path.clone()) {
                Ok(()) => format!("Saved memory to {}", path.display()),
                Err(e) => format!("Save failed: {e}"),
            };
        }
    }

    pub(crate) fn load_memory(&mut self) {
        if let Some(path) = rfd::FileDialog::new().pick_file() {
            self.load_path(path);
        }
    }

    pub(crate) fn load_path(&mut self, path: std::path::PathBuf) {
        self.status = match self.perilus.load_file(path.clone()) {
            Ok(()) => format!("Loaded memory from {}", path.display()),
            Err(e) => format!("Load failed: {e}"),
        };
        self.sync_pc();
    }

    pub(crate) fn jump_to_address(&mut self) {
        match parse_hex(&self.address_input) {
            Some(addr) => {
                let addr = addr & (Perilus::MEMORY_SIZE_BYTES as u32 - 1);
                self.mem_addr = addr;
                self.mem_nib = 0;
                self.scroll_to_row = Some(addr as usize / BYTES_PER_ROW);
                self.focus = Focus::Memory;
                self.status = format!("Selection moved to 0x{addr:08x}");
            }
            None => self.status = format!("Invalid address {:?}", self.address_input),
        }
    }

    /// Word containing the memory cursor.
    pub(crate) fn cursor_word(&self) -> u32 {
        self.mem_addr & !3
    }

    pub(crate) fn set_pc_to_cursor(&mut self) {
        let pc = self.cursor_word();
        self.perilus.set_pc(pc);
        self.current_pc = pc;
        self.status = format!("Set PC to 0x{pc:08x}");
    }

    pub(crate) fn run_to_cursor(&mut self) {
        self.run_to_pc(self.cursor_word());
    }

    fn move_cursor(&mut self, direction: Direction) {
        match self.focus {
            Focus::Memory => self.move_memory(direction),
            Focus::Registers => self.move_register(direction),
        }
    }

    fn move_memory(&mut self, direction: Direction) {
        let order = row_digits(self.view);
        let row = (self.mem_addr / BYTES_PER_ROW as u32) as usize;
        let offset = self.mem_addr % BYTES_PER_ROW as u32;
        let col = order
            .iter()
            .position(|&(o, n)| o == offset && n == self.mem_nib)
            .unwrap_or(0);
        let total_rows = Perilus::MEMORY_SIZE_WORDS / WORDS_PER_ROW;
        let (row, col) = match direction {
            Direction::Left if col == 0 && row > 0 => (row - 1, 63),
            Direction::Left if col > 0 => (row, col - 1),
            Direction::Right if col == 63 && row + 1 < total_rows => (row + 1, 0),
            Direction::Right if col < 63 => (row, col + 1),
            Direction::Up if row > 0 => (row - 1, col),
            Direction::Down if row + 1 < total_rows => (row + 1, col),
            _ => (row, col),
        };
        self.mem_addr = row as u32 * BYTES_PER_ROW as u32 + order[col].0;
        self.mem_nib = order[col].1;
    }

    /// Move to the first digit of the previous/next word (word view) or byte
    /// (byte view).
    fn move_memory_group(&mut self, forward: bool) {
        let order = row_digits(self.view);
        let row = (self.mem_addr / BYTES_PER_ROW as u32) as usize;
        let offset = self.mem_addr % BYTES_PER_ROW as u32;
        let col = order
            .iter()
            .position(|&(o, n)| o == offset && n == self.mem_nib)
            .unwrap_or(0);
        let group = match self.view {
            ViewMode::Word => 8,
            ViewMode::Byte => 2,
        };
        let groups_per_row = 64 / group;
        let index = col / group;
        let total_rows = Perilus::MEMORY_SIZE_WORDS / WORDS_PER_ROW;
        let (row, col) = if forward {
            if index + 1 < groups_per_row {
                (row, (index + 1) * group)
            } else if row + 1 < total_rows {
                (row + 1, 0)
            } else {
                (row, col)
            }
        } else if index > 0 {
            (row, (index - 1) * group)
        } else if row > 0 {
            (row - 1, (groups_per_row - 1) * group)
        } else {
            (row, col)
        };
        self.mem_addr = row as u32 * BYTES_PER_ROW as u32 + order[col].0;
        self.mem_nib = order[col].1;
    }

    fn move_register(&mut self, direction: Direction) {
        match direction {
            Direction::Left if self.reg_nib == 0 && self.reg_index > 0 => {
                self.reg_index -= 1;
                self.reg_nib = 7;
            }
            Direction::Left if self.reg_nib > 0 => self.reg_nib -= 1,
            Direction::Right if self.reg_nib == 7 && (self.reg_index as usize) < NUM_CELLS - 1 => {
                self.reg_index += 1;
                self.reg_nib = 0;
            }
            Direction::Right if self.reg_nib < 7 => self.reg_nib += 1,
            Direction::Up if self.reg_index > 0 => self.reg_index -= 1,
            Direction::Down if (self.reg_index as usize) < NUM_CELLS - 1 => self.reg_index += 1,
            _ => {}
        }
    }

    fn edit_nibble(&mut self, nibble: u8) {
        match self.focus {
            Focus::Memory => {
                let addr = self.mem_addr;
                let byte = self.perilus.get_memory_byte(addr);
                let byte = if self.mem_nib == 0 {
                    (byte & 0x0f) | (nibble << 4)
                } else {
                    (byte & 0xf0) | nibble
                };
                self.perilus.set_memory_byte(addr, byte);
                self.move_memory(Direction::Right);
            }
            Focus::Registers => {
                let shift = (7 - self.reg_nib as u32) * 4;
                if self.reg_index == 0 {
                    let value = (self.current_pc & !(0xf << shift)) | ((nibble as u32) << shift);
                    self.perilus.set_pc(value);
                    self.current_pc = value;
                } else {
                    let index = self.reg_index as usize;
                    let value = self.perilus.get_registers()[index];
                    let value = (value & !(0xf << shift)) | ((nibble as u32) << shift);
                    if let Some(reg) = Register::new(index as u32) {
                        self.perilus.set_register(reg, value);
                    }
                }
                self.move_register(Direction::Right);
            }
        }
    }

    fn backspace(&mut self) {
        match self.focus {
            Focus::Memory => {
                let order = row_digits(self.view);
                let offset = self.mem_addr % BYTES_PER_ROW as u32;
                let col = order
                    .iter()
                    .position(|&(o, n)| o == offset && n == self.mem_nib)
                    .unwrap_or(0);
                if self.mem_addr < BYTES_PER_ROW as u32 && col == 0 {
                    return;
                }
                let addr = self.mem_addr;
                let byte = self.perilus.get_memory_byte(addr);
                let byte = if self.mem_nib == 0 {
                    byte & 0x0f
                } else {
                    byte & 0xf0
                };
                self.perilus.set_memory_byte(addr, byte);
                self.move_memory(Direction::Left);
            }
            Focus::Registers => {
                if self.reg_index == 0 && self.reg_nib == 0 {
                    return;
                }
                let shift = (7 - self.reg_nib as u32) * 4;
                if self.reg_index == 0 {
                    let value = self.current_pc & !(0xf << shift);
                    self.perilus.set_pc(value);
                    self.current_pc = value;
                } else {
                    let index = self.reg_index as usize;
                    let value = self.perilus.get_registers()[index] & !(0xf << shift);
                    if let Some(reg) = Register::new(index as u32) {
                        self.perilus.set_register(reg, value);
                    }
                }
                self.move_register(Direction::Left);
            }
        }
    }

    fn clear_cursor(&mut self) {
        match self.focus {
            Focus::Memory => match self.view {
                ViewMode::Word => {
                    let word = self.mem_addr / 4;
                    self.perilus.set_memory(word, 0);
                    self.status = format!("Cleared word 0x{:08x}", word * 4);
                }
                ViewMode::Byte => {
                    self.perilus.set_memory_byte(self.mem_addr, 0);
                    self.status = format!("Cleared byte 0x{:08x}", self.mem_addr);
                }
            },
            Focus::Registers => {
                if self.reg_index == 0 {
                    self.perilus.set_pc(0);
                    self.current_pc = 0;
                    self.status = "Cleared PC".into();
                } else if let Some(reg) = Register::new(self.reg_index as u32) {
                    self.perilus.set_register(reg, 0);
                    self.status = format!("Cleared x{}", self.reg_index);
                }
            }
        }
    }

    fn scroll_page(&mut self, down: bool) {
        let total_rows = Perilus::MEMORY_SIZE_WORDS / WORDS_PER_ROW;
        let page = self.mem_viewport.len().max(1) as isize;
        let top = self.mem_viewport.start as isize;
        let new_top = if down {
            (top + page).min(total_rows as isize - 1)
        } else {
            (top - page).max(0)
        };
        self.scroll_to_row = Some(new_top as usize);
    }

    fn handle_keys(&mut self, ui: &egui::Ui) {
        let ctx = ui.ctx();
        if ctx.text_edit_focused() {
            return;
        }
        let actions = ctx.input_mut(|i| {
            let mut actions = Actions {
                step: i.consume_key(Modifiers::NONE, Key::Space),
                stop: i.consume_key(Modifiers::NONE, Key::Escape),
                backspace: i.consume_key(Modifiers::NONE, Key::Backspace),
                next_instruction: i.consume_key(Modifiers::NONE, Key::N),
                focus_address: i.consume_key(Modifiers::NONE, Key::G),
                names: i.consume_key(Modifiers::NONE, Key::X),
                view: i.consume_key(Modifiers::NONE, Key::V),
                clear: i.consume_key(Modifiers::SHIFT, Key::C),
                follow: i.consume_key(Modifiers::SHIFT, Key::P),
                set_pc_cursor: i.consume_key(Modifiers::NONE, Key::P),
                ..Default::default()
            };
            actions.run_to_cursor = i.consume_key(CTRL_SHIFT, Key::Space);
            actions.free_run = i.consume_key(Modifiers::CTRL, Key::Space);
            actions.reset = i.consume_key(Modifiers::CTRL, Key::R);
            actions.focus_registers = i.consume_key(Modifiers::NONE, Key::R);
            actions.focus_memory = i.consume_key(Modifiers::NONE, Key::M);
            actions.group_left = i.consume_key(Modifiers::CTRL, Key::ArrowLeft)
                || i.consume_key(Modifiers::CTRL, Key::H);
            actions.group_right = i.consume_key(Modifiers::CTRL, Key::ArrowRight)
                || i.consume_key(Modifiers::CTRL, Key::L);
            actions.page_up = i.consume_key(Modifiers::NONE, Key::PageUp)
                || i.consume_key(Modifiers::CTRL, Key::U);
            actions.page_down = i.consume_key(Modifiers::NONE, Key::PageDown)
                || i.consume_key(Modifiers::CTRL, Key::D);
            actions.left = i.consume_key(Modifiers::NONE, Key::ArrowLeft)
                || i.consume_key(Modifiers::NONE, Key::H);
            actions.right = i.consume_key(Modifiers::NONE, Key::ArrowRight)
                || i.consume_key(Modifiers::NONE, Key::L);
            actions.up = i.consume_key(Modifiers::NONE, Key::ArrowUp)
                || i.consume_key(Modifiers::NONE, Key::K);
            actions.down = i.consume_key(Modifiers::NONE, Key::ArrowDown)
                || i.consume_key(Modifiers::NONE, Key::J);
            for (key, value) in HEX_KEYS {
                if i.consume_key(Modifiers::NONE, key) {
                    actions.nibble = Some(value);
                }
            }
            actions
        });

        if actions.run_to_cursor {
            self.run_to_cursor();
        }
        if actions.free_run {
            self.toggle_free_run();
        }
        if actions.step {
            self.step_cycle();
        }
        if actions.next_instruction {
            self.next_instruction();
        }
        if actions.reset {
            self.reset();
        }
        if actions.focus_registers {
            self.focus = Focus::Registers;
        }
        if actions.focus_memory {
            self.focus = Focus::Memory;
        }
        if actions.set_pc_cursor {
            self.set_pc_to_cursor();
        }
        if actions.stop {
            self.stop();
        }
        if actions.focus_address {
            self.focus_address = true;
        }
        if actions.follow {
            self.toggle_follow();
        }
        if actions.names {
            self.show_aliases = !self.show_aliases;
        }
        if actions.view {
            self.view = match self.view {
                ViewMode::Word => ViewMode::Byte,
                ViewMode::Byte => ViewMode::Word,
            };
        }
        if actions.clear {
            self.clear_cursor();
        }
        if actions.group_left && self.focus == Focus::Memory {
            self.move_memory_group(false);
        }
        if actions.group_right && self.focus == Focus::Memory {
            self.move_memory_group(true);
        }
        if actions.page_up {
            self.scroll_page(false);
        }
        if actions.page_down {
            self.scroll_page(true);
        }
        let direction = if actions.left {
            Some(Direction::Left)
        } else if actions.right {
            Some(Direction::Right)
        } else if actions.up {
            Some(Direction::Up)
        } else if actions.down {
            Some(Direction::Down)
        } else {
            None
        };
        if let Some(direction) = direction {
            self.move_cursor(direction);
        }
        if let Some(nibble) = actions.nibble {
            self.edit_nibble(nibble);
        }
        if actions.backspace {
            self.backspace();
        }
    }
}

impl eframe::App for SimApp {
    fn logic(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        match self.run_state {
            RunState::Idle => {}
            RunState::FreeRun => {
                let start = Instant::now();
                while start.elapsed() < FREE_RUN_BUDGET {
                    self.perilus.pulse_clock();
                }
                self.sync_pc();
                ctx.request_repaint();
            }
            RunState::RunToPc(target) => {
                let start = Instant::now();
                loop {
                    if self.perilus.get_control_unit_state() == 0 && self.perilus.get_pc() == target
                    {
                        self.status = format!("Reached PC 0x{target:08x}");
                        self.run_state = RunState::Idle;
                        break;
                    }
                    self.perilus.pulse_clock();
                    self.run_cycles += 1;
                    if self.run_cycles >= MAX_RUN_CYCLES {
                        self.status = format!(
                            "Stopped: did not reach PC 0x{target:08x} after {MAX_RUN_CYCLES} cycles"
                        );
                        self.run_state = RunState::Idle;
                        break;
                    }
                    if start.elapsed() >= FREE_RUN_BUDGET {
                        ctx.request_repaint();
                        break;
                    }
                }
                self.sync_pc();
            }
        }
    }

    fn ui(&mut self, ui: &mut egui::Ui, _frame: &mut eframe::Frame) {
        self.handle_keys(ui);
        let top_frame = egui::Frame::side_top_panel(ui.style()).shadow(egui::Shadow::NONE);
        egui::Panel::top("registers_panel")
            .frame(top_frame)
            .show_separator_line(false)
            .show(ui, |ui| {
                ui.add_space(4.0);
                self.registers_ui(ui);
                ui.add_space(4.0);
            });
        let bottom_frame = egui::Frame::side_top_panel(ui.style()).shadow(egui::Shadow::NONE);
        egui::Panel::bottom("controls_panel")
            .frame(bottom_frame)
            .show_separator_line(false)
            .show(ui, |ui| {
                ui.add_space(4.0);
                self.controls_ui(ui);
                ui.add_space(4.0);
            });
        egui::CentralPanel::default().show(ui, |ui| {
            self.memory_ui(ui);
        });
    }
}

pub(crate) fn char_width(ui: &egui::Ui, font: &FontId) -> f32 {
    ui.painter()
        .layout_no_wrap("0".to_owned(), font.clone(), Color32::WHITE)
        .size()
        .x
}

fn parse_hex(text: &str) -> Option<u32> {
    let trimmed = text.trim();
    let digits = trimmed
        .strip_prefix("0x")
        .or_else(|| trimmed.strip_prefix("0X"))
        .unwrap_or(trimmed);
    if digits.is_empty() {
        return None;
    }
    u32::from_str_radix(digits, 16).ok()
}
