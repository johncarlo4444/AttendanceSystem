import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TeacherDashboard extends JFrame {

    private final Connection con;

    private final DefaultListModel<String> dateModel = new DefaultListModel<>();
    private final DefaultListModel<String> studentModel = new DefaultListModel<>();
    private final JList<String> dateList = new JList<>(dateModel);
    private final JList<String> studentList = new JList<>(studentModel);
    private final JTextField txtSearch = new JTextField();
    private final JComboBox<String> cmbStatus = new JComboBox<>(prependAll(CRUD_GUI.ATTENDANCE_OPTIONS));
    private final JComboBox<String> cmbSubject = new JComboBox<>(prependAll(CRUD_GUI.SUBJECTS));
    private final JComboBox<String> cmbTimeState = new JComboBox<>(new String[]{"All", "Timed In Only", "Timed Out"});
    private final DefaultTableModel summaryModel = new DefaultTableModel(
            new String[]{"Student ID", "Student Name", "Subject", "Status", "Time In", "Time Out", "Remarks"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable summaryTable = new JTable(summaryModel);
    private final JTree detailTree = new JTree(new DefaultMutableTreeNode("Select a student"));

    public TeacherDashboard(Connection con) {
        this.con = con;

        setTitle("Teacher Attendance Dashboard");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(1, 3, 12, 12));

        add(buildDatesPanel());
        add(buildStudentsPanel());
        add(buildFilterPanel());

        wireEvents();
        loadDates();
    }

    private JPanel buildDatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Dates"));
        dateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(dateList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Students"));

        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(studentList),
                new JScrollPane(detailTree)
        );
        splitPane.setResizeWeight(0.45);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Filters and Summary"));

        JPanel filters = new JPanel(new GridLayout(0, 2, 8, 8));
        filters.add(new JLabel("Search:"));
        filters.add(txtSearch);
        filters.add(new JLabel("Status:"));
        filters.add(cmbStatus);
        filters.add(new JLabel("Subject:"));
        filters.add(cmbSubject);
        filters.add(new JLabel("Time State:"));
        filters.add(cmbTimeState);

        JButton btnApply = new JButton("Apply");
        btnApply.addActionListener(e -> refreshForFilters());
        JButton btnClear = new JButton("Clear");
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbStatus.setSelectedIndex(0);
            cmbSubject.setSelectedIndex(0);
            cmbTimeState.setSelectedIndex(0);
            refreshForFilters();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.add(btnApply);
        buttonPanel.add(btnClear);

        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.add(filters, BorderLayout.CENTER);
        north.add(buttonPanel, BorderLayout.SOUTH);

        summaryTable.setRowHeight(28);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(summaryTable), BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        dateList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshForFilters();
            }
        });

        studentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadStudentTree();
            }
        });
    }

    private void loadDates() {
        dateModel.clear();
        try {
            PreparedStatement pst = con.prepareStatement(
                    "SELECT DISTINCT attendance_date FROM attendance_records ORDER BY attendance_date DESC"
            );
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                dateModel.addElement(rs.getDate("attendance_date").toString());
            }
            if (!dateModel.isEmpty()) {
                dateList.setSelectedIndex(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load dates: " + e.getMessage());
        }
    }

    private void refreshForFilters() {
        loadStudents();
        loadSummary();
        loadStudentTree();
    }

    private void loadStudents() {
        studentModel.clear();
        String selectedDate = dateList.getSelectedValue();
        if (selectedDate == null) {
            return;
        }

        try {
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    "SELECT DISTINCT student_name FROM attendance_records WHERE attendance_date = ?"
            );
            params.add(Date.valueOf(selectedDate));
            appendSharedFilters(sql, params, true);
            sql.append(" ORDER BY student_name");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            bindParameters(pst, params);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                studentModel.addElement(rs.getString("student_name"));
            }

            if (!studentModel.isEmpty()) {
                studentList.setSelectedIndex(0);
            } else {
                detailTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("No students found")));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load students: " + e.getMessage());
        }
    }

    private void loadSummary() {
        summaryModel.setRowCount(0);
        String selectedDate = dateList.getSelectedValue();
        if (selectedDate == null) {
            return;
        }

        try {
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    "SELECT student_identifier, student_name, subject_name, attendance_status, attendance_time, COALESCE(time_out, '') AS time_out, remarks " +
                            "FROM attendance_records WHERE attendance_date = ?"
            );
            params.add(Date.valueOf(selectedDate));
            appendSharedFilters(sql, params, true);
            sql.append(" ORDER BY student_name, subject_name");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            bindParameters(pst, params);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                summaryModel.addRow(new Object[]{
                        rs.getString("student_identifier"),
                        rs.getString("student_name"),
                        rs.getString("subject_name"),
                        rs.getString("attendance_status"),
                        rs.getString("attendance_time"),
                        rs.getString("time_out"),
                        rs.getString("remarks")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load summary: " + e.getMessage());
        }
    }

    private void loadStudentTree() {
        String selectedDate = dateList.getSelectedValue();
        String selectedStudent = studentList.getSelectedValue();
        if (selectedDate == null || selectedStudent == null) {
            detailTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Select a student")));
            return;
        }

        try {
            List<Object> params = new ArrayList<>();
            StringBuilder sql = new StringBuilder(
                    "SELECT subject_name, attendance_status, attendance_time, COALESCE(time_out, '') AS time_out, remarks " +
                            "FROM attendance_records WHERE attendance_date = ? AND student_name = ?"
            );
            params.add(Date.valueOf(selectedDate));
            params.add(selectedStudent);
            appendSharedFilters(sql, params, false);
            sql.append(" ORDER BY subject_name, attendance_time");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            bindParameters(pst, params);
            ResultSet rs = pst.executeQuery();

            DefaultMutableTreeNode root = new DefaultMutableTreeNode(selectedStudent);
            int count = 0;
            while (rs.next()) {
                count++;
                DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(
                        rs.getString("subject_name") + " - " + rs.getString("attendance_status")
                );
                entryNode.add(new DefaultMutableTreeNode("Time In: " + rs.getString("attendance_time")));
                entryNode.add(new DefaultMutableTreeNode("Time Out: " + rs.getString("time_out")));
                entryNode.add(new DefaultMutableTreeNode("Remarks: " + safe(rs.getString("remarks"))));
                root.add(entryNode);
            }

            root.insert(new DefaultMutableTreeNode("Total inserts: " + count), 0);
            detailTree.setModel(new DefaultTreeModel(root));
            expandAll(detailTree);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load student details: " + e.getMessage());
        }
    }

    private void appendSharedFilters(StringBuilder sql, List<Object> params, boolean includeSearch) {
        if (includeSearch && !txtSearch.getText().trim().isEmpty()) {
            sql.append(" AND (student_name LIKE ? OR student_identifier LIKE ?)");
            String keyword = "%" + txtSearch.getText().trim() + "%";
            params.add(keyword);
            params.add(keyword);
        }
        if (cmbStatus.getSelectedIndex() > 0) {
            sql.append(" AND attendance_status = ?");
            params.add(cmbStatus.getSelectedItem().toString());
        }
        if (cmbSubject.getSelectedIndex() > 0) {
            sql.append(" AND subject_name = ?");
            params.add(cmbSubject.getSelectedItem().toString());
        }

        String timeState = cmbTimeState.getSelectedItem().toString();
        if ("Timed In Only".equals(timeState)) {
            sql.append(" AND attendance_time <> '---' AND (time_out IS NULL OR TRIM(time_out) = '')");
        } else if ("Timed Out".equals(timeState)) {
            sql.append(" AND time_out IS NOT NULL AND TRIM(time_out) <> '' AND time_out <> '---'");
        }
    }

    private void bindParameters(PreparedStatement pst, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value instanceof Date) {
                pst.setDate(i + 1, (Date) value);
            } else {
                pst.setString(i + 1, value.toString());
            }
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private static String[] prependAll(String[] values) {
        String[] result = new String[values.length + 1];
        result[0] = "All";
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }
}
