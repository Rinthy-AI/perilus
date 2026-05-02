use std::{fmt::Display, time::Duration};

use crossterm::event::{self, Event, KeyCode, KeyEvent};
use ratatui::{
    DefaultTerminal, Frame,
    buffer::Buffer,
    layout::Rect,
    style::{Color, Style},
    widgets::{Paragraph, Widget},
};

use crate::perilus::{MemoryIndex, Perilus, Register};

mod perilus;

enum Mode {
    Normal,
    EditPc,
    EditRegister,
    EditMemory,
}

#[derive(PartialEq)]
enum RunState {
    Idle,
    Step { ignore_current_state: bool },
    RunTo { index: u32 },
    Run,
}

impl Display for RunState {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            RunState::Idle => write!(f, "Idle"),
            RunState::Step { .. } => write!(f, "Step"),
            RunState::RunTo { index } => write!(f, "Run to 0x{:08x}", index * 4),
            RunState::Run => write!(f, "Run"),
        }
    }
}

struct App {
    perilus: Perilus,
    current_pc: u32,
    show_register_aliases: bool,
    register_cursor: Register,
    memory_cursor: MemoryIndex,
    memory_top: MemoryIndex,
    memory_bottom: MemoryIndex,
    mode: Mode,
    run_state: RunState,
    exit: bool,
}

impl App {
    fn new(perilus: Perilus) -> App {
        let current_pc = perilus.get_pc();
        App {
            perilus,
            current_pc,
            show_register_aliases: true,
            register_cursor: Register::const_new::<0>(),
            memory_cursor: MemoryIndex::const_new::<0>(),
            memory_top: MemoryIndex::const_new::<0>(),
            memory_bottom: MemoryIndex::const_new::<0>(),
            mode: Mode::Normal,
            run_state: RunState::Idle,
            exit: false,
        }
    }
    fn run(&mut self, terminal: &mut DefaultTerminal) -> std::io::Result<()> {
        while !self.exit {
            let num_visible_memory_words = terminal
                .get_frame()
                .area()
                .height
                .saturating_sub(9)
                .saturating_mul(8);
            self.memory_bottom = self.memory_top + num_visible_memory_words as u32;
            if self.memory_cursor <= self.memory_top {
                self.memory_top = self.memory_cursor;
                self.memory_bottom = self.memory_top + num_visible_memory_words as u32;
            } else if self.memory_cursor >= self.memory_bottom {
                self.memory_bottom = self.memory_cursor.saturating_add(8);
                self.memory_top = self
                    .memory_bottom
                    .saturating_sub(num_visible_memory_words as u32);
            }
            self.memory_top -= self.memory_top % 8;
            self.memory_bottom -= self.memory_bottom % 8;
            terminal.draw(|frame| self.draw(frame))?;
            self.handle_events(self.run_state != RunState::Idle)?;
            self.run_state = match self.run_state {
                RunState::Idle => RunState::Idle,
                RunState::Step {
                    ignore_current_state,
                } => {
                    if self.perilus.get_control_unit_state() == 0 && !ignore_current_state {
                        RunState::Idle
                    } else {
                        self.perilus.pulse_clock();
                        RunState::Step {
                            ignore_current_state: false,
                        }
                    }
                }
                RunState::RunTo { index } => {
                    if let Mode::EditMemory = self.mode
                        && self.current_pc != index.saturating_mul(4)
                    {
                        self.perilus.pulse_clock();
                        RunState::RunTo { index }
                    } else {
                        RunState::Idle
                    }
                }
                RunState::Run => {
                    self.perilus.pulse_clock();
                    RunState::Run
                }
            };
            if self.perilus.get_control_unit_state() == 0 {
                self.current_pc = self.perilus.get_pc();
            }
        }
        Ok(())
    }
    fn draw(&self, frame: &mut Frame) {
        frame.render_widget(self, frame.area());
    }
    fn handle_events(&mut self, poll: bool) -> std::io::Result<()> {
        let event_ready = event::poll(Duration::from_millis(15))?;
        if (!poll || event_ready)
            && let Event::Key(k) = event::read()?
        {
            self.handle_key_event(k)
        }
        Ok(())
    }
    fn handle_key_event(&mut self, key_event: KeyEvent) {
        match key_event.code {
            KeyCode::Char('q') => {
                self.exit = true;
            }
            KeyCode::Char('R') => self.perilus.reset(),
            KeyCode::Char('n') => {
                self.show_register_aliases = !self.show_register_aliases;
            }
            KeyCode::Char(' ') => self.perilus.pulse_clock(),
            KeyCode::Char('F') => {
                self.run_state = RunState::Step {
                    ignore_current_state: true,
                };
            }
            KeyCode::Char('T') => {
                if let Mode::EditMemory = self.mode {
                    self.run_state = RunState::RunTo {
                        index: self.memory_cursor.get(),
                    };
                }
            }
            KeyCode::Char('G') => {
                self.run_state = RunState::Run;
            }

            KeyCode::Esc => {
                self.mode = Mode::Normal;
                self.run_state = RunState::Idle;
            }
            KeyCode::Char('r') => {
                if self.register_cursor == 0 {
                    self.mode = Mode::EditPc;
                } else {
                    self.mode = Mode::EditRegister;
                }
            }
            KeyCode::Char('m') => {
                self.mode = Mode::EditMemory;
            }

            KeyCode::Char('k') | KeyCode::Up => match self.mode {
                Mode::Normal | Mode::EditPc => (),
                Mode::EditRegister => {
                    if self.register_cursor % 8 != 0 {
                        self.register_cursor = self.register_cursor.saturating_sub(1)
                    }
                    if self.register_cursor == 0 {
                        self.mode = Mode::EditPc;
                    }
                }
                Mode::EditMemory => {
                    if self.memory_cursor >= 8 {
                        self.memory_cursor = self.memory_cursor.saturating_sub(8);
                    }
                }
            },
            KeyCode::Char('j') | KeyCode::Down => match self.mode {
                Mode::Normal => (),
                Mode::EditPc => {
                    self.mode = Mode::EditRegister;
                    self.register_cursor = Register::const_new::<1>()
                }
                Mode::EditRegister => {
                    if self.register_cursor % 8 != 7 {
                        self.register_cursor = self.register_cursor.saturating_add(1);
                    }
                }
                Mode::EditMemory => {
                    if self.memory_cursor <= 1016 {
                        self.memory_cursor = self.memory_cursor.saturating_add(8);
                    }
                }
            },
            KeyCode::Char('h') | KeyCode::Left => match self.mode {
                Mode::Normal | Mode::EditPc => (),
                Mode::EditRegister => {
                    if self.register_cursor > 7 {
                        self.register_cursor = self.register_cursor.saturating_sub(8);
                    }
                    if self.register_cursor == 0 {
                        self.mode = Mode::EditPc;
                    }
                }
                Mode::EditMemory => {
                    if self.memory_cursor % 8 != 0 {
                        self.memory_cursor = self.memory_cursor.saturating_sub(1);
                    }
                }
            },
            KeyCode::Char('l') | KeyCode::Right => match self.mode {
                Mode::Normal => (),
                Mode::EditPc => {
                    self.mode = Mode::EditRegister;
                    self.register_cursor = Register::const_new::<8>();
                }
                Mode::EditRegister => {
                    if self.register_cursor < 24 {
                        self.register_cursor =
                            (self.register_cursor + 8).min(Register::const_new::<31>());
                    }
                }
                Mode::EditMemory => {
                    if self.memory_cursor % 8 != 7 {
                        self.memory_cursor = self.memory_cursor.saturating_add(1);
                    }
                }
            },
            KeyCode::Char(n)
                if [
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
                ]
                .contains(&n) =>
            {
                let new_nibble = n.to_digit(16).unwrap();
                match self.mode {
                    Mode::Normal => (),
                    Mode::EditPc => {
                        let current_value = self.perilus.get_pc();
                        let new_value = (current_value << 4) + new_nibble;
                        self.perilus.set_pc(new_value);
                        self.current_pc = self.perilus.get_pc();
                    }
                    Mode::EditRegister => {
                        let current_value =
                            self.perilus.get_registers()[self.register_cursor.get() as usize];
                        let new_value = (current_value << 4) + new_nibble;
                        self.perilus.set_register(self.register_cursor, new_value);
                    }
                    Mode::EditMemory => {
                        let current_value =
                            self.perilus.get_memory()[self.memory_cursor.get() as usize];
                        let new_value = (current_value << 4) + new_nibble;
                        self.perilus.set_memory(self.memory_cursor.get(), new_value);
                    }
                }
            }
            KeyCode::Char('C') => match self.mode {
                Mode::Normal => (),
                Mode::EditPc => self.perilus.set_pc(0),
                Mode::EditRegister => self.perilus.set_register(self.register_cursor, 0),
                Mode::EditMemory => self.perilus.set_memory(self.memory_cursor.get(), 0),
            },
            KeyCode::Char('J') => {
                if let Mode::EditMemory = self.mode {
                    self.perilus.set_pc(self.memory_cursor.get() * 4);
                }
            }
            _ => (),
        }
    }
    // TODO move this method to Register
    fn register_alias(reg: Register) -> String {
        match reg.get() {
            0 => "zero".into(),
            1 => "ra".into(),
            2 => "sp".into(),
            3 => "gp".into(),
            4 => "tp".into(),
            r if (5..=7).contains(&r) => format!("t{}", r - 5),
            8 => "s0/fp".into(),
            9 => "s1".into(),
            r if (10..=17).contains(&r) => format!("a{}", r - 10),
            r if (18..=27).contains(&r) => format!("s{}", r - 16),
            r if (28..=31).contains(&r) => format!("t{}", r - 25),
            r => unreachable!("got invalid register '{r}'"),
        }
    }
}

impl Widget for &App {
    fn render(self, _area: Rect, buf: &mut Buffer) {
        let run_state_area = Rect::new(72, 6, 27, 1);
        Paragraph::new(format!("run state {}", self.run_state)).render(run_state_area, buf);
        let control_unit_state_area = Rect::new(72, 7, 27, 1);
        Paragraph::new(format!(
            " cu state {:<2}",
            self.perilus.get_control_unit_state()
        ))
        .render(control_unit_state_area, buf);
        let pc_area = Rect::new(0, 0, 17, 1);
        let mut pc_style = Style::default().fg(Color::Green);
        if let Mode::EditPc = self.mode {
            pc_style = pc_style.fg(Color::Black).bg(Color::Gray);
        }
        Paragraph::new(format!("      pc {:08x}", self.current_pc))
            .style(pc_style)
            .render(pc_area, buf);
        let register_file = self.perilus.get_registers();
        for (r, reg) in register_file.iter().enumerate() {
            if r == 0 {
                continue;
            }
            let reg_area = Rect::new(r as u16 / 8 * 18, r as u16 % 8, 17, 1);
            let mut reg_style = Style::default();
            if let Mode::EditRegister = self.mode
                && self.register_cursor.get() == r as u32
            {
                reg_style = reg_style.fg(Color::Black).bg(Color::Gray);
            }
            let reg_name = if self.show_register_aliases {
                App::register_alias(Register::new(r as u32).unwrap())
            } else {
                format!("x{r}")
            };
            Paragraph::new(format!("{reg_name:>8} {reg:08x}"))
                .style(reg_style)
                .render(reg_area, buf);
        }
        let memory = self.perilus.get_memory();
        for offset in self.memory_top.get() as u16..self.memory_bottom.get() as u16 {
            let row = (offset - self.memory_top.get() as u16) / 8 + 9;
            if offset % 8 == 0 {
                let label_area = Rect::new(0, row, 8, 1);
                Paragraph::new(format!("{:08x}", offset * 4)).render(label_area, buf);
            }
            let mem_area = Rect::new(offset % 8 * 9 + 9, row, 8, 1);
            let mut mem_style = Style::default();
            if self.current_pc == offset as u32 * 4 {
                mem_style = mem_style.fg(Color::Green);
            }
            if let Mode::EditMemory = self.mode
                && self.memory_cursor.get() == offset as u32
            {
                if self.memory_cursor.get() * 4 == self.current_pc {
                    mem_style = mem_style.fg(Color::White).bg(Color::Green);
                } else {
                    mem_style = mem_style.fg(Color::Black).bg(Color::White);
                }
            }
            Paragraph::new(format!("{:08x}", memory[offset as usize]))
                .style(mem_style)
                .render(mem_area, buf);
            let ascii_area = Rect::new(82 + (offset % 8) * 4, row, 4, 1);
            let ascii: String = memory[offset as usize]
                .to_le_bytes()
                .map(|b| {
                    if (b' '..=b'~').contains(&b) {
                        char::from_u32(b as u32).unwrap_or('.')
                    } else {
                        '.'
                    }
                })
                .iter()
                .collect();
            Paragraph::new(ascii)
                .style(mem_style)
                .render(ascii_area, buf);
        }
    }
}

fn main() -> color_eyre::Result<()> {
    let perilus = Perilus::init();
    let mut app = App::new(perilus);

    color_eyre::install()?;
    ratatui::run(|terminal| app.run(terminal))?;

    Ok(())
}
