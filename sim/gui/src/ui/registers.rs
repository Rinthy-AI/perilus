use eframe::egui;
use egui::{Align2, FontId, Rect, Sense, Ui, pos2, vec2};

use crate::app::{
    FONT_SIZE, NUM_CELLS, PC_COLOR, REG_COL_GAP, REG_COLS, REG_LABEL_W, REG_ROWS, ROW_H, SimApp,
    char_width,
};
use crate::state::{Focus, hex_char, register_alias};

impl SimApp {
    pub(crate) fn registers_ui(&mut self, ui: &mut Ui) {
        ui.horizontal_wrapped(|ui| {
            ui.label(format!("state: {}", self.run_state));
            ui.separator();
            ui.label(format!("cu: {}", self.perilus.get_control_unit_state()));
            ui.separator();
            ui.label(self.status.as_str());
        });
        ui.add_space(4.0);

        let font = FontId::monospace(FONT_SIZE);
        let nib_w = char_width(ui, &font);
        let cell_w = REG_LABEL_W + 8.0 * nib_w + REG_COL_GAP;
        let grid_size = vec2(cell_w * REG_COLS as f32, ROW_H * REG_ROWS as f32);
        let (grid_rect, response) = ui.allocate_exact_size(grid_size, Sense::click());

        let text_color = ui.visuals().text_color();
        let background = ui.visuals().panel_fill;
        let painter = ui.painter();
        let registers = self.perilus.get_registers();
        let focus_registers = self.focus == Focus::Registers;
        for col in 0..REG_COLS {
            for row in 0..REG_ROWS {
                let cell_index = col * REG_ROWS + row;
                let origin = pos2(
                    grid_rect.left() + col as f32 * cell_w,
                    grid_rect.top() + row as f32 * ROW_H,
                );
                let (label, value, color) = if cell_index == 0 {
                    ("pc".to_string(), self.current_pc, PC_COLOR)
                } else {
                    let label = if self.show_aliases {
                        register_alias(cell_index).to_string()
                    } else {
                        format!("x{cell_index}")
                    };
                    (label, registers[cell_index], text_color)
                };
                painter.text(
                    pos2(origin.x, origin.y + ROW_H * 0.5),
                    Align2::LEFT_CENTER,
                    label,
                    font.clone(),
                    text_color,
                );
                for nib in 0..8u8 {
                    let digit = ((value >> ((7 - nib as u32) * 4)) & 0xf) as u8;
                    let cell = Rect::from_min_size(
                        pos2(origin.x + REG_LABEL_W + nib as f32 * nib_w, origin.y),
                        vec2(nib_w, ROW_H),
                    );
                    let selected = focus_registers
                        && cell_index as u8 == self.reg_index
                        && nib == self.reg_nib;
                    if selected {
                        painter.rect_filled(cell, 0.0, text_color);
                        painter.text(
                            cell.center(),
                            Align2::CENTER_CENTER,
                            hex_char(digit),
                            font.clone(),
                            background,
                        );
                    } else {
                        painter.text(
                            cell.center(),
                            Align2::CENTER_CENTER,
                            hex_char(digit),
                            font.clone(),
                            color,
                        );
                    }
                }
            }
        }

        if response.clicked()
            && let Some(pos) = response.interact_pointer_pos()
        {
            let x = pos.x - grid_rect.left();
            let y = pos.y - grid_rect.top();
            if x >= 0.0 && y >= 0.0 {
                let col = (x / cell_w) as usize;
                let row = (y / ROW_H) as usize;
                let x_in_cell = x - col as f32 * cell_w;
                if col < REG_COLS && row < REG_ROWS && x_in_cell >= REG_LABEL_W {
                    let nib = (((x_in_cell - REG_LABEL_W) / nib_w) as usize).min(7);
                    self.reg_index = (col * REG_ROWS + row).min(NUM_CELLS - 1) as u8;
                    self.reg_nib = nib as u8;
                    self.focus = Focus::Registers;
                }
            }
        }
    }
}
