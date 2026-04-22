import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * CRUD_GUI - A Student Attendance Monitoring System application.
 * This class provides a Graphical User Interface (GUI) to Create, Read, Update, and Delete (CRUD) 
 * student attendance records stored in a MySQL database.
 */
public class CRUD_GUI extends JFrame {

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // UI Components for data entry and display
    JTextField txtId;           // Displays the unique record ID (auto-generated)
    JTextField txtName;         // Field to enter student name
    JTextField txtDate;         // Date picker display (click to open calendar)
    JTextField txtRemarks;      // Field to enter custom remarks/message
    JTextField txtSearch;       // Field to search records by name or status
    JTextField txtFilterDate;   // Field to filter records by a specific date
    JComboBox<String> cmbAttendance; // Dropdown to select attendance status (Present/Absent)
    JTable table;               // Table to display records from the database
    DefaultTableModel model;    // Data model for the JTable

    LocalDate selectedDate = LocalDate.now();

    // Database connection constants
    static final String URL = "jdbc:mysql://localhost:3306/"; // MySQL server address
    static final String DB_NAME = "crud_gui_db";             // Database name
    static final String USER = "root";                        // Database username
    static final String PASS = "Administrator.123";           // Database password

    // Login credentials (change as needed)
    static final String LOGIN_USERNAME = "admin";
    static final String LOGIN_PASSWORD = "admin";

    Connection con; // Connection object to interact with MySQL

    public CRUD_GUI() {
        // Basic window setup
        setTitle("Java-MySQL Based Attendance Monitoring System for College Students at MSEUF-Candelaria");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window on the screen
        setLayout(new BorderLayout(30, 30));

        // mainPanel: Acts as the container with 0.8cm (30px) margins
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setContentPane(mainPanel);

        // topPanel: Holds the title, input form, buttons, and filters
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // Application Title
        JLabel titleLabel = new JLabel("Student Attendance Monitoring System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // formPanel: Input fields organized in a grid (Label next to TextField)
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        txtId = new JTextField();
        txtId.setEnabled(false); // ID is read-only as it's auto-incremented in DB
        txtName = new JTextField();
        txtDate = new JTextField();
        txtDate.setEditable(false);
        txtDate.setText(selectedDate.format(DATE_FORMATTER));
        txtDate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtRemarks = new JTextField();
        cmbAttendance = new JComboBox<>(new String[]{"Present", "Absent", "Late", "Excuse"});

        formPanel.add(new JLabel("ID:"));
        formPanel.add(txtId);
        formPanel.add(new JLabel("Student Name:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Date:"));
        formPanel.add(txtDate);
        formPanel.add(new JLabel("Attendance:"));
        formPanel.add(cmbAttendance);
        formPanel.add(new JLabel("Remarks:"));
        formPanel.add(txtRemarks);

        // buttonPanel: Action buttons for CRUD operations
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton btnInsert = new JButton("Insert");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");

        buttonPanel.add(btnInsert);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        // filterPanel: Components for searching and filtering the list
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        txtSearch = new JTextField(15);
        txtFilterDate = new JTextField(10);
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset Filter");

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(txtSearch);
        filterPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        filterPanel.add(txtFilterDate);
        filterPanel.add(btnSearch);
        filterPanel.add(btnReset);

        // Grouping form, buttons, and filters together
        JPanel northContent = new JPanel(new BorderLayout(10, 10));
        northContent.add(formPanel, BorderLayout.NORTH);
        northContent.add(buttonPanel, BorderLayout.CENTER);
        northContent.add(filterPanel, BorderLayout.SOUTH);

        topPanel.add(northContent, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Table Setup: Defining columns and making cells non-editable
        model = new DefaultTableModel(new String[]{"ID", "Student Name", "Date", "Time", "Attendance", "Remarks"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent users from editing directly in the table
            }
        };
        table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Initialize Database and load existing records
        setupDatabase();
        loadData();

        // Button Action Listeners
        btnInsert.addActionListener(e -> insertData()); // Click to add record
        btnUpdate.addActionListener(e -> updateData()); // Click to save changes
        btnDelete.addActionListener(e -> deleteData()); // Click to remove record
        btnClear.addActionListener(e -> clearFields()); // Click to reset input form
        btnSearch.addActionListener(e -> loadData());    // Click to filter results
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            txtFilterDate.setText("");
            loadData(); // Reload all data
        });

        // Pop-up calendar date picker
        txtDate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LocalDate picked = CalendarPopup.pickDate(CRUD_GUI.this, selectedDate);
                if (picked != null) {
                    selectedDate = picked;
                    txtDate.setText(selectedDate.format(DATE_FORMATTER));
                }
            }
        });

        // Table Mouse Listener: Click a row to populate the input form for editing
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int i = table.getSelectedRow();
                if (i < 0) {
                    return;
                }

                txtId.setText(model.getValueAt(i, 0).toString());
                txtName.setText(model.getValueAt(i, 1).toString());
                Object dateValue = model.getValueAt(i, 2);
                if (dateValue instanceof java.util.Date) {
                    java.util.Date d = (java.util.Date) dateValue;
                    selectedDate = new java.sql.Date(d.getTime()).toLocalDate();
                    txtDate.setText(selectedDate.format(DATE_FORMATTER));
                } else if (dateValue != null) {
                    try {
                        java.sql.Date sqlDate = java.sql.Date.valueOf(dateValue.toString());
                        selectedDate = sqlDate.toLocalDate();
                        txtDate.setText(selectedDate.format(DATE_FORMATTER));
                    } catch (Exception ignore) {
                        selectedDate = LocalDate.now();
                        txtDate.setText(selectedDate.format(DATE_FORMATTER));
                    }
                }
                cmbAttendance.setSelectedItem(model.getValueAt(i, 4).toString());
                Object remarksValue = model.getValueAt(i, 5);
                txtRemarks.setText(remarksValue == null ? "" : remarksValue.toString());
            }
        });
    }

    /**
     * Creates the database and table if they don't already exist.
     */
    void setupDatabase() {
        try {
            // First connect to MySQL without specifying a DB to create it
            Connection tempCon = DriverManager.getConnection(URL, USER, PASS);
            Statement stmt = tempCon.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            tempCon.close();

            // Connect to the actual database
            con = DriverManager.getConnection(URL + DB_NAME, USER, PASS);
            Statement dbStmt = con.createStatement();
            // Create the attendance table if it's not there
            dbStmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS attendance_records (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "student_name VARCHAR(100) NOT NULL," +
                            "attendance_status VARCHAR(30) NOT NULL," +
                            "attendance_date DATE NOT NULL," +
                            "attendance_time VARCHAR(10) NOT NULL," +
                            "remarks TEXT NULL)"
            );

            // Upgrade existing databases: add remarks column if missing
            PreparedStatement colCheck = con.prepareStatement(
                    "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'attendance_records' AND COLUMN_NAME = 'remarks' LIMIT 1"
            );
            colCheck.setString(1, DB_NAME);
            ResultSet colRs = colCheck.executeQuery();
            boolean hasRemarks = colRs.next();
            colRs.close();
            colCheck.close();

            if (!hasRemarks) {
                dbStmt.executeUpdate("ALTER TABLE attendance_records ADD COLUMN remarks TEXT NULL");
            }

            // Upgrade existing databases: add attendance_time column if missing
            PreparedStatement timeColCheck = con.prepareStatement(
                    "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'attendance_records' AND COLUMN_NAME = 'attendance_time' LIMIT 1"
            );
            timeColCheck.setString(1, DB_NAME);
            ResultSet timeColRs = timeColCheck.executeQuery();
            boolean hasAttendanceTime = timeColRs.next();
            timeColRs.close();
            timeColCheck.close();

            if (!hasAttendanceTime) {
                dbStmt.executeUpdate("ALTER TABLE attendance_records ADD COLUMN attendance_time VARCHAR(20) NOT NULL DEFAULT '---'");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database setup failed: " + e.getMessage());
        }
    }

    /**
     * Fetches records from the database and displays them in the table.
     * Supports filtering by student name/status and date.
     */
    void loadData() {
        String searchText = txtSearch == null ? "" : txtSearch.getText().trim();
        String filterDate = txtFilterDate == null ? "" : txtFilterDate.getText().trim();

        // Validate date format before searching
        if (!filterDate.isEmpty() && !isValidDate(filterDate)) {
            JOptionPane.showMessageDialog(this, "Filter date must be in YYYY-MM-DD format.");
            return;
        }

        try {
            model.setRowCount(0); // Clear current table contents

            // Build dynamic SQL query based on filters
            StringBuilder sql = new StringBuilder(
                    "SELECT id, student_name, attendance_status, attendance_date, attendance_time, remarks " +
                            "FROM attendance_records WHERE 1=1"
            );

            if (!searchText.isEmpty()) {
                sql.append(" AND (student_name LIKE ? OR attendance_status LIKE ? OR remarks LIKE ?)");
            }
            if (!filterDate.isEmpty()) {
                sql.append(" AND attendance_date = ?");
            }
            sql.append(" ORDER BY attendance_date DESC, id DESC");

            PreparedStatement pst = con.prepareStatement(sql.toString());
            int parameterIndex = 1;

            // Fill SQL parameters
            if (!searchText.isEmpty()) {
                String keyword = "%" + searchText + "%";
                pst.setString(parameterIndex++, keyword);
                pst.setString(parameterIndex++, keyword);
                pst.setString(parameterIndex++, keyword);
            }
            if (!filterDate.isEmpty()) {
                pst.setDate(parameterIndex, Date.valueOf(filterDate));
            }

            // Execute query and add results to the table model
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("student_name"),
                        rs.getDate("attendance_date"),
                        rs.getString("attendance_time"),
                        rs.getString("attendance_status"),
                        rs.getString("remarks")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load data: " + e.getMessage());
        }
    }

    /**
     * Inserts a new student attendance record into the database.
     */
    void insertData() {
        if (!validateForm(false)) { // Validate fields (ID not required for insert)
            return;
        }

        try {
            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO attendance_records(student_name, attendance_status, attendance_date, attendance_time, remarks) VALUES (?, ?, ?, ?, ?)"
            );
            String status = cmbAttendance.getSelectedItem().toString();
            pst.setString(1, txtName.getText().trim());
            pst.setString(2, status);
            pst.setDate(3, java.sql.Date.valueOf(selectedDate));

            String time;
            if (status.equalsIgnoreCase("Absent") || status.equalsIgnoreCase("Excuse")) {
                time = "---";
            } else {
                time = LocalTime.now().format(TIME_FORMATTER);
            }
            pst.setString(4, time);

            pst.setString(5, txtRemarks.getText().trim());
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Inserted!");
            loadData();    // Refresh table
            clearFields(); // Clear form
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + e.getMessage());
        }
    }

    /**
     * Updates an existing record based on the ID in txtId.
     */
    void updateData() {
        if (!validateForm(true)) { // Validate fields (ID is required for update)
            return;
        }

        try {
            PreparedStatement pst = con.prepareStatement(
                    "UPDATE attendance_records SET student_name=?, attendance_status=?, attendance_date=?, attendance_time=?, remarks=? WHERE id=?"
            );
            String status = cmbAttendance.getSelectedItem().toString();
            pst.setString(1, txtName.getText().trim());
            pst.setString(2, status);
            pst.setDate(3, java.sql.Date.valueOf(selectedDate));

            String time;
            if (status.equalsIgnoreCase("Absent") || status.equalsIgnoreCase("Excuse")) {
                time = "---";
            } else {
                time = LocalTime.now().format(TIME_FORMATTER);
            }
            pst.setString(4, time);

            pst.setString(5, txtRemarks.getText().trim());
            pst.setInt(6, Integer.parseInt(txtId.getText().trim()));

            int updated = pst.executeUpdate();
            if (updated == 0) {
                JOptionPane.showMessageDialog(this, "No record was updated.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Updated!");
            loadData();    // Refresh table
            clearFields(); // Clear form
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Update failed: " + e.getMessage());
        }
    }

    /**
     * Deletes the currently selected record from the database.
     */
    void deleteData() {
        if (txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a record first before deleting.");
            return;
        }

        try {
            PreparedStatement pst = con.prepareStatement(
                    "DELETE FROM attendance_records WHERE id=?"
            );
            pst.setInt(1, Integer.parseInt(txtId.getText().trim()));

            int deleted = pst.executeUpdate();
            if (deleted == 0) {
                JOptionPane.showMessageDialog(this, "No record was deleted.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Deleted!");
            loadData();    // Refresh table
            clearFields(); // Clear form
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
        }
    }

    /**
     * Validates that the input form is filled out correctly.
     * @param requireId True if we are updating/deleting and need a selected ID.
     */
    boolean validateForm(boolean requireId) {
        if (requireId && txtId.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a record first before updating.");
            return false;
        }

        if (txtName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Student name is required.");
            return false;
        }

        if (selectedDate == null) {
            JOptionPane.showMessageDialog(this, "Attendance date is required.");
            return false;
        }

        return true;
    }

    /**
     * Helper to check if a string is a valid SQL Date (YYYY-MM-DD).
     */
    boolean isValidDate(String value) {
        try {
            Date.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resets the input fields and clears table selection.
     */
    void clearFields() {
        txtId.setText("");
        txtName.setText("");
        selectedDate = LocalDate.now();
        txtDate.setText(selectedDate.format(DATE_FORMATTER));
        txtRemarks.setText("");
        cmbAttendance.setSelectedIndex(0);
        table.clearSelection();
    }

    static class CalendarPopup {
        static LocalDate pickDate(Component parent, LocalDate initial) {
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
                grid.removeAll();
                YearMonth ym = shownMonth[0];
                monthLabel.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + ym.getYear());

                DayOfWeek[] week = new DayOfWeek[]{
                        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
                };
                for (DayOfWeek dow : week) {
                    JLabel lbl = new JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                    grid.add(lbl);
                }

                LocalDate first = ym.atDay(1);
                int firstDayIndex = first.getDayOfWeek().getValue() % 7; // Sunday = 0
                for (int i = 0; i < firstDayIndex; i++) {
                    grid.add(new JLabel(""));
                }

                LocalDate today = LocalDate.now();
                int length = ym.lengthOfMonth();
                for (int day = 1; day <= length; day++) {
                    LocalDate d = ym.atDay(day);
                    JButton b = new JButton(String.valueOf(day));
                    if (d.equals(today)) {
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

        Runnable attemptLogin = () -> {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Username and password are required.");
                return;
            }

            if (LOGIN_USERNAME.equals(username) && LOGIN_PASSWORD.equals(password)) {
                authenticated[0] = true;
                dialog.dispose();
                return;
            }

            JOptionPane.showMessageDialog(dialog, "Invalid username or password.");
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
        };

        btnLogin.addActionListener(e -> attemptLogin.run());
        btnCancel.addActionListener(e -> {
            authenticated[0] = false;
            dialog.dispose();
        });

        txtUsername.addActionListener(e -> attemptLogin.run());
        txtPassword.addActionListener(e -> attemptLogin.run());

        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(btnLogin);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);

        SwingUtilities.invokeLater(txtUsername::requestFocusInWindow);
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

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        // Set global font size for better accessibility
        setUIFont(new javax.swing.plaf.FontUIResource(new Font("Arial", Font.PLAIN, 18)));

        // Run the GUI on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            if (!showLoginDialog()) {
                System.exit(0);
                return;
            }
            new CRUD_GUI().setVisible(true);
        });
    }
}
