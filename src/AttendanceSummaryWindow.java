import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceSummaryWindow extends JFrame {

    private static final int WINDOW_MARGIN = 18;
    private static final int SMALL_WIDTH_BREAKPOINT = 1100;

    // these fields build the summary report with fixed student columns and scrollable date columns
    private final Connection con;
    private final JTextField txtAnchorDate = new JTextField(10);
    private final JTextField txtSearch = new JTextField(12);
    private final JComboBox<String> cmbSubject = new JComboBox<>(prependAll(CRUD_GUI.SUBJECTS));
    private final JComboBox<String> cmbPeriod = new JComboBox<>(new String[]{"Weekly", "Monthly"});
    private final JComboBox<String> cmbCountRange = new JComboBox<>(new String[]{"Current View Count", "1 Month Count", "2 Months Count", "3 Months Count", "Semester Count", "Year Count"});
    private final JLabel lblRange = new JLabel(" ");
    private final DefaultTableModel frozenModel = new DefaultTableModel(new String[]{"Student ID", "Student Name", "Present", "Absent", "Late", "Excuse"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel summaryModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable frozenTable = new JTable(frozenModel);
    private final JTable summaryTable = new JTable(summaryModel);
    private final JScrollPane summaryScrollPane = new JScrollPane(summaryTable);
    private final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
    private final JButton btnThisWeek = new JButton("This Week");
    private final JButton btnThisMonth = new JButton("This Month");
    private final JButton btnViewRemarks = new JButton("View Selected Remarks");
    private final Map<String, String> remarksByCell = new HashMap<>();

    public AttendanceSummaryWindow(Connection con) {
        // this window reads attendance records from the shared mysql connection
        this.con = con;
        AppTheme.install();

        setTitle("Attendance Summary");
        setSize(1500, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        AppTheme.stylePanel(content);
        content.setBorder(BorderFactory.createEmptyBorder(WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN));
        setContentPane(content);

        content.add(buildTopPanel(), BorderLayout.NORTH);
        content.add(buildSummaryTables(), BorderLayout.CENTER);

        txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
        configureTables();
        wireResponsiveness();
        loadSummary();
        SwingUtilities.invokeLater(this::updateButtonVisibility);
    }

    private JPanel buildTopPanel() {
        // this panel holds report controls for period date subject search and quick date actions
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        AppTheme.stylePanel(panel);

        AppTheme.stylePanel(controls);
        controls.setLayout(new BorderLayout(8, 6));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        AppTheme.stylePanel(filterRow);
        filterRow.add(new JLabel("Period:"));
        filterRow.add(cmbPeriod);
        filterRow.add(new JLabel("Count:"));
        filterRow.add(cmbCountRange);
        filterRow.add(new JLabel("Anchor Date:"));
        filterRow.add(txtAnchorDate);
        filterRow.add(new JLabel("Subject:"));
        filterRow.add(cmbSubject);
        filterRow.add(new JLabel("Search:"));
        filterRow.add(txtSearch);

        JButton btnLoad = new JButton("Load Summary");
        btnLoad.addActionListener(e -> loadSummary());
        AppTheme.styleButton(btnLoad);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        AppTheme.stylePanel(actionRow);
        actionRow.add(btnLoad);

        btnThisWeek.addActionListener(e -> {
            txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
            cmbPeriod.setSelectedItem("Weekly");
            loadSummary();
        });
        AppTheme.styleButton(btnThisWeek);
        actionRow.add(btnThisWeek);

        btnThisMonth.addActionListener(e -> {
            txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
            cmbPeriod.setSelectedItem("Monthly");
            loadSummary();
        });
        AppTheme.styleButton(btnThisMonth);
        actionRow.add(btnThisMonth);

        AppTheme.styleButton(btnViewRemarks);
        btnViewRemarks.addActionListener(e -> showSelectedRemarks());
        actionRow.add(btnViewRemarks);

        controls.add(filterRow, BorderLayout.NORTH);
        controls.add(actionRow, BorderLayout.SOUTH);

        AppTheme.styleInput(txtAnchorDate);
        AppTheme.styleInput(txtSearch);
        AppTheme.styleInput(cmbSubject);
        AppTheme.styleInput(cmbPeriod);
        AppTheme.styleInput(cmbCountRange);
        lblRange.setFont(lblRange.getFont().deriveFont(Font.BOLD));
        AppTheme.styleTitle(lblRange);
        panel.add(controls, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel(new BorderLayout(10, 6));
        AppTheme.stylePanel(infoPanel);
        infoPanel.add(buildLegendPanel(), BorderLayout.CENTER);
        infoPanel.add(lblRange, BorderLayout.SOUTH);
        panel.add(infoPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildLegendPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        AppTheme.stylePanel(legend);
        legend.add(new JLabel("Legend:"));
        legend.add(createLegendLabel("P = Present", StatusColors.PRESENT_BG, StatusColors.PRESENT_FG));
        legend.add(createLegendLabel("A = Absent", StatusColors.ABSENT_BG, StatusColors.ABSENT_FG));
        legend.add(createLegendLabel("L = Late", StatusColors.LATE_BG, StatusColors.LATE_FG));
        legend.add(createLegendLabel("E = Excuse", StatusColors.EXCUSE_BG, StatusColors.EXCUSE_FG));
        legend.add(new JLabel("Double-click A/E cells or use the top remarks button."));
        return legend;
    }

    private JComponent buildSummaryTables() {
        // this uses the scroll pane row header so student names stay visible while dates scroll
        summaryScrollPane.setRowHeaderView(frozenTable);
        summaryScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, frozenTable.getTableHeader());
        return summaryScrollPane;
    }

    private JLabel createLegendLabel(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(foreground),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return label;
    }

    private void configureTables() {
        // this prepares the summary tables for read only report viewing and custom status colors
        summaryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        summaryTable.setRowHeight(30);
        summaryTable.setDefaultRenderer(Object.class, new SummaryCellRenderer(0));
        AppTheme.styleTable(summaryTable);

        frozenTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        frozenTable.setRowHeight(30);
        frozenTable.setSelectionModel(summaryTable.getSelectionModel());
        frozenTable.setFocusable(false);
        frozenTable.setDefaultRenderer(Object.class, new SummaryCellRenderer(2));
        AppTheme.styleTable(frozenTable);
        AppTheme.styleScrollPane(summaryScrollPane);
        summaryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedRemarks();
                }
            }
        });
    }

    private void wireResponsiveness() {
        // this listener hides less important controls on smaller window widths
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateButtonVisibility();
            }
        });
    }

    private void updateButtonVisibility() {
        btnThisMonth.setVisible(getWidth() >= SMALL_WIDTH_BREAKPOINT);
        controls.revalidate();
        controls.repaint();
    }

    private void loadSummary() {
        // this method calculates the report date range then builds columns and rows dynamically
        LocalDate anchorDate;
        try {
            anchorDate = LocalDate.parse(txtAnchorDate.getText().trim(), CRUD_GUI.DATE_FORMATTER);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Anchor date must be in YYYY-MM-DD format.");
            return;
        }

        boolean monthly = "Monthly".equals(cmbPeriod.getSelectedItem());
        // weekly reports begin on monday while monthly reports cover the full calendar month
        LocalDate startDate = monthly ? anchorDate.withDayOfMonth(1) : anchorDate.with(DayOfWeek.MONDAY);
        LocalDate endDate = monthly ? anchorDate.withDayOfMonth(anchorDate.lengthOfMonth()) : startDate.plusDays(6);
        LocalDate[] countRange = resolveCountRange(anchorDate, startDate, endDate);
        LocalDate countStartDate = countRange[0];
        LocalDate countEndDate = countRange[1];
        boolean separateCountRange = !countStartDate.equals(startDate) || !countEndDate.equals(endDate);
        lblRange.setText("Showing " + startDate + " to " + endDate + " | Counts " + countStartDate + " to " + countEndDate);

        try {
            List<LocalDate> rangeDates = new ArrayList<>();
            // this date list becomes the dynamic columns of the report table
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                rangeDates.add(date);
            }
            boolean subjectFiltered = cmbSubject.getSelectedIndex() > 0;

            StringBuilder sql = new StringBuilder(
                    "SELECT student_identifier, student_name, attendance_date, attendance_status, remarks, 'DISPLAY' AS row_source " +
                            "FROM attendance_records WHERE attendance_date BETWEEN ? AND ?"
            );
            List<Object> params = new ArrayList<>();
            params.add(Date.valueOf(startDate));
            params.add(Date.valueOf(endDate));
            String sharedFilterSql = buildSharedFilterSql(params);

            sql.append(sharedFilterSql);
            if (separateCountRange) {
                sql.append(" UNION ALL ");
                sql.append(
                        "SELECT student_identifier, student_name, attendance_date, attendance_status, remarks, 'COUNT' AS row_source " +
                                "FROM attendance_records WHERE attendance_date BETWEEN ? AND ?"
                );
                params.add(Date.valueOf(countStartDate));
                params.add(Date.valueOf(countEndDate));
                sql.append(buildSharedFilterSql(params));
            }
            sql.append(" ORDER BY student_name, attendance_date");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof Date) {
                    pst.setDate(i + 1, (Date) value);
                } else {
                    pst.setString(i + 1, value.toString());
                }
            }

            ResultSet rs = pst.executeQuery();
            Map<String, StudentSummaryRow> rows = new LinkedHashMap<>();
            List<LocalDate> filteredDates = new ArrayList<>();
            // linked hash map keeps each student on one row while preserving the query order
            while (rs.next()) {
                String studentId = rs.getString("student_identifier");
                String studentName = rs.getString("student_name");
                LocalDate attendanceDate = rs.getDate("attendance_date").toLocalDate();
                String rowSource = rs.getString("row_source");
                String key = studentId + "||" + studentName;

                StudentSummaryRow row = rows.computeIfAbsent(key, ignored -> new StudentSummaryRow(studentId, studentName));
                String status = rs.getString("attendance_status");
                if ("DISPLAY".equals(rowSource)) {
                    row.addDisplayStatus(attendanceDate, status, rs.getString("remarks"));
                }
                if (!separateCountRange || "COUNT".equals(rowSource)) {
                    row.addCountStatus(status);
                }
                if ("DISPLAY".equals(rowSource) && subjectFiltered && !filteredDates.contains(attendanceDate)) {
                    filteredDates.add(attendanceDate);
                }
            }

            List<LocalDate> dates = subjectFiltered ? filteredDates : rangeDates;
            frozenModel.setRowCount(0);
            summaryModel.setRowCount(0);
            frozenModel.setColumnCount(0);
            summaryModel.setColumnCount(0);
            frozenModel.addColumn("Student ID");
            frozenModel.addColumn("Student Name");
            frozenModel.addColumn("Present");
            frozenModel.addColumn("Absent");
            frozenModel.addColumn("Late");
            frozenModel.addColumn("Excuse");
            summaryModel.setColumnCount(0);
            remarksByCell.clear();
            for (LocalDate date : dates) {
                String header = date.getDayOfMonth() + " " + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                summaryModel.addColumn(header);
            }

            int rowIndex = 0;
            for (StudentSummaryRow row : rows.values()) {
                frozenModel.addRow(new Object[]{row.studentId, row.studentName, row.present, row.absent, row.late, row.excuse});
                Object[] dateCells = new Object[dates.size()];
                for (int i = 0; i < dates.size(); i++) {
                    LocalDate date = dates.get(i);
                    dateCells[i] = row.statusesByDate.getOrDefault(date, "");
                    String remarks = row.remarksByDate.get(date);
                    if (remarks != null && !remarks.trim().isEmpty()) {
                        remarksByCell.put(cellKey(rowIndex, i), remarks);
                    }
                }
                summaryModel.addRow(dateCells);
                rowIndex++;
            }

            configureColumnWidths();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load attendance summary: " + e.getMessage());
        }
    }

    private String buildSharedFilterSql(List<Object> params) {
        StringBuilder sql = new StringBuilder();
        if (cmbSubject.getSelectedIndex() > 0) {
            sql.append(" AND subject_name = ?");
            params.add(cmbSubject.getSelectedItem().toString());
        }
        // these optional filters narrow the report without needing separate queries
        if (!txtSearch.getText().trim().isEmpty()) {
            sql.append(" AND (student_identifier LIKE ? OR student_name LIKE ?)");
            String keyword = "%" + txtSearch.getText().trim() + "%";
            params.add(keyword);
            params.add(keyword);
        }
        return sql.toString();
    }

    private LocalDate[] resolveCountRange(LocalDate anchorDate, LocalDate viewStartDate, LocalDate viewEndDate) {
        String selected = cmbCountRange.getSelectedItem() == null ? "Current View Count" : cmbCountRange.getSelectedItem().toString();
        if ("1 Month Count".equals(selected)) {
            return new LocalDate[]{anchorDate.withDayOfMonth(1), anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())};
        }
        if ("2 Months Count".equals(selected)) {
            LocalDate start = anchorDate.minusMonths(1).withDayOfMonth(1);
            return new LocalDate[]{start, anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())};
        }
        if ("3 Months Count".equals(selected)) {
            LocalDate start = anchorDate.minusMonths(2).withDayOfMonth(1);
            return new LocalDate[]{start, anchorDate.withDayOfMonth(anchorDate.lengthOfMonth())};
        }
        if ("Semester Count".equals(selected)) {
            int month = anchorDate.getMonthValue();
            LocalDate start = month <= 6 ? anchorDate.withMonth(1).withDayOfMonth(1) : anchorDate.withMonth(7).withDayOfMonth(1);
            LocalDate end = month <= 6 ? anchorDate.withMonth(6).withDayOfMonth(30) : anchorDate.withMonth(12).withDayOfMonth(31);
            return new LocalDate[]{start, end};
        }
        if ("Year Count".equals(selected)) {
            return new LocalDate[]{anchorDate.withDayOfYear(1), anchorDate.withMonth(12).withDayOfMonth(31)};
        }
        return new LocalDate[]{viewStartDate, viewEndDate};
    }

    private void configureColumnWidths() {
        // fixed column widths keep the report readable even when many dates are displayed
        if (frozenTable.getColumnModel().getColumnCount() < 2) {
            return;
        }
        frozenTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        frozenTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        for (int i = 2; i < frozenTable.getColumnModel().getColumnCount(); i++) {
            frozenTable.getColumnModel().getColumn(i).setPreferredWidth(75);
        }
        frozenTable.setPreferredScrollableViewportSize(new Dimension(620, frozenTable.getPreferredSize().height));
        for (int i = 0; i < summaryTable.getColumnModel().getColumnCount(); i++) {
            summaryTable.getColumnModel().getColumn(i).setPreferredWidth(75);
        }
    }

    private String statusSymbol(String status) {
        // these compact symbols make the weekly and monthly report easier to scan
        if ("Present".equalsIgnoreCase(status)) {
            return "P";
        }
        if ("Absent".equalsIgnoreCase(status)) {
            return "A";
        }
        if ("Late".equalsIgnoreCase(status)) {
            return "L";
        }
        if ("Excuse".equalsIgnoreCase(status)) {
            return "E";
        }
        return "";
    }

    private void showSelectedRemarks() {
        int selectedRow = summaryTable.getSelectedRow();
        int selectedColumn = summaryTable.getSelectedColumn();
        if (selectedRow < 0 || selectedColumn < 0) {
            JOptionPane.showMessageDialog(this, "Select an Absent or Excuse date cell first.");
            return;
        }
        String cellText = String.valueOf(summaryTable.getValueAt(selectedRow, selectedColumn));
        if (!cellText.contains("A") && !cellText.contains("E")) {
            JOptionPane.showMessageDialog(this, "Remarks are shown for Absent or Excuse cells.");
            return;
        }
        String remarks = remarksByCell.get(cellKey(selectedRow, selectedColumn));
        if (remarks == null || remarks.trim().isEmpty()) {
            remarks = "No remarks recorded.";
        }
        String studentName = String.valueOf(frozenModel.getValueAt(selectedRow, 1));
        String dateHeader = summaryTable.getColumnName(selectedColumn);
        JOptionPane.showMessageDialog(this, remarks, "Remarks - " + studentName + " - " + dateHeader, JOptionPane.INFORMATION_MESSAGE);
    }

    private String cellKey(int row, int column) {
        return row + ":" + column;
    }

    private static String[] prependAll(String[] values) {
        // this creates subject filter choices with all as the first option
        String[] result = new String[values.length + 1];
        result[0] = "All";
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private static class SummaryCellRenderer extends DefaultTableCellRenderer {
        // this renderer colors status symbols without changing the stored table values
        private final int frozenColumns;

        private SummaryCellRenderer(int frozenColumns) {
            this.frozenColumns = frozenColumns;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // this method runs for each visible cell and decides alignment color and selection style
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(column < frozenColumns ? LEFT : CENTER);

            if (!isSelected) {
                component.setBackground(Color.WHITE);
                component.setForeground(Color.BLACK);
                if (column >= frozenColumns) {
                    String text = value == null ? "" : value.toString();
                    if (table.getColumnCount() >= 6 && column >= 2 && table.getColumnName(column).matches("Present|Absent|Late|Excuse")) {
                        applyCountColumnStyle(component, column);
                    } else {
                        applyStatusStyle(component, text);
                    }
                }
            }
            return component;
        }

        private void applyCountColumnStyle(Component component, int column) {
            if (column == 2) {
                component.setBackground(StatusColors.PRESENT_BG);
                component.setForeground(StatusColors.PRESENT_FG);
            } else if (column == 3) {
                component.setBackground(StatusColors.ABSENT_BG);
                component.setForeground(StatusColors.ABSENT_FG);
            } else if (column == 4) {
                component.setBackground(StatusColors.LATE_BG);
                component.setForeground(StatusColors.LATE_FG);
            } else if (column == 5) {
                component.setBackground(StatusColors.EXCUSE_BG);
                component.setForeground(StatusColors.EXCUSE_FG);
            }
        }

        private void applyStatusStyle(Component component, String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            if (text.contains("A")) {
                component.setBackground(StatusColors.ABSENT_BG);
                component.setForeground(StatusColors.ABSENT_FG);
            } else if (text.contains("E")) {
                component.setBackground(StatusColors.EXCUSE_BG);
                component.setForeground(StatusColors.EXCUSE_FG);
            } else if (text.contains("L")) {
                component.setBackground(StatusColors.LATE_BG);
                component.setForeground(StatusColors.LATE_FG);
            } else if (text.contains("P")) {
                component.setBackground(StatusColors.PRESENT_BG);
                component.setForeground(StatusColors.PRESENT_FG);
            }
        }
    }

    private static class StatusColors {
        private static final Color PRESENT_BG = new Color(224, 245, 229);
        private static final Color PRESENT_FG = new Color(20, 110, 45);
        private static final Color ABSENT_BG = new Color(252, 226, 226);
        private static final Color ABSENT_FG = new Color(165, 35, 35);
        private static final Color LATE_BG = new Color(255, 241, 205);
        private static final Color LATE_FG = new Color(150, 95, 0);
        private static final Color EXCUSE_BG = new Color(232, 231, 250);
        private static final Color EXCUSE_FG = new Color(75, 70, 145);
    }

    private static class StudentSummaryRow {
        private final String studentId;
        private final String studentName;
        private final Map<LocalDate, String> statusesByDate = new LinkedHashMap<>();
        private final Map<LocalDate, String> remarksByDate = new LinkedHashMap<>();
        private int present;
        private int absent;
        private int late;
        private int excuse;

        private StudentSummaryRow(String studentId, String studentName) {
            this.studentId = studentId;
            this.studentName = studentName;
        }

        private void addDisplayStatus(LocalDate date, String status, String remarks) {
            String symbol = symbolFor(status);
            String existing = statusesByDate.get(date);
            if (existing == null || existing.isEmpty()) {
                statusesByDate.put(date, symbol);
            } else if (!existing.contains(symbol)) {
                statusesByDate.put(date, existing + "," + symbol);
            }

            if (remarks != null && !remarks.trim().isEmpty() && ("A".equals(symbol) || "E".equals(symbol))) {
                String existingRemarks = remarksByDate.get(date);
                remarksByDate.put(date, existingRemarks == null || existingRemarks.isEmpty()
                        ? remarks
                        : existingRemarks + "\n" + remarks);
            }

        }

        private void addCountStatus(String status) {
            if ("Present".equalsIgnoreCase(status)) {
                present++;
            } else if ("Absent".equalsIgnoreCase(status)) {
                absent++;
            } else if ("Late".equalsIgnoreCase(status)) {
                late++;
            } else if ("Excuse".equalsIgnoreCase(status)) {
                excuse++;
            }
        }

        private static String symbolFor(String status) {
            if ("Present".equalsIgnoreCase(status)) {
                return "P";
            }
            if ("Absent".equalsIgnoreCase(status)) {
                return "A";
            }
            if ("Late".equalsIgnoreCase(status)) {
                return "L";
            }
            if ("Excuse".equalsIgnoreCase(status)) {
                return "E";
            }
            return "";
        }
    }
}
