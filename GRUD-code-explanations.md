# CRUD GUI Reviewer


## 1. Compact and Concise

chronological parts from:

- `CRUD_GUI.java`
- `AppTheme.java`
- `TeacherDashboard.java`
- `AttendanceSummaryWindow.java`

 compact and concise.

---

## 2. Import Section

### Code

```java
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
```

### Explanation

This import section prepares the file to use GUI, database, date/time, and validation tools.

- `javax.swing.*` imports Swing components like `JFrame`, `JButton`, `JLabel`, `JTable`, `JTextField`, and `JOptionPane`.
- `Border` is used for borders around components.
- `DefaultTableModel` manages data inside a `JTable`.
- `java.awt.*` imports GUI design tools like `Color`, `Font`, `Dimension`, and layout managers.
- `ComponentAdapter` and `ComponentEvent` handle component events like window resizing.
- `MouseAdapter` and `MouseEvent` handle mouse actions like clicking and hovering.
- SQL imports connect Java to MySQL:
  - `Connection` stores the database connection.
  - `DriverManager` opens the connection.
  - `PreparedStatement` runs safer SQL with `?` placeholders.
  - `ResultSet` stores query results.
  - `Statement` runs simple SQL commands.
  - `Date` stores SQL-compatible dates.
- Time imports:
  - `LocalDate` is date only.
  - `LocalTime` is time only.
  - `DayOfWeek` represents days like Monday or Tuesday.
  - `YearMonth` represents a year and month.
- `DateTimeFormatter` and `TextStyle` format dates, months, and day names.
- `Locale` controls language/region formatting.
- `Pattern` is used for regex validation, such as checking names or IDs.

---

## 3. Class Declaration, Constants, And GUI Fields

### Code

```java
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
```

### Explanation

`public class CRUD_GUI extends JFrame` declares a class named `CRUD_GUI`. Because it extends `JFrame`, this class behaves like a GUI window.

`static final` values are constants:

- `TIME_FORMATTER` formats time like `08:30 AM`.
- `DATE_FORMATTER` formats dates like `2026-05-03`.
- `WINDOW_MARGIN` controls spacing around the window.
- `RESPONSIVE_BREAKPOINT` is likely used to switch layouts when the window width changes.

The `JTextField` variables are text inputs for record ID, student ID, name, date, remarks, search, and filter date.

The `JComboBox<String>` variables are dropdowns for attendance status, subject entry, attendance filter, and subject filter.

`JTable table` displays records, while `DefaultTableModel model` stores and manages the table data.

---

## 4. Selected Date, Database Constants, Login, Arrays, And Panels

### Code

```java
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
```

### Explanation

`selectedDate` stores the selected attendance date and starts as today. `selectedSubject` starts as `"General"`.

The database constants define the MySQL connection:

- `URL` is the MySQL server location.
- `DB_NAME` is the database name.
- `USER` and `PASS` are the MySQL credentials.

`LOGIN_USERNAME` and `LOGIN_PASSWORD` are hardcoded login credentials. This is simple for school projects, but real systems should store passwords securely.

`ATTENDANCE_OPTIONS` stores attendance choices. `SUBJECTS` stores subject choices. These arrays are reused in dropdowns, filters, dashboard, and summary windows.

`Connection con` stores the active database connection. `databaseReady` tracks if the database is ready.

The `JPanel` variables are main GUI containers. `BorderLayout(10, 10)` arranges components with gaps. `stackedLayout` likely tracks whether the layout is side-by-side or stacked for smaller windows.

---

## 5. Constructor: Main Window Setup

### Code

```java
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
```

### Explanation

The constructor runs when a `CRUD_GUI` window is created.

- `AppTheme.install()` applies the app theme.
- `setTitle()` sets the window title.
- `setSize(1250, 820)` sets the window size.
- `setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)` exits the app when closed.
- `setLocationRelativeTo(null)` centers the window.

`mainPanel` is the main container. It uses `BorderLayout` and receives padding using `BorderFactory.createEmptyBorder(...)`.

`setContentPane(mainPanel)` makes `mainPanel` the window's main content area.

`titleLabel` creates and styles the title, then adds it to the top using `BorderLayout.NORTH`.

`buildCrudPanel()` and `buildRightPanel()` build the main sections. `responsiveBody` is added to the center.

---

## 6. Constructor: Database Loading And Responsive Layout

### Code

```java
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
```

### Explanation

`setupDatabase()` prepares the MySQL database. If `databaseReady` is true, `loadData()` loads records into the table.

`addComponentListener(new ComponentAdapter() {...})` listens for window resizing. When resized, `componentResized()` calls `updateResponsiveLayout()`.

`@Override` means the method replaces a method from the parent class.

`SwingUtilities.invokeLater(this::updateResponsiveLayout)` runs the layout update after Swing finishes creating the GUI. `this::updateResponsiveLayout` is a method reference, similar to `() -> updateResponsiveLayout()`.

---

## 7. Build CRUD Panel Start

### Code

```java
private void buildCrudPanel() {
    // this panel contains the main create read update and delete workflow for attendance records
    AppTheme.stylePanel(crudPanel);
```

### Explanation

`buildCrudPanel()` builds the CRUD area of the window.

`private` means it can only be used inside the same class.

CRUD means Create, Read, Update, and Delete.

`AppTheme.stylePanel(crudPanel)` applies the app's theme to the CRUD panel before fields, buttons, and the table are added.

---

## 8. Form Listeners

### Code

```java
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
```

### Explanation

`txtName.addActionListener(e -> autofillStudentIdentifier())` runs when the user presses Enter in the name field.

`addFocusListener(...)` runs `autofillStudentIdentifier()` when the user leaves the name field. `focusLost()` means the field is no longer active.

`txtDate.addMouseListener(...)` opens a calendar when the date field is clicked.

`CalendarPopup.pickDate(CRUD_GUI.this, selectedDate)` opens the custom calendar. `CRUD_GUI.this` refers to the current main window.

If a date is picked, `selectedDate` is updated and `updateDateField()` refreshes the visible date text.

Subject and filter dropdowns use action listeners:

- Subject entry updates `selectedSubject`.
- Attendance and subject filters call `loadData()` to refresh the table.

---

## 9. Table Model

### Code

```java
model = new DefaultTableModel(new String[]{"Record ID", "Student ID", "Student Name", "Subject", "Date", "Time In", "Time Out", "Attendance", "Remarks"}, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
};
```

### Explanation

This creates the table model for attendance records.

The string array defines the column names. The `0` means the table starts with zero rows.

`isCellEditable()` returns `false`, so users cannot directly edit table cells.

The syntax `new DefaultTableModel(...) { ... }` creates an anonymous subclass so the code can override the default editable behavior.

---

## 10. Table Row Click Listener

### Code

```java
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
```

### Explanation

This listener fills the form when the user clicks a table row.

`table.getSelectedRow()` gets the selected row index. If it is less than `0`, no row is selected and the method stops.

`model.getValueAt(i, column)` gets values from the selected row:

- `0` record ID
- `1` student ID
- `2` student name
- `3` subject
- `4` date
- `7` attendance status
- `8` remarks

The date is handled carefully because it might be a `java.util.Date` object or a text value. `instanceof` checks the data type.

The `try-catch` prevents crashing if a date cannot be parsed.

The ternary expression for remarks means: if remarks are `null`, show blank text; otherwise convert remarks to text.

---

## 11. Database Setup

### Code

```java
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
```

### Explanation

`setupDatabase()` prepares MySQL for the app.

It first connects to the MySQL server, creates the database if missing, then reconnects directly to that database.

It creates two tables:

- `students` for student names and IDs.
- `attendance_records` for attendance entries.

`ensureStudentsTableSchema(...)` and `ensureColumnExists(...)` update older database structures by adding missing columns.

`migrateExistingStudents()` moves older records into the newer student ID system.

If setup succeeds, `databaseReady = true`. If an error happens, the app shows a message and sets `databaseReady = false`.

---

## 12. Load Data With Filters

### Code

```java
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
```

### Explanation

`loadData()` refreshes the table from the database.

It first checks if the database is ready. Then it reads optional filters: search text, date, attendance status, and subject.

If a filter date is typed, `isValidDate()` makes sure it follows `YYYY-MM-DD`.

`model.setRowCount(0)` clears the table before loading fresh records.

`StringBuilder sql` builds a dynamic SQL query. `WHERE 1=1` makes it easy to append optional `AND` conditions.

`PreparedStatement` safely fills `?` placeholders. `parameterIndex++` uses the current placeholder number, then increases it.

`LIKE` with `%keyword%` means "contains this text".

`ResultSet rs = pst.executeQuery()` runs the query. `while (rs.next())` loops through each row and adds it to the table model.

---

## 13. Insert Data: Time In And Time Out

### Code

```java
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
```

### Explanation

`insertData()` saves attendance. It handles both time in and time out.

It stops if the database is not ready or the form is invalid.

It gets the student name, finds or creates the student ID, gets the selected status, and gets the selected subject.

The first query checks if the student already has a record for the same date and subject.

If no record exists, it inserts a new record as time in.

If a record already exists, it treats the action as time out. It prevents time out if time in is missing, and prevents duplicate time out if the student has already timed out.

After saving, it reloads the table and clears the form.

---

## 14. Update Data

### Code

```java
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
```

### Explanation

`updateData()` updates an existing attendance record.

It requires a selected record ID, so it uses `validateForm(true)`.

It finds or creates the student ID, then checks for duplicates using:

```java
id <> ?
```

This means "exclude the current record being edited."

If another record already exists for the same student, date, and subject, the update is stopped.

The update query changes student ID, name, subject, attendance status, date, and remarks. It does not change time in or time out.

`executeUpdate()` returns the number of updated rows. If greater than `0`, the app shows success, reloads the table, and clears the form.

---

## 15. Delete Data

### Code

```java
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
```

### Explanation

`deleteData()` deletes the selected attendance record.

It first checks if the database is ready and if a record is selected.

`JOptionPane.showConfirmDialog(...)` asks for confirmation before deleting. If the user does not choose Yes, the method stops.

The SQL command deletes the record by ID:

```sql
DELETE FROM attendance_records WHERE id=?
```

After successful deletion, it reloads the table and clears the form.

---

## 16. Validate Form

### Code

```java
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
```

### Explanation

`validateForm()` checks if the form is valid before saving or updating.

It returns `true` if valid and `false` if invalid.

If `requireId` is true, a selected record ID is required. This is used for updates.

It also checks that the student name is not empty and follows:

```text
Surname, Firstname
```

---

## 17. Validate Date

### Code

```java
boolean isValidDate(String value) {
    try {
        Date.valueOf(value);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

### Explanation

`isValidDate()` checks if a text value is a valid SQL date.

Expected format:

```text
YYYY-MM-DD
```

`Date.valueOf(value)` attempts to convert the text to a SQL date. If it succeeds, the method returns `true`. If it fails, the `catch` block returns `false`.

---

## 18. Clear Fields

### Code

```java
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
```

### Explanation

`clearFields()` resets the form.

It clears text fields, resets the date to today, resets subject to `"General"`, clears remarks, sets the attendance dropdown to the first option, and removes table selection.

---

## 19. Date Field, Name Format, And Time In

### Code

```java
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
```

### Explanation

`updateDateField()` displays `selectedDate` using `DATE_FORMATTER`, such as `2026-05-03`.

`isValidStudentNameFormat()` checks this format:

```text
Surname, Firstname
```

Regex notes:

- `[A-Za-z .'-]+` allows letters, spaces, dots, apostrophes, and hyphens.
- `,` requires a comma.
- `\\s*` allows spaces after the comma.

`isNonTimedStatus()` returns true for `Absent` or `Excuse`, ignoring capitalization.

`resolveTimeIn()` returns blank for non-timed statuses. Otherwise, it returns the current time formatted like `08:30 AM`.

---

## 20. Time Out, Reset Filters, Dropdown Options, And Database Check

### Code

```java
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
```

### Explanation

`resolveTimeOut()` works like `resolveTimeIn()`. It returns blank for `Absent` or `Excuse`, otherwise it returns the current formatted time.

`resetFilters()` clears search and filter controls, then reloads the table.

`prependAllOption()` creates a new array with `"All"` added at the beginning. `System.arraycopy(...)` copies the original array into the new one starting at index `1`.

`isDatabaseReady()` returns true only if `databaseReady` is true and `con` is not null. Otherwise, it shows a database warning and returns false.

---

## 21. Value Helper And Opening Other Windows

### Code

```java
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
```

### Explanation

`valueOf(JTextField field)` safely gets text from a field. If the field is null, it returns an empty string.

`openTeacherDashboard()` opens the teacher dashboard only if the database is ready.

`openAttendanceSummary()` opens the attendance summary window only if the database is ready.

Both windows receive `con`, so they can use the same database connection.

---

## 22. Ensure Column And Index Exist

### Code

```java
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
```

### Explanation

`ensureColumnExists()` checks MySQL metadata to see if a column exists. If it does not, it runs the `ALTER TABLE` SQL.

It uses `INFORMATION_SCHEMA.COLUMNS`, which stores table column information.

`ensureIndexExists()` does the same for indexes using `INFORMATION_SCHEMA.STATISTICS`.

Indexes help speed up searches and can also enforce uniqueness.

Both methods close their `ResultSet` and `PreparedStatement` after use.

---

## 23. Students Table Schema And Backfill

### Code

```java
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
```

### Explanation

`ensureStudentsTableSchema()` makes sure the `students` table has the needed columns for automatic student ID generation.

It checks for:

- `student_name`
- `student_identifier`

Then it fills missing identifiers and creates a unique index for `student_identifier`.

`backfillStudentIdentifiers()` finds old student rows with missing or blank student IDs. For each row, it generates an ID and updates that student record.

Note: `dbStmt` is passed to `ensureStudentsTableSchema()`, but in this shown code it is not used.

---

## 24. Autofill Student Identifier

### Code

```java
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
```

### Explanation

`autofillStudentIdentifier()` automatically fills the student ID field based on the typed student name.

If the name is empty, it clears the ID field and stops.

It searches the database using `findStudentIdentifierByName(name)`.

The ternary expression means: if no ID is found, show blank; otherwise show the found ID.

---

## 25. Ensure And Find Student Identifier

### Code

```java
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
```

### Explanation

`ensureStudentIdentifierForName()` makes sure a student has a student ID.

It first checks if the student already exists. If yes, it returns the existing ID. If not, it generates a new ID, inserts the student into the database, and returns the generated ID.

`findStudentIdentifierByName()` searches the `students` table by student name.

`return rs.next() ? rs.getString("student_identifier") : null;` means:

- If a record exists, return the student ID.
- If none exists, return `null`.

---

## 26. Find Student Name And Migrate Existing Students

### Code

```java
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
```

### Explanation

`findStudentNameByIdentifier()` searches for a student name using a student ID.

`migrateExistingStudents()` updates old attendance data to use the newer student ID system.

It selects unique student names from old attendance records using `DISTINCT`, checks if each student already has an ID, creates one if needed, then updates attendance records that have missing student IDs.

---

## 27. Generate Automatic Student Identifier

### Code

```java
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
```

### Explanation

The no-argument version gets the name from `txtName` and passes it to the main generator.

The main method creates a student ID using:

- A name-based prefix.
- The last two digits of the current year.
- A two-digit running number.

Example idea:

```text
DE2601
```

`String.format("%02d", number)` makes sure numbers use two digits, such as `01`.

The SQL query finds the latest existing ID with the same base. The code extracts the numeric part using `substring(base.length())`, converts it to an integer, and adds `1`.

The `do-while` loop keeps generating candidates until it finds one that is not already used.

If the number goes past `99`, it throws an error.

---

## 28. Build Student ID Prefix And Legacy Method

### Code

```java
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
```

### Explanation

`buildStudentIdPrefix()` creates the letter prefix for automatic student IDs.

The regex `[^A-Za-z]` means "anything that is not a letter." `replaceAll("")` removes those characters.

Example:

```text
Dela Cruz, Juan -> DELACRUZJUAN -> DE
```

If there are at least two letters, it returns the first two. If there is one letter, it adds `X`. If there are no letters, it returns `XX`.

`generateLegacyStudentIdentifier()` is for compatibility with older code. It accepts `studentName`, but the shown implementation just calls the automatic generator.

---

## 29. Calendar Popup

### Code

```java
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
```

### Explanation

`CalendarPopup` is an inner helper class for selecting dates.

`pickDate()` opens a modal calendar dialog and returns the selected `LocalDate`. If the user cancels, it returns `null`.

`JDialog` creates the popup. `APPLICATION_MODAL` means the user must close the popup before using the main window again.

`LocalDate[] result` and `YearMonth[] shownMonth` use arrays because lambdas can modify array contents, while ordinary local variables used in lambdas must be effectively final.

The calendar has:

- A header with previous and next buttons.
- A month label.
- A grid with seven columns.
- A cancel button.

`Runnable rebuild = () -> {...}` defines reusable code that rebuilds the calendar whenever the visible month changes.

`firstDayIndex` calculates where day 1 should appear. `getValue()` returns Monday as `1` and Sunday as `7`, so `% 7` makes Sunday become `0`.

Each day is a button. Clicking it saves the selected date and closes the dialog.

---

## 30. Login Dialog

### Code

```java
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
```

### Explanation

`showLoginDialog()` displays a login popup before opening the system.

It returns `true` if login succeeds, otherwise `false`.

`new JDialog((Frame) null, "Login", true)` creates a modal login dialog. The `true` value makes it modal.

`GridBagLayout` and `GridBagConstraints` arrange the username and password form fields.

`JPasswordField` hides typed password characters.

`final boolean[] authenticated = {false};` uses an array so the lambda can change the value.

The login button checks the typed username and password against `LOGIN_USERNAME` and `LOGIN_PASSWORD`. If correct, it sets `authenticated[0] = true` and closes the dialog. If incorrect, it shows an error.

Cancel closes the dialog without authentication.

---

## 31. Set UI Font And Main Method

### Code

```java
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
```

### Explanation

`setUIFont()` changes the default font of Swing components.

`UIManager.getDefaults().keys()` gets all default UI setting keys. The loop checks each setting, and if it is a font, replaces it with the given font.

`main()` is the Java program entry point.

It sets the global font to Arial, plain, size 18.

`SwingUtilities.invokeLater(...)` runs the GUI startup on Swing's Event Dispatch Thread, which is the safe thread for Swing UI code.

The app shows the login dialog first. If login succeeds, it opens the main `CRUD_GUI` window. Otherwise, it exits.

---

## 32. Repetitive And Most Used Syntaxes

### Access Modifiers

`public` means accessible from other classes.

Example:

```java
public class CRUD_GUI
```

`private` means accessible only inside the same class.

Example:

```java
private boolean stackedLayout;
```

### Class And Object Keywords

`class` creates a blueprint for objects.

```java
public class CRUD_GUI
```

`extends` means inheritance.

```java
extends JFrame
```

This makes `CRUD_GUI` a kind of `JFrame`.

`new` creates an object.

```java
new JButton("Login")
```

`this` refers to the current object.

```java
JOptionPane.showMessageDialog(this, "Updated!");
```

### Method Return Types

`void` means the method does not return a value.

```java
void loadData()
```

`boolean` stores or returns `true` or `false`.

```java
boolean databaseReady = false;
```

`String` stores text.

```java
String selectedSubject = "General";
```

`int` stores whole numbers.

```java
int updated = pst.executeUpdate();
```

### Variable Modifiers

`static` means the member belongs to the class itself, not a specific object.

```java
static final String USER = "root";
```

`final` means the value cannot be changed after assignment.

```java
static final String DB_NAME = "crud_gui_db";
```

`static final` is commonly used for constants.

```java
static final int WINDOW_MARGIN = 18;
```

### Control Flow

`if` runs code only when a condition is true.

```java
if (!isDatabaseReady()) {
    return;
}
```

`else` runs when the `if` condition is false.

`return` stops a method. It can also return a value.

```java
return false;
```

For `void` methods:

```java
return;
```

### Exception Handling

`try` contains code that might cause an error.

`catch` handles the error.

```java
try {
    loadData();
} catch (Exception e) {
    JOptionPane.showMessageDialog(this, e.getMessage());
}
```

`throws Exception` means the method may pass an error to the caller.

```java
String findStudentNameByIdentifier(String id) throws Exception
```

### Object And Type Checking

`instanceof` checks if a value is a certain type.

```java
if (dateValue instanceof java.util.Date)
```

`null` means no object or no value.

```java
if (studentIdentifier == null)
```

### Operators

`=` assigns a value.

```java
String name = "Juan";
```

`==` compares primitive values like numbers or booleans.

`.equals()` compares object values like strings.

```java
LOGIN_USERNAME.equals(username)
```

`!` means not.

```java
if (!databaseReady)
```

`&&` means AND. Both conditions must be true.

```java
if (databaseReady && con != null)
```

`||` means OR. At least one condition must be true.

```java
if (status.equals("Absent") || status.equals("Excuse"))
```

### Loops

`while` repeats while a condition is true.

```java
while (rs.next()) {
}
```

`for` repeats a known number of times.

```java
for (int day = 1; day <= length; day++)
```

`do while` runs once first, then repeats while the condition is true.

```java
do {
} while (condition);
```

### Arrays

`String[]` means an array of text values.

```java
static final String[] ATTENDANCE_OPTIONS = {"Present", "Absent", "Late", "Excuse"};
```

`values.length` gets the number of items in an array.

### Generics

`JComboBox<String>` means the combo box stores `String` values.

This helps Java know the dropdown items are text.

### Lambda Expressions

```java
e -> loadData()
```

This is a short event handler. It means when the event happens, run `loadData()`.

### Anonymous Classes

```java
new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
    }
}
```

This creates a temporary class directly in place, commonly used for GUI events.

### `@Override`

`@Override` means the method replaces a method from a parent class.

```java
@Override
public void mouseClicked(MouseEvent e)
```

### PreparedStatement

```java
PreparedStatement pst = con.prepareStatement("SELECT * FROM table WHERE id=?");
pst.setInt(1, id);
```

`?` is a placeholder. Values are inserted with methods like:

- `setString()`
- `setInt()`
- `setDate()`

This is safer than directly joining user input into SQL.

### ResultSet

```java
ResultSet rs = pst.executeQuery();
while (rs.next()) {
    String name = rs.getString("student_name");
}
```

`ResultSet` stores query results. `rs.next()` moves to the next row.

### Ternary Operator

```java
condition ? valueIfTrue : valueIfFalse
```

Example:

```java
return field == null ? "" : field.getText().trim();
```

This means: if the field is null, return blank; otherwise return trimmed field text.

### Common Naming Pattern

- `txt` means text field.
- `cmb` means combo box.
- `btn` means button.
- `lbl` means label.
- `pst` means prepared statement.
- `rs` means result set.

These prefixes make the code easier to read.

