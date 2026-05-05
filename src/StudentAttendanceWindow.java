import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.regex.Pattern;

public class StudentAttendanceWindow extends JFrame {

    private static final int WINDOW_MARGIN = 18;

    private final Connection con;
    private final JTextField txtStudentIdentifier = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtDate = new JTextField();
    private final JTextField txtRemarks = new JTextField();
    private final JComboBox<String> cmbSubject = new JComboBox<>(CRUD_GUI.SUBJECTS);
    private final JComboBox<String> cmbAttendance = new JComboBox<>(CRUD_GUI.ATTENDANCE_OPTIONS);
    private final JButton btnTimeAction = new JButton("Time In");
    private final JLabel lblLookupStatus = new JLabel(" ");
    private LocalDate selectedDate = LocalDate.now();

    public StudentAttendanceWindow(Connection con) {
        this.con = con;
        AppTheme.install();

        setTitle("Student Attendance Time Entry");
        setSize(900, 620);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        AppTheme.stylePanel(content);
        content.setBorder(BorderFactory.createEmptyBorder(WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN, WINDOW_MARGIN));
        setContentPane(content);

        JLabel title = new JLabel("Student Time Entry", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        AppTheme.styleTitle(title);
        content.add(title, BorderLayout.NORTH);
        content.add(buildEntryPanel(), BorderLayout.CENTER);

        updateDateField();
        updateActionLabel();
    }

    private JPanel buildEntryPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        AppTheme.stylePanel(wrapper);
        wrapper.setBorder(AppTheme.createSectionBorder("Attendance Entry", WINDOW_MARGIN));

        JPanel form = new JPanel(new GridBagLayout());
        AppTheme.stylePanel(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtDate.setEditable(false);
        txtDate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleInputs(txtStudentIdentifier, txtName, txtDate, txtRemarks, cmbSubject, cmbAttendance);
        lblLookupStatus.setForeground(AppTheme.MAROON_DARK);

        addFormRow(form, gbc, 0, "Student ID:", txtStudentIdentifier);
        addFormRow(form, gbc, 1, "Student Name:", txtName);
        addFormRow(form, gbc, 2, "Date:", txtDate);
        addFormRow(form, gbc, 3, "Subject:", cmbSubject);
        addFormRow(form, gbc, 4, "Attendance:", cmbAttendance);
        addFormRow(form, gbc, 5, "Remarks:", txtRemarks);

        AppTheme.styleButton(btnTimeAction);
        JButton btnClear = new JButton("Clear");
        AppTheme.styleButton(btnClear);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        AppTheme.stylePanel(buttons);
        buttons.add(btnClear);
        buttons.add(btnTimeAction);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        form.add(lblLookupStatus, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        form.add(buttons, gbc);

        txtDate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LocalDate picked = CRUD_GUI.CalendarPopup.pickDate(StudentAttendanceWindow.this, selectedDate);
                if (picked != null) {
                    selectedDate = picked;
                    updateDateField();
                    updateActionLabel();
                }
            }
        });
        txtName.addActionListener(e -> {
            autofillStudentIdentifier();
            updateActionLabel();
        });
        txtName.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autofillStudentIdentifier();
                updateActionLabel();
            }
        });
        txtStudentIdentifier.addActionListener(e -> {
            autofillStudentNameFromIdentifier();
            updateActionLabel();
        });
        txtStudentIdentifier.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autofillStudentNameFromIdentifier();
                updateActionLabel();
            }
        });
        txtStudentIdentifier.addCaretListener(e -> updateActionLabel());
        txtName.addCaretListener(e -> updateActionLabel());
        cmbSubject.addActionListener(e -> updateActionLabel());
        cmbAttendance.addActionListener(e -> updateActionLabel());
        btnClear.addActionListener(e -> clearFields());
        btnTimeAction.addActionListener(e -> saveAttendanceAction());

        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.gridy = 0;
        outer.weightx = 1;
        outer.weighty = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;
        outer.anchor = GridBagConstraints.NORTH;
        wrapper.add(form, outer);
        return wrapper;
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

    private void styleInputs(JComponent... inputs) {
        for (JComponent input : inputs) {
            AppTheme.styleInput(input);
        }
    }

    private void saveAttendanceAction() {
        if (!validateForm()) {
            return;
        }

        try {
            StudentIdentity identity = resolveStudentIdentityForSave();
            if (identity == null) {
                return;
            }
            String studentName = identity.studentName;
            String studentIdentifier = identity.studentIdentifier;
            txtStudentIdentifier.setText(studentIdentifier);
            txtName.setText(studentName);

            String subject = cmbSubject.getSelectedItem().toString();
            String status = cmbAttendance.getSelectedItem().toString();
            PreparedStatement existingPst = con.prepareStatement(
                    "SELECT id, attendance_time, time_out FROM attendance_records WHERE student_identifier = ? AND attendance_date = ? AND subject_name = ? LIMIT 1"
            );
            existingPst.setString(1, studentIdentifier);
            existingPst.setDate(2, Date.valueOf(selectedDate));
            existingPst.setString(3, subject);
            ResultSet rs = existingPst.executeQuery();

            if (!rs.next()) {
                PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO attendance_records(student_identifier, student_name, subject_name, attendance_status, attendance_date, attendance_time, time_out, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                );
                pst.setString(1, studentIdentifier);
                pst.setString(2, studentName);
                pst.setString(3, subject);
                pst.setString(4, status);
                pst.setDate(5, Date.valueOf(selectedDate));
                pst.setString(6, resolveTimeIn(status));
                pst.setString(7, "");
                pst.setString(8, txtRemarks.getText().trim());
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, isNonTimedStatus(status) ? status + " record saved." : "Time in saved.");
            } else {
                String currentTimeOut = rs.getString("time_out");
                if (currentTimeOut != null && !currentTimeOut.trim().isEmpty() && !"---".equals(currentTimeOut)) {
                    JOptionPane.showMessageDialog(this, studentName + " already has a complete record for this subject and date.");
                    return;
                }

                PreparedStatement pst = con.prepareStatement(
                        "UPDATE attendance_records SET attendance_status=?, time_out=?, remarks=? WHERE id=?"
                );
                pst.setString(1, status);
                pst.setString(2, resolveTimeOut(status));
                pst.setString(3, txtRemarks.getText().trim());
                pst.setInt(4, rs.getInt("id"));
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, isNonTimedStatus(status) ? status + " record updated." : "Time out saved.");
            }
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to save attendance: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        String studentName = txtName.getText().trim();
        String studentIdentifier = txtStudentIdentifier.getText().trim();
        if (studentName.isEmpty() && studentIdentifier.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter your Student ID or Student Name.");
            return false;
        }
        if (!studentName.isEmpty() && !studentName.matches("[A-Za-z .'-]+,\\s*[A-Za-z .'-]+")) {
            JOptionPane.showMessageDialog(this, "Student name must follow this format: Surname, Firstname");
            return false;
        }
        return true;
    }

    private void updateActionLabel() {
        String status = cmbAttendance.getSelectedItem() == null ? "Present" : cmbAttendance.getSelectedItem().toString();
        if (isNonTimedStatus(status)) {
            btnTimeAction.setText("Save " + status);
            return;
        }
        try {
            String name = txtName.getText().trim();
            String id = txtStudentIdentifier.getText().trim();
            if (id.isEmpty() && !name.isEmpty()) {
                id = findStudentIdentifierByName(name);
            }
            if (id != null && hasOpenTimeIn(id, cmbSubject.getSelectedItem().toString())) {
                btnTimeAction.setText("Time Out");
            } else {
                btnTimeAction.setText("Time In");
            }
        } catch (Exception e) {
            btnTimeAction.setText("Time In");
        }
    }

    private boolean hasOpenTimeIn(String studentIdentifier, String subject) throws Exception {
        PreparedStatement pst = con.prepareStatement(
                "SELECT 1 FROM attendance_records WHERE student_identifier = ? AND attendance_date = ? AND subject_name = ? " +
                        "AND attendance_time IS NOT NULL AND TRIM(attendance_time) <> '' AND (time_out IS NULL OR TRIM(time_out) = '' OR time_out = '---') LIMIT 1"
        );
        pst.setString(1, studentIdentifier);
        pst.setDate(2, Date.valueOf(selectedDate));
        pst.setString(3, subject);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }

    private void autofillStudentIdentifier() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            lblLookupStatus.setText(" ");
            return;
        }
        try {
            String id = findStudentIdentifierByName(name);
            if (id != null) {
                txtStudentIdentifier.setText(id);
                lblLookupStatus.setText("Student ID found for this name.");
            } else if (txtStudentIdentifier.getText().trim().isEmpty()) {
                lblLookupStatus.setText("New student: an ID will be generated after first save.");
            }
        } catch (Exception e) {
            lblLookupStatus.setText("Unable to check Student ID.");
        }
    }

    private void autofillStudentNameFromIdentifier() {
        String studentIdentifier = txtStudentIdentifier.getText().trim();
        if (studentIdentifier.isEmpty()) {
            lblLookupStatus.setText(" ");
            return;
        }
        try {
            String name = findStudentNameByIdentifier(studentIdentifier);
            if (name != null) {
                txtName.setText(name);
                lblLookupStatus.setText("Student found. Name filled automatically.");
            } else {
                lblLookupStatus.setText("Student ID not found. Type your name once to create your record.");
            }
        } catch (Exception e) {
            lblLookupStatus.setText("Unable to check Student ID.");
        }
    }

    private StudentIdentity resolveStudentIdentityForSave() throws Exception {
        String typedId = txtStudentIdentifier.getText().trim();
        String typedName = txtName.getText().trim();

        if (!typedId.isEmpty()) {
            String nameById = findStudentNameByIdentifier(typedId);
            if (nameById != null) {
                return new StudentIdentity(typedId, nameById);
            }
            if (typedName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Student ID was not found. Type your name once to create your student record.");
                return null;
            }
        }

        String studentIdentifier = ensureStudentIdentifierForName(typedName);
        return new StudentIdentity(studentIdentifier, typedName);
    }

    private String ensureStudentIdentifierForName(String studentName) throws Exception {
        String existing = findStudentIdentifierByName(studentName);
        if (existing != null) {
            return existing;
        }
        String generated = generateAutomaticStudentIdentifier(studentName);
        PreparedStatement pst = con.prepareStatement("INSERT INTO students(student_name, student_identifier) VALUES (?, ?)");
        pst.setString(1, studentName);
        pst.setString(2, generated);
        pst.executeUpdate();
        return generated;
    }

    private String findStudentIdentifierByName(String studentName) throws Exception {
        PreparedStatement pst = con.prepareStatement("SELECT student_identifier FROM students WHERE student_name = ? LIMIT 1");
        pst.setString(1, studentName);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getString("student_identifier") : null;
    }

    private String findStudentNameByIdentifier(String studentIdentifier) throws Exception {
        PreparedStatement pst = con.prepareStatement("SELECT student_name FROM students WHERE student_identifier = ? LIMIT 1");
        pst.setString(1, studentIdentifier);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getString("student_name") : null;
    }

    private String generateAutomaticStudentIdentifier(String studentName) throws Exception {
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
                try {
                    nextNumber = Integer.parseInt(lastIdentifier.substring(base.length())) + 1;
                } catch (NumberFormatException ignore) {
                    nextNumber = 1;
                }
            }
        }

        String candidate;
        do {
            if (nextNumber > 99) {
                throw new IllegalStateException("Student ID limit reached for prefix " + base);
            }
            candidate = base + String.format("%02d", nextNumber++);
        } while (findStudentNameByIdentifier(candidate) != null);
        return candidate;
    }

    private String buildStudentIdPrefix(String studentName) {
        String lettersOnly = Pattern.compile("[^A-Za-z]").matcher(studentName == null ? "" : studentName).replaceAll("").toUpperCase(Locale.ENGLISH);
        if (lettersOnly.length() >= 2) {
            return lettersOnly.substring(0, 2);
        }
        if (lettersOnly.length() == 1) {
            return lettersOnly + "X";
        }
        return "XX";
    }

    private boolean isNonTimedStatus(String status) {
        return "Absent".equalsIgnoreCase(status) || "Excuse".equalsIgnoreCase(status);
    }

    private String resolveTimeIn(String status) {
        return isNonTimedStatus(status) ? "" : LocalTime.now().format(CRUD_GUI.TIME_FORMATTER);
    }

    private String resolveTimeOut(String status) {
        return isNonTimedStatus(status) ? "" : LocalTime.now().format(CRUD_GUI.TIME_FORMATTER);
    }

    private void updateDateField() {
        txtDate.setText(selectedDate.format(CRUD_GUI.DATE_FORMATTER));
    }

    private void clearFields() {
        txtStudentIdentifier.setText("");
        txtName.setText("");
        lblLookupStatus.setText(" ");
        selectedDate = LocalDate.now();
        updateDateField();
        cmbSubject.setSelectedIndex(0);
        cmbAttendance.setSelectedIndex(0);
        txtRemarks.setText("");
        updateActionLabel();
    }

    private static class StudentIdentity {
        private final String studentIdentifier;
        private final String studentName;

        private StudentIdentity(String studentIdentifier, String studentName) {
            this.studentIdentifier = studentIdentifier;
            this.studentName = studentName;
        }
    }
}
