mod app;
mod state;
mod ui;

use std::path::PathBuf;

use app::{FONT_SIZE, SimApp};

fn main() -> eframe::Result {
    let file = std::env::args_os().nth(1).map(PathBuf::from);
    let options = eframe::NativeOptions {
        viewport: eframe::egui::ViewportBuilder::default()
            .with_inner_size([1420.0, 900.0])
            .with_min_inner_size([900.0, 480.0])
            .with_title("perilus"),
        ..Default::default()
    };
    eframe::run_native(
        "perilus",
        options,
        Box::new(move |cc| {
            cc.egui_ctx.all_styles_mut(|style| {
                style.override_font_id = Some(eframe::egui::FontId::monospace(FONT_SIZE));
            });
            let mut app = SimApp::new();
            if let Some(file) = file {
                app.load_path(file);
            }
            Ok(Box::new(app))
        }),
    )
}
