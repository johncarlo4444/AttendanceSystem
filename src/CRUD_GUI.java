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

/**
 * CRUD_GUI - A Student Attendance Monitoring System application.
 * This class provides a Graphical User Interface (GUI) to Create, Read, Update, and Delete (CRUD) 
 * student attendance records stored in a MySQL database.
 */
public class CRUD_GUI extends JFrame {

    // UI Components for data entry and display
    JTextField txtId;           // Displays the unique record ID (auto-generated)
    JTextField txtName;         // Field to enter student name
    JTextField txtDate;         // Field to enter attendance date (YYYY-MM-DD)
    JTextField txtSearch;       // Field to search records by name or status
    JTextField txtFilterDate;   // Field to filter records by a specific date
    JComboBox<String> cmbAttendance; // Dropdown to select attendance status (Present/Absent)
    JTable table;               // Table to display records from the database
    DefaultTableModel model;    // Data model for the JTable

    // Database connection constants
    static final String URL = "jdbc:mysql://localhost:3306/"; // MySQL server address
    static final String DB_NAME = "crud_gui_db";             // Database name
    static final String USER = "root";                        // Database username
    static final String PASS = "Administrator.123";           // Database password

    Connection con; // Connection object to interact with MySQL

    public CRUD_GUI() {
        // Basic window setup
        setTitle("Java-MySQL Based Attendance Monitoring System for College Students at MSEUF-Candelaria");
        setSize(900, 650);
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
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // formPanel: Input fields organized in a grid (Label next to TextField)
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));

        txtId = new JTextField();
        txtId.setEnabled(false); // ID is read-only as it's auto-incremented in DB
        txtName = new JTextField();
        txtDate = new JTextField();
        cmbAttendance = new JComboBox<>(new String[]{"Present", "Absent"});

        formPanel.add(new JLabel("ID:"));
        formPanel.add(txtId);
        formPanel.add(new JLabel("Student Name:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        formPanel.add(txtDate);
        formPanel.add(new JLabel("Attendance:"));
        formPanel.add(cmbAttendance);

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
        model = new DefaultTableModel(new String[]{"ID", "Student Name", "Date", "Attendance"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent users from editing directly in the table
            }
        };
        table = new JTable(model);
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

        // Table Mouse Listener: Click a row to populate the input form for editing
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int i = table.getSelectedRow();
                if (i < 0) {
                    return;
                }

                txtId.setText(model.getValueAt(i, 0).toString());
                txtName.setText(model.getValueAt(i, 1).toString());
                txtDate.setText(model.getValueAt(i, 2).toString());
                cmbAttendance.setSelectedItem(model.getValueAt(i, 3).toString());
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
                            "attendance_status VARCHAR(20) NOT NULL," +
                            "attendance_date DATE NOT NULL)"
            );
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
                    "SELECT id, student_name, attendance_status, attendance_date " +
                            "FROM attendance_records WHERE 1=1"
            );

            if (!searchText.isEmpty()) {
                sql.append(" AND (student_name LIKE ? OR attendance_status LIKE ?)");
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
                        rs.getString("attendance_status")
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
                    "INSERT INTO attendance_records(student_name, attendance_status, attendance_date) VALUES (?, ?, ?)"
            );
            pst.setString(1, txtName.getText().trim());
            pst.setString(2, cmbAttendance.getSelectedItem().toString());
            pst.setDate(3, Date.valueOf(txtDate.getText().trim()));
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
                    "UPDATE attendance_records SET student_name=?, attendance_status=?, attendance_date=? WHERE id=?"
            );
            pst.setString(1, txtName.getText().trim());
            pst.setString(2, cmbAttendance.getSelectedItem().toString());
            pst.setDate(3, Date.valueOf(txtDate.getText().trim()));
            pst.setInt(4, Integer.parseInt(txtId.getText().trim()));

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

        if (txtDate.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Attendance date is required.");
            return false;
        }

        if (!isValidDate(txtDate.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format.");
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
        txtDate.setText("");
        cmbAttendance.setSelectedIndex(0);
        table.clearSelection();
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> new CRUD_GUI().setVisible(true));
    }
}
