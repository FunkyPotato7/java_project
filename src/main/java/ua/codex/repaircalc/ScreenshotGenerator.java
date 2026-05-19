package ua.codex.repaircalc;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class ScreenshotGenerator {
    private ScreenshotGenerator() {
    }

    static void createScreenshots(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);

            RepairEstimatorPanel startPanel = new RepairEstimatorPanel();
            startPanel.setPreferredSize(new Dimension(1280, 780));
            render(startPanel, outputDirectory.resolve("01-start-screen.png"));

            RepairEstimatorPanel resultPanel = new RepairEstimatorPanel();
            resultPanel.loadSampleProject();
            resultPanel.setPreferredSize(new Dimension(1280, 780));
            render(resultPanel, outputDirectory.resolve("02-calculated-estimate.png"));
            System.out.println("Screenshots saved to " + outputDirectory.toAbsolutePath());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create screenshots", ex);
        } finally {
            SwingUtilities.invokeLater(() -> System.exit(0));
        }
    }

    private static void render(JComponent component, Path file) throws IOException {
        Dimension size = component.getPreferredSize();
        component.setSize(size);
        component.doLayout();
        layoutChildren(component);

        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(247, 249, 252));
            graphics.fillRect(0, 0, size.width, size.height);
            component.printAll(graphics);
        } finally {
            graphics.dispose();
        }

        ImageIO.write(image, "png", file.toFile());
    }

    private static void layoutChildren(JComponent component) {
        for (java.awt.Component child : component.getComponents()) {
            if (child instanceof JComponent nested) {
                nested.doLayout();
                layoutChildren(nested);
            }
        }
    }
}
