package ua.codex.repaircalc;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && "--screenshot".equalsIgnoreCase(args[0])) {
            Path output = args.length > 1 ? Path.of(args[1]) : Path.of("docs", "screenshots");
            SwingUtilities.invokeLater(() -> ScreenshotGenerator.createScreenshots(output));
            return;
        }

        SwingUtilities.invokeLater(() -> {
            RepairEstimatorFrame frame = new RepairEstimatorFrame();
            frame.setVisible(true);
        });
    }
}
