import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.regex.Pattern;

public class CRUD_GUI extends JFrame {

    // these formatters keep date and time values consistent for database saving display and reports
    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final int WINDOW_MARGIN = 18;
    static final int RESPONSIVE_BREAKPOINT = 980;

    JTextField txtId;
    JTextField txtStudentIdentifier;
    JTextField txtName;
    JTextField txtDate;
    JTextField txtRemarks;
    JTextField txtSearch;
    JTextField txtFilterDate;
    JComboBox<String> cmbAttendance;
    JComboBox<String> cmbSubjectEntry;
    JComboBox<String> cmbFilterAttendance;
    JComboBox<String> cmbFilterSubject;
    JTable table;
    DefaultTableModel model;

    LocalDate selectedDate = LocalDate.now();
    String selectedSubject = "General";

    static final String URL = "jdbc:mysql://localhost:3306/";
    static final String DB_NAME = "crud_gui_db";
    static final String USER = "root";
    static final String PASS = "Administrator.123";

    static final String LOGIN_USERNAME = "admin";
    static final String LOGIN_PASSWORD = "admin";

    // these arrays are reused by entry forms filters dashboard and summary windows
    static final String[] ATTENDANCE_OPTIONS = {"Present", "Absent", "Late", "Excuse"};
    static final String[] SUBJECTS = {
            "General / Events / All Day",
            "Object Oriented Programming",
            "CpE as a Discipline",
            "Calculus",
            "Physics",
            "AutoCAD",
            "Panitikang Filipino",
            "University and I" ,
            "NSTP - CWTS" ,
            "Physical Education"
    };

    Connection con;
    boolean databaseReady = false;

    private final JPanel responsiveBody = new JPanel(new BorderLayout());
    private final JPanel crudPanel = new JPanel(new BorderLayout(10, 10));
    private final JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
    private boolean stackedLayout;

    public CRUD_GUI() {
        // this constructor builds the main attendance window then prepares the mysql database
        AppTheme.install();
        setTitle("Java-MySQL Based Attendance Monitoring System for College Students at MSEUF-Candelaria");
        setSize(1250, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        AppTheme.stylePanel(mainPanel);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("Student Attendance Monitoring System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        AppTheme.styleTitle(titleLabel);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, WINDOW_MARGIN, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        buildCrudPanel();
        buildRightPanel();

        mainPanel.add(responsiveBody, BorderLayout.CENTER);

        setupDatabase();
        if (databaseReady) {
            loadData();
        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout();
            }
        });
        SwingUtilities.invokeLater(this::updateResponsiveLayout);
    }

    private void buildCrudPanel() {
        // this panel contains the main create read update and delete workflow for attendance records
        AppTheme.stylePanel(crudPanel);
        crudPanel.setBorder(AppTheme.createSectionBorder("Attendance CRUD", WINDOW_MARGIN));

        JPanel formPanel = new JPanel(new GridBagLayout());
        AppTheme.stylePanel(formPanel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtId = new JTextField();
        txtId.setEnabled(false);
        txtStudentIdentifier = new JTextField();
        txtStudentIdentifier.setEditable(false);
        txtName = new JTextField();
        txtDate = new JTextField();
        txtDate.setEditable(false);
        txtDate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtRemarks = new JTextField();
        cmbAttendance = new JComboBox<>(ATTENDANCE_OPTIONS);
        cmbSubjectEntry = new JComboBox<>(SUBJECTS);
        cmbSubjectEntry.setSelectedItem(selectedSubject);
        updateDateField();
        styleInputs(txtStudentIdentifier, txtName, txtDate, txtRemarks, cmbAttendance, cmbSubjectEntry);

        addFormRow(formPanel, gbc, 0, "Student ID:", txtStudentIdentifier);
        addFormRow(formPanel, gbc, 1, "Student Name:", txtName);
        addFormRow(formPanel, gbc, 2, "Date:", txtDate);
        addFormRow(formPanel, gbc, 3, "Subject:", cmbSubjectEntry);
        addFormRow(formPanel, gbc, 4, "Attendance:", cmbAttendance);
        addFormRow(formPanel, gbc, 5, "Remarks:", txtRemarks);

        JButton btnInsert = new JButton("Insert");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset Filter");
        styleButtons(btnInsert, btnUpdate, btnDelete, btnClear, btnSearch, btnReset);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        AppTheme.stylePanel(buttonPanel);
        buttonPanel.add(btnInsert);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        AppTheme.stylePanel(filterPanel);
        GridBagConstraints gbcFilter = new GridBagConstraints();
        gbcFilter.insets = new Insets(5, 5, 5, 5);
        gbcFilter.fill = GridBagConstraints.HORIZONTAL;

        txtSearch = new JTextField(12);
        txtFilterDate = new JTextField(10);
        cmbFilterAttendance = new JComboBox<>(prependAllOption(ATTENDANCE_OPTIONS));
        cmbFilterSubject = new JComboBox<>(prependAllOption(SUBJECTS));
        styleInputs(txtSearch, txtFilterDate, cmbFilterAttendance, cmbFilterSubject);

        addFilterRow(filterPanel, gbcFilter, 0, "Search:", txtSearch, "Status:", cmbFilterAttendance);
        addFilterRow(filterPanel, gbcFilter, 1, "Date:", txtFilterDate, "Subject:", cmbFilterSubject);
        gbcFilter.gridx = 0;
        gbcFilter.gridy = 2;
        gbcFilter.gridwidth = 2;
        filterPanel.add(btnSearch, gbcFilter);
        gbcFilter.gridx = 2;
        filterPanel.add(btnReset, gbcFilter);

        JPanel lowerPanel = new JPanel();
        AppTheme.stylePanel(lowerPanel);
        lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
        lowerPanel.add(buttonPanel);
        lowerPanel.add(Box.createVerticalStrut(12));
        lowerPanel.add(filterPanel);

        crudPanel.add(formPanel, BorderLayout.CENTER);
        crudPanel.add(lowerPanel, BorderLayout.SOUTH);

        btnInsert.addActionListener(e -> insertData());
        btnUpdate.addActionListener(e -> updateData());
        btnDelete.addActionListener(e -> deleteData());
        btnClear.addActionListener(e -> clearFields());
        btnSearch.addActionListener(e -> loadData());
        btnReset.addActionListener(e -> resetFilters());

        txtName.addActionListener(e -> autofillStudentIdentifier());
        txtName.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autofillStudentIdentifier();
            }
        });
        txtDate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LocalDate picked = CalendarPopup.pickDate(CRUD_GUI.this, selectedDate);
                if (picked != null) {
                    selectedDate = picked;
                    updateDateField();
                }
            }
        });
        cmbSubjectEntry.addActionListener(e -> selectedSubject = cmbSubjectEntry.getSelectedItem().toString());
        cmbFilterAttendance.addActionListener(e -> loadData());
        cmbFilterSubject.addActionListener(e -> loadData());
    }

    private void buildRightPanel() {
        // this panel shows saved records and opens the teacher and summary views
        AppTheme.stylePanel(rightPanel);
        rightPanel.setBorder(AppTheme.createSectionBorder("Views and Inserted Students", WINDOW_MARGIN));

        JPanel topPanel = new JPanel();
        AppTheme.stylePanel(topPanel);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JButton btnTeacherView = new JButton("Teacher View");
        JButton btnAttendanceSummary = new JButton("Attendance Summary");
        styleButtons(btnTeacherView, btnAttendanceSummary);
        JPanel viewButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        AppTheme.stylePanel(viewButtonPanel);
        viewButtonPanel.add(btnTeacherView);
        viewButtonPanel.add(btnAttendanceSummary);

        topPanel.add(viewButtonPanel);
        // para di maedit directly sa inserted rows
        model = new DefaultTableModel(new String[]{"Record ID", "Student ID", "Student Name", "Subject", "Date", "Time In", "Time Out", "Attendance", "Remarks"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        AppTheme.styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(0));

        JScrollPane scrollPane = new JScrollPane(table);
        AppTheme.styleScrollPane(scrollPane);
        rightPanel.add(topPanel, BorderLayout.NORTH);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        btnTeacherView.addActionListener(e -> openTeacherDashboard());
        btnAttendanceSummary.addActionListener(e -> openAttendanceSummary());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = table.getSelectedRow();
                if (i < 0) {
                    return;
                }

                txtId.setText(model.getValueAt(i, 0).toString());
                txtStudentIdentifier.setText(model.getValueAt(i, 1).toString());
                txtName.setText(model.getValueAt(i, 2).toString());
                selectedSubject = model.getValueAt(i, 3).toString();
                cmbSubjectEntry.setSelectedItem(selectedSubject);

                Object dateValue = model.getValueAt(i, 4);
                if (dateValue instanceof java.util.Date) {
                    java.util.Date d = (java.util.Date) dateValue;
                    selectedDate = new java.sql.Date(d.getTime()).toLocalDate();
                } else if (dateValue != null) {
                    try {
                        selectedDate = java.sql.Date.valueOf(dateValue.toString()).toLocalDate();
                    } catch (Exception ignore) {
                        selectedDate = LocalDate.now();
                    }
                }
                updateDateField();
                cmbAttendance.setSelectedItem(model.getValueAt(i, 7).toString());
                Object remarksValue = model.getValueAt(i, 8);
                txtRemarks.setText(remarksValue == null ? "" : remarksValue.toString());
            }
        });
    }

    private void updateResponsiveLayout() {
        // this method changes the layout when the window becomes narrow so the form remains readable
        boolean shouldStack = getWidth() < RESPONSIVE_BREAKPOINT;
        if (responsiveBody.getComponentCount() > 0 && stackedLayout == shouldStack) {
            return;
        }

        stackedLayout = shouldStack;
        responsiveBody.removeAll();

        if (shouldStack) {
            JPanel stacked = new JPanel();
            stacked.setLayout(new BoxLayout(stacked, BoxLayout.Y_AXIS));
            stacked.add(crudPanel);
            stacked.add(Box.createVerticalStrut(12));
            stacked.add(rightPanel);
            responsiveBody.add(new JScrollPane(stacked), BorderLayout.CENTER);
        } else {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, crudPanel, rightPanel);
            splitPane.setResizeWeight(0.35);
            splitPane.setBorder(null);
            responsiveBody.add(splitPane, BorderLayout.CENTER);
        }

        responsiveBody.revalidate();
        responsiveBody.repaint();
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void addFilterRow(JPanel panel, GridBagConstraints gbc, int row, String leftLabel, JComponent leftField, String rightLabel, JComponent rightField) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(leftLabel), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(leftField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel(rightLabel), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        panel.add(rightField, gbc);
    }

    private void styleButtons(AbstractButton... buttons) {
        for (AbstractButton button : buttons) {
            AppTheme.styleButton(button);
        }
    }

    private void styleInputs(JComponent... inputs) {
        for (JComponent input : inputs) {
            AppTheme.styleInput(input);
        }
    }

    void setupDatabase() {
        // this method creates the mysql database tables and missing columns needed by the app
        try {
            Connection tempCon = DriverManager.getConnection(URL, USER, PASS);
            Statement stmt = tempCon.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            tempCon.close();

            con = DriverManager.getConnection(URL + DB_NAME, USER, PASS);
            Statement dbStmt = con.createStatement();
            dbStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS students (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "student_name VARCHAR(100) NOT NULL," +
                            "student_identifier VARCHAR(50) NULL)"
            );
            dbStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS attendance_records (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "student_identifier VARCHAR(50) NOT NULL DEFAULT ''," +
                            "student_name VARCHAR(100) NOT NULL," +
                            "subject_name VARCHAR(100) NOT NULL DEFAULT 'General'," +
                            "attendance_status VARCHAR(30) NOT NULL," +
                            "attendance_date DATE NOT NULL," +
                            "attendance_time VARCHAR(10) NOT NULL," +
                            "time_out VARCHAR(10) NULL," +
                            "remarks TEXT NULL)"
            );

            // these schema checks allow older database copies to run with the newer program version
            ensureStudentsTableSchema(dbStmt);
            ensureColumnExists("attendance_records", "remarks", "ALTER TABLE attendance_records ADD COLUMN remarks TEXT NULL");
            ensureColumnExists("attendance_records", "attendance_time", "ALTER TABLE attendance_records ADD COLUMN attendance_time VARCHAR(20) NOT NULL DEFAULT '---'");
            ensureColumnExists("attendance_records", "time_out", "ALTER TABLE attendance_records ADD COLUMN time_out VARCHAR(20) NULL");
            ensureColumnExists("attendance_records", "subject_name", "ALTER TABLE attendance_records ADD COLUMN subject_name VARCHAR(100) NOT NULL DEFAULT 'General'");
            ensureColumnExists("attendance_records", "student_identifier", "ALTER TABLE attendance_records ADD COLUMN student_identifier VARCHAR(50) NOT NULL DEFAULT '' AFTER id");

            migrateExistingStudents();
            databaseReady = true;
        } catch (Exception e) {
            databaseReady = false;
            JOptionPane.showMessageDialog(this, "Database setup failed: " + e.getMessage());
        }
    }

    void loadData() {
        // this method loads table rows and builds the sql query based on optional search filters
        if (!isDatabaseReady()) {
            return;
        }

        String searchText = txtSearch == null ? "" : txtSearch.getText().trim();
        String filterDate = txtFilterDate == null ? "" : txtFilterDate.getText().trim();
        String filterStatus = (cmbFilterAttendance == null || cmbFilterAttendance.getSelectedIndex() == 0)
                ? "" : cmbFilterAttendance.getSelectedItem().toString();
        String filterSubject = (cmbFilterSubject == null || cmbFilterSubject.getSelectedIndex() == 0)
                ? "" : cmbFilterSubject.getSelectedItem().toString();

        if (!filterDate.isEmpty() && !isValidDate(filterDate)) {
            JOptionPane.showMessageDialog(this, "Filter date must be in YYYY-MM-DD format.");
            return;
        }

        try {
            model.setRowCount(0);

            StringBuilder sql = new StringBuilder(
                    "SELECT id, student_identifier, student_name, subject_name, attendance_status, attendance_date, attendance_time, time_out, remarks " +
                            "FROM attendance_records WHERE 1=1"
            );

            // dynamic sql is used here so empty filters do not affect the result set
            if (!searchText.isEmpty()) {
                sql.append(" AND (student_identifier LIKE ? OR student_name LIKE ? OR attendance_status LIKE ? OR remarks LIKE ? OR subject_name LIKE ?)");
            }
            if (!filterDate.isEmpty()) {
                sql.append(" AND attendance_date = ?");
            }
            if (!filterStatus.isEmpty()) {
                sql.append(" AND attendance_status = ?");
            }
            if (!filterSubject.isEmpty()) {
                sql.append(" AND subject_name = ?");
            }
            sql.append(" ORDER BY attendance_date DESC, id DESC");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            int parameterIndex = 1;

            // prepared statement parameters prevent direct user input from being placed into sql text
            if (!searchText.isEmpty()) {
                String keyword = "%" + searchText + "%";
                pst.setString(parameterIndex++, keyword);
                pst.setString(parameterIndex++, keyword);
                pst.setString(parameterIndex++, keyword);
                pst.setString(parameterIndex++, keyword);
                pst.setString(parameterIndex++, keyword);
            }
            if (!filterDate.isEmpty()) {
                pst.setDate(parameterIndex++, Date.valueOf(filterDate));
            }
            if (!filterStatus.isEmpty()) {
                pst.setString(parameterIndex++, filterStatus);
            }
            if (!filterSubject.isEmpty()) {
                pst.setString(parameterIndex++, filterSubject);
            }

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("student_identifier"),
                        rs.getString("student_name"),
                        rs.getString("subject_name"),
                        rs.getDate("attendance_date"),
                        rs.getString("attendance_time"),
                        rs.getString("time_out"),
                        rs.getString("attendance_status"),
                        rs.getString("remarks")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load data: " + e.getMessage());
        }
    }

    void insertData() {
        // this method validates input then saves a new attendance record with automatic time handling
        if (!isDatabaseReady()) {
            return;
        }
        if (!validateForm(false)) {
            return;
        }

        try {
            String studentName = txtName.getText().trim();
            String studentIdentifier = ensureStudentIdentifierForName(studentName);
            if (studentIdentifier == null) {
                return;
            }
            txtStudentIdentifier.setText(studentIdentifier);
            String status = cmbAttendance.getSelectedItem().toString();
            selectedSubject = cmbSubjectEntry.getSelectedItem().toString();

            PreparedStatement existingPst = con.prepareStatement(
                    "SELECT id, attendance_time, time_out FROM attendance_records WHERE student_identifier = ? AND attendance_date = ? AND subject_name = ? LIMIT 1"
            );
            existingPst.setString(1, studentIdentifier);
            existingPst.setDate(2, java.sql.Date.valueOf(selectedDate));
            existingPst.setString(3, selectedSubject);
            ResultSet rs = existingPst.executeQuery();

            if (!rs.next()) {
                PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO attendance_records(student_identifier, student_name, subject_name, attendance_status, attendance_date, attendance_time, time_out, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                );
                pst.setString(1, studentIdentifier);
                pst.setString(2, studentName);
                pst.setString(3, selectedSubject);
                pst.setString(4, status);
                pst.setDate(5, java.sql.Date.valueOf(selectedDate));
                pst.setString(6, resolveTimeIn(status));
                pst.setString(7, "");
                pst.setString(8, txtRemarks.getText().trim());
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Time in saved.");
            } else {
                String currentTimeIn = rs.getString("attendance_time");
                String currentTimeOut = rs.getString("time_out");
                if (currentTimeIn == null || currentTimeIn.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Time in is missing for this attendance record.");
                    return;
                }
                if (currentTimeOut != null && !currentTimeOut.trim().isEmpty() && !"---".equals(currentTimeOut)) {
                    JOptionPane.showMessageDialog(this, studentName + " has already timed in and timed out for this subject and date.");
                    return;
                }

                PreparedStatement timeOutPst = con.prepareStatement(
                        "UPDATE attendance_records SET attendance_status=?, time_out=?, remarks=? WHERE id=?"
                );
                timeOutPst.setString(1, status);
                timeOutPst.setString(2, resolveTimeOut(status));
                timeOutPst.setString(3, txtRemarks.getText().trim());
                timeOutPst.setInt(4, rs.getInt("id"));
                timeOutPst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Time out saved.");
            }

            loadData();
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + e.getMessage());
        }
    }

    void updateData() {
        // this method updates editable attendance fields without changing the original time in and time out values
        if (!isDatabaseReady()) {
            return;
        }
        if (!validateForm(true)) {
            return;
        }

        try {
            String studentName = txtName.getText().trim();
            String studentIdentifier = ensureStudentIdentifierForName(studentName);
            if (studentIdentifier == null) {
                return;
            }
            txtStudentIdentifier.setText(studentIdentifier);
            selectedSubject = cmbSubjectEntry.getSelectedItem().toString();

            PreparedStatement duplicateCheck = con.prepareStatement(
                    "SELECT 1 FROM attendance_records WHERE student_identifier = ? AND attendance_date = ? AND subject_name = ? AND id <> ?"
            );
            duplicateCheck.setString(1, studentIdentifier);
            duplicateCheck.setDate(2, java.sql.Date.valueOf(selectedDate));
            duplicateCheck.setString(3, selectedSubject);
            duplicateCheck.setInt(4, Integer.parseInt(txtId.getText().trim()));
            ResultSet duplicateRs = duplicateCheck.executeQuery();
            if (duplicateRs.next()) {
                JOptionPane.showMessageDialog(this, "Another attendance record already exists for this student, subject, and date.");
                return;
            }

            PreparedStatement pst = con.prepareStatement(
                    "UPDATE attendance_records SET student_identifier=?, student_name=?, subject_name=?, attendance_status=?, attendance_date=?, remarks=? WHERE id=?"
            );
            pst.setString(1, studentIdentifier);
            pst.setString(2, studentName);
            pst.setString(3, selectedSubject);
            pst.setString(4, cmbAttendance.getSelectedItem().toString());
            pst.setDate(5, java.sql.Date.valueOf(selectedDate));
            pst.setString(6, txtRemarks.getText().trim());
            pst.setInt(7, Integer.parseInt(txtId.getText().trim()));

            int updated = pst.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Updated!");
                loadData();
                clearFields();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Update failed: " + e.getMessage());
        }
    }

    void deleteData() {
        // this method asks for confirmation before removing a selected attendance record
        if (!isDatabaseReady()) {
            return;
        }
        if (txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a record first before deleting.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete the selected attendance record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            PreparedStatement pst = con.prepareStatement("DELETE FROM attendance_records WHERE id=?");
            pst.setInt(1, Integer.parseInt(txtId.getText().trim()));

            int deleted = pst.executeUpdate();
            if (deleted > 0) {
                JOptionPane.showMessageDialog(this, "Deleted!");
                loadData();
                clearFields();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
        }
    }

    boolean validateForm(boolean requireId) {
        // this validation keeps required fields and student name format consistent before saving
        if (requireId && txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return false;
        }
        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Student name is required.");
            return false;
        }
        if (!isValidStudentNameFormat(txtName.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Student name must follow this format: Surname, Firstname");
            return false;
        }
        return true;
    }

    boolean isValidDate(String value) {
        try {
            Date.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void clearFields() {
        txtId.setText("");
        txtStudentIdentifier.setText("");
        txtName.setText("");
        selectedDate = LocalDate.now();
        selectedSubject = "General";
        cmbSubjectEntry.setSelectedItem(selectedSubject);
        updateDateField();
        txtRemarks.setText("");
        cmbAttendance.setSelectedIndex(0);
        table.clearSelection();
    }

    void updateDateField() {
        txtDate.setText(selectedDate.format(DATE_FORMATTER));
    }

    boolean isValidStudentNameFormat(String studentName) {
        return studentName.matches("[A-Za-z .'-]+,\\s*[A-Za-z .'-]+");
    }

    boolean isNonTimedStatus(String status) {
        // absent and excuse records do not need time in and time out values
        return "Absent".equalsIgnoreCase(status) || "Excuse".equalsIgnoreCase(status);
    }

    String resolveTimeIn(String status) {
        if (isNonTimedStatus(status)) {
            return "";
        }
        return LocalTime.now().format(TIME_FORMATTER);
    }

    String resolveTimeOut(String status) {
        if (isNonTimedStatus(status)) {
            return "";
        }
        return LocalTime.now().format(TIME_FORMATTER);
    }

    void resetFilters() {
        txtSearch.setText("");
        txtFilterDate.setText("");
        cmbFilterAttendance.setSelectedIndex(0);
        cmbFilterSubject.setSelectedIndex(0);
        loadData();
    }

    String[] prependAllOption(String[] values) {
        String[] options = new String[values.length + 1];
        options[0] = "All";
        System.arraycopy(values, 0, options, 1, values.length);
        return options;
    }

    boolean isDatabaseReady() {
        if (databaseReady && con != null) {
            return true;
        }
        JOptionPane.showMessageDialog(this, "Database is not ready.");
        return false;
    }

    String valueOf(JTextField field) {
        return field == null ? "" : field.getText().trim();
    }

    void openTeacherDashboard() {
        if (!isDatabaseReady()) {
            return;
        }
        new TeacherDashboard(con).setVisible(true);
    }

    void openAttendanceSummary() {
        if (!isDatabaseReady()) {
            return;
        }
        new AttendanceSummaryWindow(con).setVisible(true);
    }

    void ensureColumnExists(String tableName, String columnName, String alterSql) throws Exception {
        // this checks mysql metadata before running alter table so the app can start repeatedly
        PreparedStatement columnCheck = con.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ? LIMIT 1"
        );
        columnCheck.setString(1, DB_NAME);
        columnCheck.setString(2, tableName);
        columnCheck.setString(3, columnName);
        ResultSet rs = columnCheck.executeQuery();
        if (!rs.next()) {
            con.createStatement().executeUpdate(alterSql);
        }
        rs.close();
        columnCheck.close();
    }

    void ensureIndexExists(String tableName, String indexName, String createSql) throws Exception {
        PreparedStatement indexCheck = con.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ? LIMIT 1"
        );
        indexCheck.setString(1, DB_NAME);
        indexCheck.setString(2, tableName);
        indexCheck.setString(3, indexName);
        ResultSet rs = indexCheck.executeQuery();
        if (!rs.next()) {
            con.createStatement().executeUpdate(createSql);
        }
        rs.close();
        indexCheck.close();
    }

    void ensureStudentsTableSchema(Statement dbStmt) throws Exception {
        // this keeps the students table ready for automatic student id generation
        ensureColumnExists("students", "student_name", "ALTER TABLE students ADD COLUMN student_name VARCHAR(100) NOT NULL DEFAULT ''");
        ensureColumnExists("students", "student_identifier", "ALTER TABLE students ADD COLUMN student_identifier VARCHAR(50) NULL");
        backfillStudentIdentifiers();
        ensureIndexExists("students", "idx_students_student_identifier", "CREATE UNIQUE INDEX idx_students_student_identifier ON students(student_identifier)");
    }

    void backfillStudentIdentifiers() throws Exception {
        // this gives old student rows a generated student id when the column was previously empty
        PreparedStatement pst = con.prepareStatement(
                "SELECT id, student_name FROM students WHERE student_identifier IS NULL OR TRIM(student_identifier) = ''"
        );
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            int studentRowId = rs.getInt("id");
            String generatedIdentifier = generateAutomaticStudentIdentifier(rs.getString("student_name"));

            PreparedStatement updateStudent = con.prepareStatement(
                    "UPDATE students SET student_identifier = ? WHERE id = ?"
            );
            updateStudent.setString(1, generatedIdentifier);
            updateStudent.setInt(2, studentRowId);
            updateStudent.executeUpdate();
        }
    }

    void autofillStudentIdentifier() {
        // this finds an existing student id as soon as the user enters a known student name
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            txtStudentIdentifier.setText("");
            return;
        }

        try {
            String studentIdentifier = findStudentIdentifierByName(name);
            txtStudentIdentifier.setText(studentIdentifier == null ? "" : studentIdentifier);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to check student ID: " + e.getMessage());
        }
    }

    String ensureStudentIdentifierForName(String studentName) throws Exception {
        // this reuses an existing student id or creates a new one for a new student name
        String existingIdentifier = findStudentIdentifierByName(studentName);
        if (existingIdentifier != null) {
            return existingIdentifier;
        }
        String generatedIdentifier = generateAutomaticStudentIdentifier(studentName);
        PreparedStatement pst = con.prepareStatement(
                "INSERT INTO students(student_name, student_identifier) VALUES (?, ?)"
        );
        pst.setString(1, studentName);
        pst.setString(2, generatedIdentifier);
        pst.executeUpdate();
        return generatedIdentifier;
    }

    String findStudentIdentifierByName(String studentName) throws Exception {
        PreparedStatement pst = con.prepareStatement(
                "SELECT student_identifier FROM students WHERE student_name = ? LIMIT 1"
        );
        pst.setString(1, studentName);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getString("student_identifier") : null;
    }

    String findStudentNameByIdentifier(String studentIdentifier) throws Exception {
        PreparedStatement pst = con.prepareStatement(
                "SELECT student_name FROM students WHERE student_identifier = ? LIMIT 1"
        );
        pst.setString(1, studentIdentifier);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getString("student_name") : null;
    }

    void migrateExistingStudents() throws Exception {
        // this moves old attendance names into the students table and links records to generated ids
        PreparedStatement pst = con.prepareStatement(
                "SELECT DISTINCT student_name FROM attendance_records WHERE student_name IS NOT NULL AND TRIM(student_name) <> ''"
        );
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            String studentName = rs.getString("student_name");
            String studentIdentifier = findStudentIdentifierByName(studentName);
            if (studentIdentifier == null) {
                studentIdentifier = generateAutomaticStudentIdentifier(studentName);
                PreparedStatement insertStudent = con.prepareStatement(
                        "INSERT INTO students(student_name, student_identifier) VALUES (?, ?)"
                );
                insertStudent.setString(1, studentName);
                insertStudent.setString(2, studentIdentifier);
                insertStudent.executeUpdate();
            }

            PreparedStatement updateAttendance = con.prepareStatement(
                    "UPDATE attendance_records SET student_identifier = ? WHERE student_name = ? AND (student_identifier IS NULL OR student_identifier = '')"
            );
            updateAttendance.setString(1, studentIdentifier);
            updateAttendance.setString(2, studentName);
            updateAttendance.executeUpdate();
        }
    }

    String generateAutomaticStudentIdentifier() throws Exception {
        return generateAutomaticStudentIdentifier(valueOf(txtName));
    }

    String generateAutomaticStudentIdentifier(String studentName) throws Exception {
        // this builds a student id from name letters current year and the next available number
        String prefix = buildStudentIdPrefix(studentName);
        String year = String.format("%02d", LocalDate.now().getYear() % 100);
        String base = prefix + year;

        PreparedStatement pst = con.prepareStatement(
                "SELECT student_identifier FROM students WHERE student_identifier LIKE ? ORDER BY student_identifier DESC LIMIT 1"
        );
        pst.setString(1, base + "%");
        ResultSet rs = pst.executeQuery();

        int nextNumber = 1;
        if (rs.next()) {
            String lastIdentifier = rs.getString("student_identifier");
            if (lastIdentifier != null && lastIdentifier.startsWith(base)) {
                String numericPart = lastIdentifier.substring(base.length());
                try {
                    nextNumber = Integer.parseInt(numericPart) + 1;
                } catch (NumberFormatException ignore) {
                    nextNumber = 1;
                }
            }
        }

        String candidate;
        // this loop avoids duplicates by checking every generated candidate before returning it
        do {
            if (nextNumber > 99) {
                throw new IllegalStateException("Student ID limit reached for prefix " + base);
            }
            candidate = base + String.format("%02d", nextNumber++);
        } while (findStudentNameByIdentifier(candidate) != null);

        return candidate;
    }

    String buildStudentIdPrefix(String studentName) {
        // this extracts only letters from the student name so the id prefix is predictable
        String lettersOnly = Pattern.compile("[^A-Za-z]").matcher(studentName == null ? "" : studentName).replaceAll("").toUpperCase(Locale.ENGLISH);
        if (lettersOnly.length() >= 2) {
            return lettersOnly.substring(0, 2);
        }
        if (lettersOnly.length() == 1) {
            return lettersOnly + "X";
        }
        return "XX";
    }

    String generateLegacyStudentIdentifier(String studentName) throws Exception {
        return generateAutomaticStudentIdentifier();
    }

    static class CalendarPopup {
        static LocalDate pickDate(Component parent, LocalDate initial) {
            // this custom calendar lets users choose a date without typing the date manually
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Select Date", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            LocalDate[] result = {null};
            YearMonth[] shownMonth = {YearMonth.from(initial == null ? LocalDate.now() : initial)};

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(new Font("Arial", Font.BOLD, 22));

            JButton prev = new JButton("<");
            JButton next = new JButton(">");
            JPanel header = new JPanel(new BorderLayout(10, 0));
            header.add(prev, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);
            root.add(header, BorderLayout.NORTH);

            JPanel grid = new JPanel(new GridLayout(0, 7, 6, 6));
            root.add(grid, BorderLayout.CENTER);

            JButton cancel = new JButton("Cancel");
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.add(cancel);
            root.add(footer, BorderLayout.SOUTH);

            Runnable rebuild = () -> {
                // this rebuilds the month grid every time the user changes the visible month
                grid.removeAll();
                YearMonth ym = shownMonth[0];
                monthLabel.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear());

                DayOfWeek[] week = {DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};
                for (DayOfWeek dow : week) {
                    JLabel lbl = new JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                    grid.add(lbl);
                }

                int firstDayIndex = ym.atDay(1).getDayOfWeek().getValue() % 7;
                for (int i = 0; i < firstDayIndex; i++) {
                    grid.add(new JLabel(""));
                }

                int length = ym.lengthOfMonth();
                for (int day = 1; day <= length; day++) {
                    LocalDate d = ym.atDay(day);
                    JButton b = new JButton(String.valueOf(day));
                    if (d.equals(LocalDate.now())) {
                        b.setFont(b.getFont().deriveFont(Font.BOLD));
                    }
                    b.addActionListener(e -> {
                        result[0] = d;
                        dialog.dispose();
                    });
                    grid.add(b);
                }
                dialog.pack();
                dialog.setLocationRelativeTo(parent);
                grid.revalidate();
                grid.repaint();
            };

            prev.addActionListener(e -> {
                shownMonth[0] = shownMonth[0].minusMonths(1);
                rebuild.run();
            });
            next.addActionListener(e -> {
                shownMonth[0] = shownMonth[0].plusMonths(1);
                rebuild.run();
            });
            cancel.addActionListener(e -> {
                result[0] = null;
                dialog.dispose();
            });

            dialog.setContentPane(root);
            dialog.setResizable(false);
            rebuild.run();
            dialog.setVisible(true);

            return result[0];
        }
    }

    private static boolean showLoginDialog() {
        // this modal login prevents users from opening the system without the configured credentials
        final JDialog dialog = new JDialog((Frame) null, "Login", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Please login to continue", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        content.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtUsername = new JTextField(18);
        JPasswordField txtPassword = new JPasswordField(18);

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        form.add(txtUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        form.add(txtPassword, gbc);

        content.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnLogin = new JButton("Login");
        JButton btnCancel = new JButton("Cancel");
        buttons.add(btnLogin);
        buttons.add(btnCancel);
        content.add(buttons, BorderLayout.SOUTH);

        final boolean[] authenticated = {false};

        btnLogin.addActionListener(e -> {
            if (LOGIN_USERNAME.equals(txtUsername.getText().trim()) && LOGIN_PASSWORD.equals(new String(txtPassword.getPassword()))) {
                authenticated[0] = true;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Invalid credentials.");
            }
        });
        btnCancel.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return authenticated[0];
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    public static void main(String[] args) {
        setUIFont(new javax.swing.plaf.FontUIResource(new Font("Arial", Font.PLAIN, 18)));
        SwingUtilities.invokeLater(() -> {
            if (showLoginDialog()) {
                new CRUD_GUI().setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
