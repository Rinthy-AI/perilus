use eframe::egui;
use egui::Ui;

use crate::app::SimApp;
use crate::state::RunState;

impl SimApp {
    pub(crate) fn controls_ui(&mut self, ui: &mut Ui) {
        // Two rows keep the controls within the width of the memory view.
        ui.horizontal_wrapped(|ui| {
            if ui.button("Reset [ctrl+r]").clicked() {
                self.reset();
            }
            if ui.button("Step cycle [space]").clicked() {
                self.step_cycle();
            }
            if ui.button("Next instruction [n]").clicked() {
                self.next_instruction();
            }
            let free_run_label = if self.run_state == RunState::FreeRun {
                "Stop [ctrl+space]"
            } else {
                "Free run [ctrl+space]"
            };
            if ui.button(free_run_label).clicked() {
                self.toggle_free_run();
            }
        });
        ui.horizontal_wrapped(|ui| {
            if ui.button("Set PC to cursor [p]").clicked() {
                self.set_pc_to_cursor();
            }
            if ui.button("Run to cursor [ctrl+shift+space]").clicked() {
                self.run_to_cursor();
            }
            if ui.button("Run to PC").clicked() {
                self.run_to_input_pc();
            }
            if ui.button("Load memory").clicked() {
                self.load_memory();
            }
            if ui.button("Save memory").clicked() {
                self.save_memory();
            }
        });
        ui.horizontal_wrapped(|ui| {
            let mut tracing = self.tracing;
            if ui.toggle_value(&mut tracing, "Trace [t]").changed() {
                self.toggle_trace();
            }
        });
    }
}
