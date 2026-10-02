use eframe::egui;
use egui::{Align2, FontId, Key, Rect, Sense, Stroke, TextEdit, Ui, pos2, vec2};
use perilus_core::perilus::Perilus;

use crate::app::{
    ADDR_W, BYTES_PER_ROW, DIVIDER_GAP, FONT_SIZE, GROUP_GAP, PC_COLOR, ROW_H, SimApp,
    WORDS_PER_ROW, char_width,
};
use crate::state::{Focus, ViewMode, hex_char, printable, row_digits};

fn divider(ui: &mut Ui) {
    let (rect, _) = ui.allocate_exact_size(vec2(DIVIDER_GAP * 2.0, ROW_H), Sense::hover());
    let color = ui.visuals().weak_text_color();
    let spacing = ui.spacing().item_spacing.y;
    ui.painter().line_segment(
        [
            pos2(rect.center().x, rect.top() - spacing * 0.5),
            pos2(rect.center().x, rect.bottom() + spacing * 0.5),
        ],
        Stroke::new(1.0, color),
    );
}

impl SimApp {
    pub(crate) fn memory_ui(&mut self, ui: &mut Ui) {
        let font = FontId::monospace(FONT_SIZE);
        let nib_w = char_width(ui, &font);

        ui.horizontal_wrapped(|ui| {
            ui.strong("Memory");
            ui.toggle_value(&mut self.show_aliases, "ABI names [x]");
            let mut byte_view = self.view == ViewMode::Byte;
            if ui.toggle_value(&mut byte_view, "Byte view [v]").changed() {
                self.view = if byte_view {
                    ViewMode::Byte
                } else {
                    ViewMode::Word
                };
            }
            let mut follow = self.follow_pc;
            if ui
                .toggle_value(&mut follow, "Follow PC [shift+p]")
                .changed()
            {
                self.follow_pc = follow;
                self.follow_pc_last = self.current_pc;
                self.status = format!("Follow PC {}", if follow { "on" } else { "off" });
            }
            ui.separator();
            ui.label("addr");
            let response = ui.add(
                TextEdit::singleline(&mut self.address_input)
                    .desired_width(90.0)
                    .hint_text("hex"),
            );
            if std::mem::take(&mut self.focus_address) {
                response.request_focus();
            }
            if response.lost_focus() && ui.input(|i| i.key_pressed(Key::Enter)) {
                self.jump_to_address();
            }
            if ui.button("Go [g]").clicked() {
                self.jump_to_address();
            }
            ui.separator();
            ui.label(format!("selected 0x{:08x}", self.mem_addr));
        });
        ui.separator();

        if self.follow_pc && self.current_pc != self.follow_pc_last {
            self.follow_pc_last = self.current_pc;
            let pc_row = self.current_pc as usize / BYTES_PER_ROW;
            if !self.mem_viewport.contains(&pc_row) {
                self.scroll_to_row = Some(pc_row);
            }
        }

        let row_pitch = ROW_H + ui.spacing().item_spacing.y;
        let mut area = egui::ScrollArea::both().auto_shrink([false, false]);
        if let Some(row) = self.scroll_to_row.take() {
            area = area.vertical_scroll_offset(row as f32 * row_pitch);
        }
        let total_rows = Perilus::MEMORY_SIZE_WORDS / WORDS_PER_ROW;
        area.show_rows(ui, ROW_H, total_rows, |ui, rows| {
            self.mem_viewport = rows.clone();
            for row in rows {
                self.memory_row(ui, row, nib_w, &font);
            }
        });
    }

    fn memory_row(&mut self, ui: &mut Ui, row: usize, nib_w: f32, font: &FontId) {
        let row_addr = (row * BYTES_PER_ROW) as u32;
        ui.horizontal(|ui| {
            let text_color = ui.visuals().text_color();
            let background = ui.visuals().panel_fill;
            let pc = self.current_pc;
            let focus_memory = self.focus == Focus::Memory;
            let cursor_in_row =
                focus_memory && (self.mem_addr / BYTES_PER_ROW as u32) as usize == row;
            let cursor_index = (self.mem_addr % BYTES_PER_ROW as u32) as usize;

            let (addr_rect, _) = ui.allocate_exact_size(vec2(ADDR_W, ROW_H), Sense::hover());
            ui.painter().text(
                addr_rect.left_center(),
                Align2::LEFT_CENTER,
                format!("{row_addr:08x}"),
                font.clone(),
                text_color,
            );
            divider(ui);

            let group = match self.view {
                ViewMode::Word => 8,
                ViewMode::Byte => 2,
            };
            let gaps = (64 / group - 1) as f32;
            let digit_w = 64.0 * nib_w + gaps * GROUP_GAP;
            let (digits_rect, digits_response) =
                ui.allocate_exact_size(vec2(digit_w, ROW_H), Sense::click());

            let order = row_digits(self.view);
            let cursor = (self.mem_addr, self.mem_nib);
            let mut cells: Vec<(Rect, u32, u8)> = Vec::with_capacity(64);
            let mut x = digits_rect.left();
            for (i, &(offset, nib)) in order.iter().enumerate() {
                if i > 0 && i % group == 0 {
                    x += GROUP_GAP;
                }
                let cell = Rect::from_min_size(pos2(x, digits_rect.top()), vec2(nib_w, ROW_H));
                let addr = row_addr + offset;
                let byte = self.perilus.get_memory_byte(addr);
                let digit = if nib == 0 { byte >> 4 } else { byte & 0xf };
                if focus_memory && (addr, nib) == cursor {
                    ui.painter().rect_filled(cell, 0.0, text_color);
                    ui.painter().text(
                        cell.center(),
                        Align2::CENTER_CENTER,
                        hex_char(digit),
                        font.clone(),
                        background,
                    );
                } else {
                    let color = if (addr & !3) == (pc & !3) {
                        PC_COLOR
                    } else {
                        text_color
                    };
                    ui.painter().text(
                        cell.center(),
                        Align2::CENTER_CENTER,
                        hex_char(digit),
                        font.clone(),
                        color,
                    );
                }
                cells.push((cell, addr, nib));
                x += nib_w;
            }

            if let Some(pos) = digits_response.interact_pointer_pos()
                && (digits_response.clicked() || digits_response.double_clicked())
                && let Some(&(_, addr, nib)) = cells.iter().find(|(rect, _, _)| rect.contains(pos))
            {
                self.mem_addr = addr;
                self.mem_nib = nib;
                self.focus = Focus::Memory;
                if digits_response.double_clicked() {
                    self.set_pc_to_cursor();
                }
            }

            divider(ui);

            let ascii: Vec<char> = match self.view {
                ViewMode::Word => (0..WORDS_PER_ROW)
                    .flat_map(|word| {
                        self.perilus.get_memory()[row * WORDS_PER_ROW + word].to_le_bytes()
                    })
                    .map(printable)
                    .collect(),
                ViewMode::Byte => (0..BYTES_PER_ROW)
                    .map(|i| printable(self.perilus.get_memory_byte(row_addr + i as u32)))
                    .collect(),
            };
            let ascii_width = ascii.len() as f32 * nib_w;
            let (ascii_rect, _) = ui.allocate_exact_size(vec2(ascii_width, ROW_H), Sense::hover());
            let painter = ui.painter();
            for (i, ch) in ascii.iter().enumerate() {
                let cell = Rect::from_min_size(
                    pos2(ascii_rect.left() + i as f32 * nib_w, ascii_rect.top()),
                    vec2(nib_w, ROW_H),
                );
                if cursor_in_row && i == cursor_index {
                    painter.rect_filled(cell, 0.0, text_color);
                    painter.text(
                        cell.center(),
                        Align2::CENTER_CENTER,
                        *ch,
                        font.clone(),
                        background,
                    );
                } else {
                    let color = if (row_addr + i as u32) & !3 == pc & !3 {
                        PC_COLOR
                    } else {
                        text_color
                    };
                    painter.text(
                        cell.center(),
                        Align2::CENTER_CENTER,
                        *ch,
                        font.clone(),
                        color,
                    );
                }
            }
        });
    }
}
