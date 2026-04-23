import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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

    private final Connection con;
    private final JTextField txtAnchorDate = new JTextField(10);
    private final JTextField txtSearch = new JTextField(12);
    private final JComboBox<String> cmbSubject = new JComboBox<>(prependAll(CRUD_GUI.SUBJECTS));
    private final JComboBox<String> cmbPeriod = new JComboBox<>(new String[]{"Weekly", "Monthly"});
    private final JLabel lblRange = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public AttendanceSummaryWindow(Connection con) {
        this.con = con;

        setTitle("Attendance Summary");
        setSize(1500, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(30);
        table.setDefaultRenderer(Object.class, new SummaryCellRenderer());

        loadSummary();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
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
        controls.add(btnLoad);

        JButton btnThisWeek = new JButton("This Week");
        btnThisWeek.addActionListener(e -> {
            txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
            cmbPeriod.setSelectedItem("Weekly");
            loadSummary();
        });
        controls.add(btnThisWeek);

        JButton btnThisMonth = new JButton("This Month");
        btnThisMonth.addActionListener(e -> {
            txtAnchorDate.setText(LocalDate.now().format(CRUD_GUI.DATE_FORMATTER));
            cmbPeriod.setSelectedItem("Monthly");
            loadSummary();
        });
        controls.add(btnThisMonth);

        lblRange.setFont(lblRange.getFont().deriveFont(Font.BOLD));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(lblRange, BorderLayout.SOUTH);
        return panel;
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

            model.setRowCount(0);
            model.setColumnCount(0);
            model.addColumn("Student ID");
            model.addColumn("Student Name");
            for (LocalDate date : dates) {
                String header = date.getDayOfMonth() + " " + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                model.addColumn(header);
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
                model.addRow(row);
            }

            configureColumnWidths();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load attendance summary: " + e.getMessage());
        }
    }

    private void configureColumnWidths() {
        if (table.getColumnModel().getColumnCount() < 2) {
            return;
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        for (int i = 2; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(75);
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
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(column < 2 ? LEFT : CENTER);

            if (!isSelected) {
                component.setBackground(Color.WHITE);
                component.setForeground(Color.BLACK);
                if (column >= 2) {
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
