import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentSubjectRecordsWindow extends JFrame {

    private static final int WINDOW_MARGIN = 18;
    private static final String ALL_SUBJECTS = "All Subjects";

    private final Connection con;
    private final JTextField txtSearch = new JTextField();
    private final JComboBox<String> cmbSubject = new JComboBox<>();
    private final DefaultTableModel studentModel = new DefaultTableModel(new String[]{"Student ID", "Student Name"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel recordModel = new DefaultTableModel(
            new String[]{"Subject", "Date", "Status", "Time In", "Time Out", "Remarks"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable studentTable = new JTable(studentModel);
    private final JTable recordTable = new JTable(recordModel);
    private final JLabel lblSelectedStudent = new JLabel("Select a student");
    private boolean updatingSubjectOptions;

    public StudentSubjectRecordsWindow(Connection con) {
        this.con = con;
        AppTheme.install();

        setTitle("Student Records by Subject");
        setSize(1400, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        AppTheme.stylePanel(content);
        content.setBorder(BorderFactory.createEmptyBorder(WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN));
        setContentPane(content);

        JLabel title = new JLabel("Student Records by Subject", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        AppTheme.styleTitle(title);
        content.add(title, BorderLayout.NORTH);
        content.add(buildBody(), BorderLayout.CENTER);

        configureTables();
        loadStudents();
    }

    private JComponent buildBody() {
        JPanel studentPanel = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(studentPanel);
        studentPanel.setBorder(AppTheme.createSectionBorder("Students", WINDOW_MARGIN));

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        AppTheme.stylePanel(searchPanel);
        AppTheme.styleInput(txtSearch);
        JButton btnSearch = new JButton("Search");
        JButton btnClear = new JButton("Clear");
        AppTheme.styleButton(btnSearch);
        AppTheme.styleButton(btnClear);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        AppTheme.stylePanel(buttons);
        buttons.add(btnSearch);
        buttons.add(btnClear);
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(buttons, BorderLayout.EAST);

        studentPanel.add(searchPanel, BorderLayout.NORTH);
        JScrollPane studentScroll = new JScrollPane(studentTable);
        AppTheme.styleScrollPane(studentScroll);
        studentPanel.add(studentScroll, BorderLayout.CENTER);

        JPanel recordsPanel = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(recordsPanel);
        recordsPanel.setBorder(AppTheme.createSectionBorder("Per Subject Records", WINDOW_MARGIN));
        lblSelectedStudent.setFont(lblSelectedStudent.getFont().deriveFont(Font.BOLD, 18f));
        AppTheme.styleTitle(lblSelectedStudent);

        JPanel recordHeader = new JPanel(new BorderLayout(8, 8));
        AppTheme.stylePanel(recordHeader);
        JPanel subjectPanel = new JPanel(new BorderLayout(8, 0));
        AppTheme.stylePanel(subjectPanel);
        AppTheme.styleInput(cmbSubject);
        subjectPanel.add(new JLabel("Subject:"), BorderLayout.WEST);
        subjectPanel.add(cmbSubject, BorderLayout.CENTER);
        recordHeader.add(lblSelectedStudent, BorderLayout.NORTH);
        recordHeader.add(subjectPanel, BorderLayout.SOUTH);
        recordsPanel.add(recordHeader, BorderLayout.NORTH);

        JScrollPane recordScroll = new JScrollPane(recordTable);
        AppTheme.styleScrollPane(recordScroll);
        recordsPanel.add(recordScroll, BorderLayout.CENTER);

        btnSearch.addActionListener(e -> loadStudents());
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            loadStudents();
        });
        txtSearch.addActionListener(e -> loadStudents());
        studentTable.getSelectionModel().addListSelectionListener(this::handleStudentSelection);
        cmbSubject.addActionListener(e -> {
            if (!updatingSubjectOptions) {
                reloadSelectedStudentRecords();
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, studentPanel, recordsPanel);
        splitPane.setResizeWeight(0.28);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);
        return splitPane;
    }

    private void configureTables() {
        studentTable.setRowHeight(30);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        studentTable.setAutoCreateRowSorter(true);
        recordTable.setRowHeight(30);
        recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        recordTable.setAutoCreateRowSorter(true);
        recordTable.setFillsViewportHeight(true);
        studentTable.setFillsViewportHeight(true);
        AppTheme.styleTable(studentTable);
        AppTheme.styleTable(recordTable);
    }

    private void loadStudents() {
        studentModel.setRowCount(0);
        recordModel.setRowCount(0);
        lblSelectedStudent.setText("Select a student");
        resetSubjectOptions();

        try {
            String search = txtSearch.getText().trim();
            StringBuilder sql = new StringBuilder(
                    "SELECT student_identifier, student_name FROM students WHERE TRIM(student_name) <> ''"
            );
            if (!search.isEmpty()) {
                sql.append(" AND (student_identifier LIKE ? OR student_name LIKE ?)");
            }
            sql.append(" ORDER BY student_name");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            if (!search.isEmpty()) {
                String keyword = "%" + search + "%";
                pst.setString(1, keyword);
                pst.setString(2, keyword);
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                studentModel.addRow(new Object[]{rs.getString("student_identifier"), rs.getString("student_name")});
            }
            if (studentModel.getRowCount() > 0) {
                studentTable.setRowSelectionInterval(0, 0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load students: " + e.getMessage());
        }
    }

    private void handleStudentSelection(ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || studentTable.getSelectedRow() < 0) {
            return;
        }
        int modelRow = studentTable.convertRowIndexToModel(studentTable.getSelectedRow());
        String studentId = String.valueOf(studentModel.getValueAt(modelRow, 0));
        String studentName = String.valueOf(studentModel.getValueAt(modelRow, 1));
        loadRecordsForStudent(studentId, studentName);
    }

    private void loadRecordsForStudent(String studentId, String studentName) {
        recordModel.setRowCount(0);
        lblSelectedStudent.setText(studentName + " (" + studentId + ")");
        loadSubjectOptionsForStudent(studentId);

        try {
            String selectedSubject = cmbSubject.getSelectedItem() == null ? ALL_SUBJECTS : cmbSubject.getSelectedItem().toString();
            StringBuilder sql = new StringBuilder(
                    "SELECT subject_name, attendance_date, attendance_status, attendance_time, COALESCE(time_out, '') AS time_out, remarks " +
                            "FROM attendance_records WHERE student_identifier = ?"
            );
            if (!ALL_SUBJECTS.equals(selectedSubject)) {
                sql.append(" AND subject_name = ?");
            }
            sql.append(" ORDER BY attendance_date DESC, id DESC");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            pst.setString(1, studentId);
            if (!ALL_SUBJECTS.equals(selectedSubject)) {
                pst.setString(2, selectedSubject);
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String subject = rs.getString("subject_name");
                recordModel.addRow(new Object[]{
                        subject,
                        rs.getDate("attendance_date"),
                        rs.getString("attendance_status"),
                        emptyAsDash(rs.getString("attendance_time")),
                        emptyAsDash(rs.getString("time_out")),
                        emptyAsDash(rs.getString("remarks"))
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load subject records: " + e.getMessage());
        }
    }

    private void loadSubjectOptionsForStudent(String studentId) {
        String previousSelection = cmbSubject.getSelectedItem() == null ? ALL_SUBJECTS : cmbSubject.getSelectedItem().toString();
        updatingSubjectOptions = true;
        cmbSubject.removeAllItems();
        cmbSubject.addItem(ALL_SUBJECTS);

        try {
            PreparedStatement pst = con.prepareStatement(
                    "SELECT DISTINCT subject_name FROM attendance_records WHERE student_identifier = ? ORDER BY subject_name"
            );
            pst.setString(1, studentId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                cmbSubject.addItem(rs.getString("subject_name"));
            }
            cmbSubject.setSelectedItem(previousSelection);
            if (cmbSubject.getSelectedIndex() < 0) {
                cmbSubject.setSelectedIndex(0);
            }
        } catch (Exception e) {
            cmbSubject.setSelectedIndex(0);
        } finally {
            updatingSubjectOptions = false;
        }
    }

    private void reloadSelectedStudentRecords() {
        if (studentTable.getSelectedRow() < 0) {
            return;
        }
        int modelRow = studentTable.convertRowIndexToModel(studentTable.getSelectedRow());
        String studentId = String.valueOf(studentModel.getValueAt(modelRow, 0));
        String studentName = String.valueOf(studentModel.getValueAt(modelRow, 1));
        loadRecordsForStudentWithoutReloadingSubjects(studentId, studentName);
    }

    private void loadRecordsForStudentWithoutReloadingSubjects(String studentId, String studentName) {
        recordModel.setRowCount(0);
        lblSelectedStudent.setText(studentName + " (" + studentId + ")");

        try {
            String selectedSubject = cmbSubject.getSelectedItem() == null ? ALL_SUBJECTS : cmbSubject.getSelectedItem().toString();
            StringBuilder sql = new StringBuilder(
                    "SELECT subject_name, attendance_date, attendance_status, attendance_time, COALESCE(time_out, '') AS time_out, remarks " +
                            "FROM attendance_records WHERE student_identifier = ?"
            );
            if (!ALL_SUBJECTS.equals(selectedSubject)) {
                sql.append(" AND subject_name = ?");
            }
            sql.append(" ORDER BY attendance_date DESC, id DESC");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            pst.setString(1, studentId);
            if (!ALL_SUBJECTS.equals(selectedSubject)) {
                pst.setString(2, selectedSubject);
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                recordModel.addRow(new Object[]{
                        rs.getString("subject_name"),
                        rs.getDate("attendance_date"),
                        rs.getString("attendance_status"),
                        emptyAsDash(rs.getString("attendance_time")),
                        emptyAsDash(rs.getString("time_out")),
                        emptyAsDash(rs.getString("remarks"))
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load subject records: " + e.getMessage());
        }
    }

    private void resetSubjectOptions() {
        updatingSubjectOptions = true;
        cmbSubject.removeAllItems();
        cmbSubject.addItem(ALL_SUBJECTS);
        updatingSubjectOptions = false;
    }

    private String emptyAsDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
