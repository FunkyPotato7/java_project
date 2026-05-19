package ua.codex.repaircalc;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RepairEstimatorFrame extends JFrame {
    public RepairEstimatorFrame() {
        super("RenovaCalc - калькулятор ремонту");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setLocationByPlatform(true);

        RepairEstimatorPanel panel = new RepairEstimatorPanel();
        setJMenuBar(createMenuBar(panel));
        setContentPane(panel);
        pack();
    }

    private JMenuBar createMenuBar(RepairEstimatorPanel panel) {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("Файл");
        JMenuItem sample = new JMenuItem("Заповнити приклад");
        sample.addActionListener(event -> panel.loadSampleProject());
        JMenuItem export = new JMenuItem("Експортувати кошторис...");
        export.addActionListener(event -> panel.chooseExportFile());
        JMenuItem exit = new JMenuItem("Вийти");
        exit.addActionListener(event -> dispose());
        file.add(sample);
        file.add(export);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Довідка");
        JMenuItem about = new JMenuItem("Про програму");
        about.addActionListener(event -> JOptionPane.showMessageDialog(
                this,
                "RenovaCalc допомагає швидко оцінити бюджет ремонту за площею, типом робіт і коефіцієнтами складності.",
                "Про програму",
                JOptionPane.INFORMATION_MESSAGE
        ));
        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }
}

final class RepairEstimatorPanel extends JPanel {
    private static final Locale UK_LOCALE = Locale.forLanguageTag("uk-UA");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(UK_LOCALE);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JTextField clientField = new JTextField("Навчальний проєкт", 18);
    private final JTextField objectField = new JTextField("Квартира", 18);
    private final JSpinner areaSpinner = new JSpinner(new SpinnerNumberModel(48.0, 5.0, 500.0, 1.0));
    private final JSpinner roomSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 12, 1));
    private final JSpinner budgetSpinner = new JSpinner(new SpinnerNumberModel(250000, 10000, 5000000, 10000));
    private final JComboBox<String> qualityBox = new JComboBox<>(new String[]{"Економ", "Стандарт", "Преміум"});
    private final JComboBox<String> cityBox = new JComboBox<>(new String[]{"Київ", "Львів", "Дніпро", "Одеса", "Інше місто"});
    private final JCheckBox urgentCheck = new JCheckBox("Терміново (+15%)");
    private final JCheckBox designCheck = new JCheckBox("Дизайн-супровід (+8%)", true);
    private final JCheckBox reserveCheck = new JCheckBox("Резерв матеріалів (+7%)", true);
    private final JRadioButton apartmentRadio = new JRadioButton("Квартира", true);
    private final JRadioButton houseRadio = new JRadioButton("Будинок");
    private final JSlider complexitySlider = new JSlider(1, 5, 3);
    private final JList<WorkType> workList = new JList<>(WorkType.values());
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Робота", "Обсяг", "Од.", "Базова ціна", "Коеф.", "Сума"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable estimateTable = new JTable(tableModel);
    private final JTextArea summaryArea = new JTextArea();
    private final JLabel totalLabel = new JLabel("Разом: 0 грн");
    private final JLabel statusLabel = new JLabel("Готово до розрахунку");
    private final JProgressBar budgetProgress = new JProgressBar(0, 100);
    private final JTabbedPane tabs = new JTabbedPane();

    private ProjectEstimate lastEstimate = ProjectEstimate.empty();

    RepairEstimatorPanel() {
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(16, 18, 16, 18));
        setBackground(new Color(247, 249, 252));

        configureLook();
        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        calculateEstimate();
    }

    void loadSampleProject() {
        clientField.setText("Сім'я Коваль");
        objectField.setText("Двокімнатна квартира");
        areaSpinner.setValue(62.0);
        roomSpinner.setValue(3);
        budgetSpinner.setValue(360000);
        qualityBox.setSelectedItem("Стандарт");
        cityBox.setSelectedItem("Львів");
        urgentCheck.setSelected(false);
        designCheck.setSelected(true);
        reserveCheck.setSelected(true);
        apartmentRadio.setSelected(true);
        complexitySlider.setValue(4);
        workList.setSelectedIndices(new int[]{0, 1, 2, 3, 5});
        calculateEstimate();
    }

    void chooseExportFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Зберегти кошторис");
        chooser.setSelectedFile(Path.of("koshtorys-renovacalc.txt").toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        try {
            exportEstimate(file);
            statusLabel.setText("Кошторис збережено: " + file.getFileName());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.toFile());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Не вдалося зберегти файл: " + ex.getMessage(), "Помилка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    void exportEstimate(Path file) throws IOException {
        calculateEstimate();
        Files.writeString(file, createExportText(), StandardCharsets.UTF_8);
    }

    void calculateEstimate() {
        List<WorkType> selected = workList.getSelectedValuesList();
        if (selected.isEmpty()) {
            selected = List.of(WorkType.WALLS, WorkType.FLOOR, WorkType.CEILING);
            workList.setSelectedIndices(new int[]{0, 1, 2});
        }

        double area = ((Number) areaSpinner.getValue()).doubleValue();
        int rooms = ((Number) roomSpinner.getValue()).intValue();
        int budget = ((Number) budgetSpinner.getValue()).intValue();
        double coefficient = qualityCoefficient()
                * cityCoefficient()
                * objectCoefficient()
                * (1.0 + (complexitySlider.getValue() - 3) * 0.08)
                * (1.0 + Math.max(0, rooms - 1) * 0.015);

        tableModel.setRowCount(0);
        List<EstimateRow> rows = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (WorkType type : selected) {
            double quantity = type.quantity(area, rooms);
            BigDecimal base = money(type.pricePerUnit() * quantity);
            BigDecimal sum = money(base.doubleValue() * coefficient);
            EstimateRow row = new EstimateRow(type.title(), quantity, type.unit(), base, coefficient, sum);
            rows.add(row);
            subtotal = subtotal.add(sum);
            tableModel.addRow(new Object[]{
                    row.name(),
                    formatQuantity(row.quantity()),
                    row.unit(),
                    MONEY.format(row.base()),
                    String.format(UK_LOCALE, "%.2f", row.coefficient()),
                    MONEY.format(row.total())
            });
        }

        BigDecimal extra = BigDecimal.ZERO;
        extra = addPercentRow(extra, subtotal, urgentCheck.isSelected(), "Терміновість", 0.15);
        extra = addPercentRow(extra, subtotal, designCheck.isSelected(), "Дизайн-супровід", 0.08);
        extra = addPercentRow(extra, subtotal, reserveCheck.isSelected(), "Резерв матеріалів", 0.07);

        BigDecimal total = subtotal.add(extra);
        lastEstimate = new ProjectEstimate(rows, subtotal, extra, total, budget);
        updateSummary();
        updateBudgetProgress(total.doubleValue(), budget);
        totalLabel.setText("Разом: " + MONEY.format(total));
        statusLabel.setText("Розраховано " + LocalDate.now().format(DATE_FORMAT) + " для " + selected.size() + " видів робіт");
    }

    private BigDecimal addPercentRow(BigDecimal extra, BigDecimal subtotal, boolean enabled, String name, double percent) {
        if (!enabled) {
            return extra;
        }

        BigDecimal value = money(subtotal.doubleValue() * percent);
        tableModel.addRow(new Object[]{
                name,
                String.format(UK_LOCALE, "%.0f%%", percent * 100),
                "послуга",
                MONEY.format(subtotal),
                String.format(UK_LOCALE, "%.2f", percent),
                MONEY.format(value)
        });
        return extra.add(value);
    }

    private Component createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);

        JLabel title = new JLabel("RenovaCalc");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(new Color(30, 47, 76));

        JLabel subtitle = new JLabel("Калькулятор бюджету ремонту з деталізованим кошторисом");
        subtitle.setForeground(new Color(78, 89, 108));
        subtitle.setFont(subtitle.getFont().deriveFont(15f));

        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);

        JButton sampleButton = new JButton("Приклад");
        sampleButton.addActionListener(event -> loadSampleProject());
        JButton calculateButton = new JButton("Розрахувати");
        calculateButton.addActionListener(event -> calculateEstimate());
        JButton exportButton = new JButton("Експорт");
        exportButton.addActionListener(event -> chooseExportFile());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(sampleButton);
        actions.add(calculateButton);
        actions.add(exportButton);

        header.add(titleBox, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private Component createMainContent() {
        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);

        main.add(createInputPanel(), BorderLayout.WEST);
        main.add(createTabs(), BorderLayout.CENTER);
        return main;
    }

    private Component createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(221, 227, 236)),
                new EmptyBorder(14, 14, 14, 14)
        ));
        panel.setPreferredSize(new Dimension(330, 560));

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(apartmentRadio);
        typeGroup.add(houseRadio);

        complexitySlider.setMajorTickSpacing(1);
        complexitySlider.setPaintTicks(true);
        complexitySlider.setPaintLabels(true);
        complexitySlider.setSnapToTicks(true);

        workList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        workList.setVisibleRowCount(6);
        workList.setSelectedIndices(new int[]{0, 1, 2, 3});

        int row = 0;
        addFormTitle(panel, row++, "Параметри проєкту");
        addField(panel, row++, "Замовник", clientField);
        addField(panel, row++, "Об'єкт", objectField);
        addField(panel, row++, "Площа, м2", areaSpinner);
        addField(panel, row++, "Кімнати", roomSpinner);
        addField(panel, row++, "Бюджет, грн", budgetSpinner);
        addField(panel, row++, "Якість", qualityBox);
        addField(panel, row++, "Місто", cityBox);

        JPanel typePanel = new JPanel();
        typePanel.setOpaque(false);
        typePanel.add(apartmentRadio);
        typePanel.add(houseRadio);
        addField(panel, row++, "Тип", typePanel);

        addField(panel, row++, "Складність", complexitySlider);

        JPanel checks = new JPanel(new GridBagLayout());
        checks.setOpaque(false);
        addCheck(checks, urgentCheck, 0);
        addCheck(checks, designCheck, 1);
        addCheck(checks, reserveCheck, 2);
        addField(panel, row++, "Опції", checks);

        JScrollPane workScroll = new JScrollPane(workList);
        workScroll.setPreferredSize(new Dimension(210, 108));
        addField(panel, row++, "Роботи", workScroll);

        JButton calculateButton = new JButton("Розрахувати кошторис");
        calculateButton.addActionListener((ActionEvent event) -> calculateEstimate());
        JButton clearButton = new JButton("Очистити");
        clearButton.addActionListener(event -> resetForm());

        JPanel buttons = new JPanel(new BorderLayout(8, 0));
        buttons.setOpaque(false);
        buttons.add(calculateButton, BorderLayout.CENTER);
        buttons.add(clearButton, BorderLayout.EAST);
        addWide(panel, row++, buttons);

        addWide(panel, row, Box.createVerticalGlue());
        return panel;
    }

    private Component createTabs() {
        estimateTable.setRowHeight(28);
        estimateTable.getTableHeader().setReorderingAllowed(false);
        estimateTable.setFillsViewportHeight(true);
        int[] widths = {310, 70, 70, 140, 70, 140};
        for (int i = 0; i < widths.length; i++) {
            estimateTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < estimateTable.getColumnCount(); i++) {
            estimateTable.getColumnModel().getColumn(i).setCellRenderer(right);
        }

        JPanel estimatePanel = new JPanel(new BorderLayout(0, 12));
        estimatePanel.setOpaque(false);
        estimatePanel.add(new JScrollPane(estimateTable), BorderLayout.CENTER);

        JPanel totalPanel = new JPanel(new BorderLayout(10, 0));
        totalPanel.setOpaque(false);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 20f));
        totalLabel.setForeground(new Color(24, 94, 73));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        totalPanel.add(budgetProgress, BorderLayout.CENTER);
        estimatePanel.add(totalPanel, BorderLayout.SOUTH);

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        summaryArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel aboutPanel = new JPanel(new BorderLayout());
        aboutPanel.setBackground(Color.WHITE);
        JTextArea about = new JTextArea("""
                Призначення:
                Програма оцінює бюджет ремонту на основі площі, типу робіт, міста, складності та додаткових опцій.

                Основні компоненти:
                JTextField, JSpinner, JComboBox, JCheckBox, JRadioButton, JSlider, JList, JTable, JTextArea, JButton, JTabbedPane, JProgressBar, JMenuBar.

                Практична користь:
                Користувач отримує орієнтовний кошторис, бачить перевищення бюджету та може експортувати результат у текстовий файл.
                """);
        about.setEditable(false);
        about.setLineWrap(true);
        about.setWrapStyleWord(true);
        about.setFont(about.getFont().deriveFont(15f));
        about.setBorder(new EmptyBorder(18, 18, 18, 18));
        aboutPanel.add(about, BorderLayout.CENTER);

        tabs.addTab("Кошторис", estimatePanel);
        tabs.addTab("Підсумок", new JScrollPane(summaryArea));
        tabs.addTab("Про застосунок", aboutPanel);
        return tabs;
    }

    private Component createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel.setForeground(new Color(78, 89, 108));
        footer.add(statusLabel, BorderLayout.WEST);
        return footer;
    }

    private void resetForm() {
        clientField.setText("");
        objectField.setText("");
        areaSpinner.setValue(48.0);
        roomSpinner.setValue(2);
        budgetSpinner.setValue(250000);
        qualityBox.setSelectedIndex(1);
        cityBox.setSelectedIndex(0);
        urgentCheck.setSelected(false);
        designCheck.setSelected(false);
        reserveCheck.setSelected(true);
        apartmentRadio.setSelected(true);
        complexitySlider.setValue(3);
        workList.setSelectedIndices(new int[]{0, 1, 2});
        calculateEstimate();
    }

    private void updateSummary() {
        String text = """
                КОШТОРИС RENOVACALC
                Дата: %s
                Замовник: %s
                Об'єкт: %s (%s)
                Місто: %s
                Площа: %.1f м2, кімнат: %d
                Рівень якості: %s
                Складність: %d з 5

                Підсумки:
                Базова сума робіт: %s
                Додаткові нарахування: %s
                Орієнтовна вартість: %s
                Запланований бюджет: %s
                Статус бюджету: %s

                Рекомендація:
                %s
                """.formatted(
                LocalDate.now().format(DATE_FORMAT),
                valueOrDash(clientField.getText()),
                valueOrDash(objectField.getText()),
                apartmentRadio.isSelected() ? "квартира" : "будинок",
                cityBox.getSelectedItem(),
                ((Number) areaSpinner.getValue()).doubleValue(),
                ((Number) roomSpinner.getValue()).intValue(),
                qualityBox.getSelectedItem(),
                complexitySlider.getValue(),
                MONEY.format(lastEstimate.subtotal()),
                MONEY.format(lastEstimate.extra()),
                MONEY.format(lastEstimate.total()),
                MONEY.format(BigDecimal.valueOf(lastEstimate.budget())),
                lastEstimate.total().doubleValue() <= lastEstimate.budget() ? "у межах бюджету" : "потрібне коригування",
                budgetAdvice()
        );
        summaryArea.setText(text);
        summaryArea.setCaretPosition(0);
    }

    private String createExportText() {
        StringBuilder builder = new StringBuilder(summaryArea.getText()).append(System.lineSeparator());
        builder.append("Деталізація:").append(System.lineSeparator());
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            builder.append("- ")
                    .append(tableModel.getValueAt(i, 0)).append(": ")
                    .append(tableModel.getValueAt(i, 5))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void updateBudgetProgress(double total, int budget) {
        int percent = budget <= 0 ? 100 : (int) Math.round(total / budget * 100.0);
        budgetProgress.setValue(Math.min(percent, 100));
        budgetProgress.setStringPainted(true);
        budgetProgress.setString(percent + "% від бюджету");
        budgetProgress.setForeground(percent <= 90 ? new Color(46, 125, 95) : percent <= 110 ? new Color(203, 134, 39) : new Color(181, 63, 63));
    }

    private String budgetAdvice() {
        double total = lastEstimate.total().doubleValue();
        if (total <= lastEstimate.budget() * 0.9) {
            return "Є запас бюджету. Можна залишити резерв на непередбачені витрати або підвищити якість окремих робіт.";
        }
        if (total <= lastEstimate.budget()) {
            return "Кошторис близький до бюджету. Варто контролювати закупівлі матеріалів.";
        }
        return "Сума перевищує бюджет. Рекомендовано зменшити складність, змінити рівень якості або прибрати необов'язкові опції.";
    }

    private double qualityCoefficient() {
        return switch ((String) qualityBox.getSelectedItem()) {
            case "Економ" -> 0.9;
            case "Преміум" -> 1.35;
            default -> 1.0;
        };
    }

    private double cityCoefficient() {
        return switch ((String) cityBox.getSelectedItem()) {
            case "Київ" -> 1.12;
            case "Львів" -> 1.06;
            case "Одеса" -> 1.08;
            case "Дніпро" -> 1.02;
            default -> 1.0;
        };
    }

    private double objectCoefficient() {
        return houseRadio.isSelected() ? 1.08 : 1.0;
    }

    private void addFormTitle(JPanel panel, int row, String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 17f));
        label.setForeground(new Color(30, 47, 76));
        addWide(panel, row, label);
    }

    private void addField(JPanel panel, int row, String labelText, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 0, 5, 8);
        JLabel label = new JLabel(labelText);
        panel.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 0, 5, 0);
        panel.add(field, fieldConstraints);
    }

    private void addCheck(JPanel panel, JCheckBox box, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        panel.add(box, constraints);
    }

    private void addWide(JPanel panel, int row, Component component) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(6, 0, 6, 0);
        panel.add(component, constraints);
    }

    private void configureLook() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
            // The default cross-platform look is acceptable if the system theme is unavailable.
        }
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String formatQuantity(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.01) {
            return String.format(UK_LOCALE, "%.0f", value);
        }
        return String.format(UK_LOCALE, "%.1f", value);
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}

enum WorkType {
    WALLS("Стіни: шпаклювання та фарбування", "м2", 260.0),
    FLOOR("Підлога: ламінат/плінтус", "м2", 520.0),
    CEILING("Стеля: підготовка та фарбування", "м2", 210.0),
    ELECTRIC("Електрика: точки та автоматика", "точка", 480.0),
    PLUMBING("Сантехніка: вузли підключення", "вузол", 1850.0),
    DOORS("Міжкімнатні двері", "шт.", 3200.0),
    CLEANUP("Фінішне прибирання", "м2", 55.0);

    private final String title;
    private final String unit;
    private final double pricePerUnit;

    WorkType(String title, String unit, double pricePerUnit) {
        this.title = title;
        this.unit = unit;
        this.pricePerUnit = pricePerUnit;
    }

    String title() {
        return title;
    }

    String unit() {
        return unit;
    }

    double pricePerUnit() {
        return pricePerUnit;
    }

    double quantity(double area, int rooms) {
        return switch (this) {
            case WALLS -> area * 2.6;
            case FLOOR, CEILING, CLEANUP -> area;
            case ELECTRIC -> rooms * 7.0 + area / 12.0;
            case PLUMBING -> Math.max(2, Math.ceil(rooms / 2.0) + 1);
            case DOORS -> rooms + 1.0;
        };
    }

    @Override
    public String toString() {
        return title;
    }
}

record EstimateRow(String name, double quantity, String unit, BigDecimal base, double coefficient, BigDecimal total) {
}

record ProjectEstimate(List<EstimateRow> rows, BigDecimal subtotal, BigDecimal extra, BigDecimal total, int budget) {
    static ProjectEstimate empty() {
        return new ProjectEstimate(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}
