import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceSummaryWindow extends JFrame {

    private static final int WINDOW_MARGIN = 18;
    private static final int SMALL_WIDTH_BREAKPOINT = 1100;

    private final Connection con;
    private final JTextField txtAnchorDate = new JTextField(10);
    private final JTextField txtSearch = new JTextField(12);
    private final JComboBox<String> cmbSubject = new JComboBox<>(prependAll(CRUD_GUI.SUBJECTS));
    private final JComboBox<String> cmbPeriod = new JComboBox<>(new String[]{"Weekly", "Monthly"});
    private final JLabel lblRange = new JLabel(" ");
    private final DefaultTableModel frozenModel = new DefaultTableModel(new String[]{"Student ID", "Student Name"}, 0) {
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

    public AttendanceSummaryWindow(Connection con) {
        this.con = con;
        AppTheme.install();

        setTitle("Attendance Summary");
        setSize(1500, 800);
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
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        AppTheme.stylePanel(panel);

        AppTheme.stylePanel(controls);
        controls.add(new JLabel("Period:"));
        controls.add(cmbPeriod);
        controls.add(new JLabel("Anchor Date:"));
        controls.add(txtAnchorDate);
        controls.add(new JLabel("Subject:"));
        controls.add(cmbSubject);
        controls.add(new JLabel("Search:"));
        controls.add(txtSearch);

        JButton btnLoad = new JButton("Load Summary");
        btnLoad.addActionListener(e -> loadSummary());
        AppTheme.styleButton(btnLoad);
        controls.add(btnLoad);

        btnThisWeek.addActionListener(e -> {
            txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
            cmbPeriod.setSelectedItem("Weekly");
            loadSummary();
        });
        AppTheme.styleButton(btnThisWeek);
        controls.add(btnThisWeek);

        btnThisMonth.addActionListener(e -> {
            txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
            cmbPeriod.setSelectedItem("Monthly");
            loadSummary();
        });
        AppTheme.styleButton(btnThisMonth);
        controls.add(btnThisMonth);

        AppTheme.styleInput(txtAnchorDate);
        AppTheme.styleInput(txtSearch);
        AppTheme.styleInput(cmbSubject);
        AppTheme.styleInput(cmbPeriod);
        lblRange.setFont(lblRange.getFont().deriveFont(Font.BOLD));
        AppTheme.styleTitle(lblRange);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(lblRange, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildSummaryTables() {
        summaryScrollPane.setRowHeaderView(frozenTable);
        summaryScrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, frozenTable.getTableHeader());
        return summaryScrollPane;
    }

    private void configureTables() {
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
    }

    private void wireResponsiveness() {
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
        LocalDate anchorDate;
        try {
            anchorDate = LocalDate.parse(txtAnchorDate.getText().trim(), CRUD_GUI.DATE_FORMATTER);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Anchor date must be in YYYY-MM-DD format.");
            return;
        }

        boolean monthly = "Monthly".equals(cmbPeriod.getSelectedItem());
        LocalDate startDate = monthly ? anchorDate.withDayOfMonth(1) : anchorDate.with(DayOfWeek.MONDAY);
        LocalDate endDate = monthly ? anchorDate.withDayOfMonth(anchorDate.lengthOfMonth()) : startDate.plusDays(6);
        lblRange.setText("Showing " + startDate + " to " + endDate);

        try {
            List<LocalDate> dates = new ArrayList<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dates.add(date);
            }

            frozenModel.setRowCount(0);
            summaryModel.setRowCount(0);
            frozenModel.setColumnCount(0);
            summaryModel.setColumnCount(0);
            frozenModel.addColumn("Student ID");
            frozenModel.addColumn("Student Name");
            for (LocalDate date : dates) {
                String header = date.getDayOfMonth() + " " + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                summaryModel.addColumn(header);
            }

            StringBuilder sql = new StringBuilder(
                    "SELECT student_identifier, student_name, attendance_date, attendance_status " +
                            "FROM attendance_records WHERE attendance_date BETWEEN ? AND ?"
            );
            List<Object> params = new ArrayList<>();
            params.add(Date.valueOf(startDate));
            params.add(Date.valueOf(endDate));

            if (cmbSubject.getSelectedIndex() > 0) {
                sql.append(" AND subject_name = ?");
                params.add(cmbSubject.getSelectedItem().toString());
            }
            if (!txtSearch.getText().trim().isEmpty()) {
                sql.append(" AND (student_identifier LIKE ? OR student_name LIKE ?)");
                String keyword = "%" + txtSearch.getText().trim() + "%";
                params.add(keyword);
                params.add(keyword);
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
            Map<String, String[]> rows = new LinkedHashMap<>();
            while (rs.next()) {
                String studentId = rs.getString("student_identifier");
                String studentName = rs.getString("student_name");
                LocalDate attendanceDate = rs.getDate("attendance_date").toLocalDate();
                String key = studentId + "||" + studentName;

                String[] row = rows.computeIfAbsent(key, ignored -> {
                    String[] values = new String[2 + dates.size()];
                    values[0] = studentId;
                    values[1] = studentName;
                    for (int col = 2; col < values.length; col++) {
                        values[col] = "";
                    }
                    return values;
                });

                int dateIndex = dates.indexOf(attendanceDate);
                if (dateIndex >= 0) {
                    row[2 + dateIndex] = statusSymbol(rs.getString("attendance_status"));
                }
            }

            for (String[] row : rows.values()) {
                frozenModel.addRow(new Object[]{row[0], row[1]});
                Object[] dateCells = new Object[dates.size()];
                System.arraycopy(row, 2, dateCells, 0, dateCells.length);
                summaryModel.addRow(dateCells);
            }

            configureColumnWidths();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load attendance summary: " + e.getMessage());
        }
    }

    private void configureColumnWidths() {
        if (frozenTable.getColumnModel().getColumnCount() < 2) {
            return;
        }
        frozenTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        frozenTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        frozenTable.setPreferredScrollableViewportSize(new Dimension(320, frozenTable.getPreferredSize().height));
        for (int i = 0; i < summaryTable.getColumnModel().getColumnCount(); i++) {
            summaryTable.getColumnModel().getColumn(i).setPreferredWidth(75);
        }
    }

    private String statusSymbol(String status) {
        if ("Present".equalsIgnoreCase(status)) {
            return "P";
        }
        if ("Absent".equalsIgnoreCase(status)) {
            return "A";
        }
        if ("Late".equalsIgnoreCase(status)) {
            return "LP";
        }
        if ("Excuse".equalsIgnoreCase(status)) {
            return "E";
        }
        return "";
    }

    private static String[] prependAll(String[] values) {
        String[] result = new String[values.length + 1];
        result[0] = "All";
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private static class SummaryCellRenderer extends DefaultTableCellRenderer {
        private final int frozenColumns;

        private SummaryCellRenderer(int frozenColumns) {
            this.frozenColumns = frozenColumns;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(column < frozenColumns ? LEFT : CENTER);

            if (!isSelected) {
                component.setBackground(Color.WHITE);
                component.setForeground(Color.BLACK);
                if (column >= frozenColumns) {
                    String text = value == null ? "" : value.toString();
                    if ("P".equals(text)) {
                        component.setForeground(new Color(0, 128, 0));
                    } else if ("A".equals(text)) {
                        component.setForeground(Color.RED.darker());
                    } else if ("LP".equals(text)) {
                        component.setForeground(new Color(180, 120, 0));
                    } else if ("E".equals(text)) {
                        component.setForeground(new Color(90, 90, 160));
                    }
                }
            }
            return component;
        }
    }
}
