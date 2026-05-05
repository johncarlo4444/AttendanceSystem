import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeacherDashboard extends JFrame {

    private static final int WINDOW_MARGIN = 18;

    // these components form the teacher view with schedules students filters details and records
    private final Connection con;
    private final JTree scheduleTree = new JTree(new DefaultMutableTreeNode("Schedules"));
    private final DefaultListModel<String> studentModel = new DefaultListModel<>();
    private final JList<String> studentList = new JList<>(studentModel);
    private final JTextField txtSearch = new JTextField();
    private final JComboBox<String> cmbStatus = new JComboBox<>(prependAll(CRUD_GUI.ATTENDANCE_OPTIONS));
    private final JComboBox<String> cmbSubject = new JComboBox<>(prependAll(CRUD_GUI.SUBJECTS));
    private final JComboBox<String> cmbTimeState = new JComboBox<>(new String[]{"All", "Timed In Only", "Timed Out"});
    private final JTree detailTree = new JTree(new DefaultMutableTreeNode("Select a student"));

    private String selectedDate;

    public TeacherDashboard(Connection con) {
        // this window reuses the active mysql connection from the main attendance form
        this.con = con;
        AppTheme.install();

        setTitle("Teacher Attendance Dashboard");
        setSize(1400, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new GridLayout(1, 3, 12, 12));
        AppTheme.stylePanel(content);
        content.setBorder(BorderFactory.createEmptyBorder(WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN));
        setContentPane(content);

        content.add(buildSchedulePanel());
        content.add(buildStudentsPanel());
        content.add(buildSummaryPanel());

        wireEvents();
        loadScheduleTree();
    }

    private JPanel buildSchedulePanel() {
        // this panel lists attendance dates with their day names for quick schedule selection
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(panel);
        panel.setBorder(AppTheme.createSectionBorder("Schedules", WINDOW_MARGIN));
        scheduleTree.setRootVisible(true);
        JScrollPane scrollPane = new JScrollPane(scheduleTree);
        AppTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStudentsPanel() {
        // this panel displays students who have records for the selected schedule date
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(panel);
        panel.setBorder(AppTheme.createSectionBorder("Students", WINDOW_MARGIN));
        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(studentList);
        AppTheme.styleScrollPane(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummaryPanel() {
        // this panel combines filters and selected student details
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(panel);
        panel.setBorder(AppTheme.createSectionBorder("Filters and Details", WINDOW_MARGIN));

        JPanel filters = new JPanel(new GridLayout(0, 2, 8, 8));
        AppTheme.stylePanel(filters);
        AppTheme.styleInput(txtSearch);
        AppTheme.styleInput(cmbStatus);
        AppTheme.styleInput(cmbSubject);
        AppTheme.styleInput(cmbTimeState);
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
        AppTheme.styleButton(btnApply);
        AppTheme.styleButton(btnClear);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbStatus.setSelectedIndex(0);
            cmbSubject.setSelectedIndex(0);
            cmbTimeState.setSelectedIndex(0);
            refreshForFilters();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        AppTheme.stylePanel(buttonPanel);
        buttonPanel.add(btnApply);
        buttonPanel.add(btnClear);

        JPanel filterWrapper = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(filterWrapper);
        filterWrapper.add(filters, BorderLayout.CENTER);
        filterWrapper.add(buttonPanel, BorderLayout.SOUTH);

        detailTree.setRootVisible(true);
        JScrollPane detailScrollPane = new JScrollPane(detailTree);
        AppTheme.styleScrollPane(detailScrollPane);

        panel.add(filterWrapper, BorderLayout.NORTH);
        panel.add(detailScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        // these listeners refresh the dashboard whenever the teacher chooses a date or student
        scheduleTree.addTreeSelectionListener(this::handleScheduleSelection);
        scheduleTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = scheduleTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        if (scheduleTree.isExpanded(path)) {
                            scheduleTree.collapsePath(path);
                        } else {
                            scheduleTree.expandPath(path);
                        }
                    }
                }
            }
        });

        studentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadStudentTree();
            }
        });
    }

    private void handleScheduleSelection(TreeSelectionEvent event) {
        // this extracts the selected date object from the tree node and refreshes related data
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) scheduleTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        Object userObject = node.getUserObject();
        if (userObject instanceof DateSelection) {
            selectedDate = ((DateSelection) userObject).date;
        } else {
            selectedDate = null;
        }

        if (event.isAddedPath()) {
            refreshForFilters();
        }
    }

    private void loadScheduleTree() {
        // this query loads each unique attendance date and the node label adds the weekday name
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Schedules");
        try {
            PreparedStatement pst = con.prepareStatement(
                    "SELECT DISTINCT attendance_date FROM attendance_records ORDER BY attendance_date DESC"
            );
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String date = rs.getDate("attendance_date").toString();
                DefaultMutableTreeNode dateNode = new DefaultMutableTreeNode(new DateSelection(date));
                root.add(dateNode);
            }

            scheduleTree.setModel(new DefaultTreeModel(root));
            scheduleTree.expandRow(0);
            if (root.getChildCount() > 0) {
                DefaultMutableTreeNode firstDateNode = (DefaultMutableTreeNode) root.getChildAt(0);
                TreePath firstPath = new TreePath(firstDateNode.getPath());
                scheduleTree.setSelectionPath(firstPath);
                scheduleTree.expandPath(firstPath);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load dates: " + e.getMessage());
        }
    }

    private void refreshForFilters() {
        // this keeps the student list and detail tree synchronized with filters
        loadStudents();
        loadStudentTree();
    }

    private void loadStudents() {
        // this query finds students for one date and applies the same dashboard filters
        studentModel.clear();
        if (selectedDate == null) {
            detailTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Select a date")));
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

    private void loadStudentTree() {
        // this builds a nested detail view for the selected student by subject and attendance status
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
                entryNode.add(new DefaultMutableTreeNode("Time In: " + safe(rs.getString("attendance_time"))));
                entryNode.add(new DefaultMutableTreeNode("Time Out: " + safe(rs.getString("time_out"))));
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
        // this method appends optional where clauses and records the matching parameter values
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
            sql.append(" AND attendance_time IS NOT NULL AND TRIM(attendance_time) <> '' AND (time_out IS NULL OR TRIM(time_out) = '')");
        } else if ("Timed Out".equals(timeState)) {
            sql.append(" AND time_out IS NOT NULL AND TRIM(time_out) <> '' AND time_out <> '---'");
        }
    }

    private void bindParameters(PreparedStatement pst, List<Object> params) throws Exception {
        // this binds dates and strings in order so prepared statements match the generated sql
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
        // this creates combo box options that include all before the fixed choices
        String[] result = new String[values.length + 1];
        result[0] = "All";
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private static class DateSelection {
        // this object stores the real date value while showing a friendlier tree label
        private final String date;

        private DateSelection(String date) {
            this.date = date;
        }

        @Override
        public String toString() {
            // this label shows the date and day name without changing the database value
            LocalDate localDate = LocalDate.parse(date);
            String dayName = localDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            return date + " - " + dayName;
        }
    }
}
