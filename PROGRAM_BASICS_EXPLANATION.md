# Attendance System Program Basics Explanation

This guide explains the basic Java concepts used in the attendance system based on these files:

- `src/CRUD_GUI.java`
- `src/AppTheme.java`
- `src/AttendanceSummaryWindow.java`
- `src/TeacherDashboard.java`

## How The Files Connect

`CRUD_GUI.java` is the main file of the program. It starts the application, shows the login dialog, builds the main attendance CRUD window, connects to MySQL, and opens the other windows.

The program starts in `CRUD_GUI.java` at `main()` near line 1117:

```java
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

`AppTheme.java` is a helper file for design. It does not store attendance data. It only styles panels, buttons, input fields, tables, and borders.

`TeacherDashboard.java` is opened from the main CRUD window. It receives the same database connection from `CRUD_GUI.java`.

`AttendanceSummaryWindow.java` is also opened from the main CRUD window. It also receives the same database connection from `CRUD_GUI.java`.

The main connection flow is:

```text
CRUD_GUI.java
  -> creates database connection
  -> opens TeacherDashboard.java using new TeacherDashboard(con)
  -> opens AttendanceSummaryWindow.java using new AttendanceSummaryWindow(con)
  -> uses AppTheme.java for design
```

## Basic Data Types Used

### String

`String` is used for text values such as database URL, username, password, student name, subject, status, and remarks.

Examples from `CRUD_GUI.java`:

```java
static final String URL = "jdbc:mysql://localhost:3306/";
static final String DB_NAME = "crud_gui_db";
static final String USER = "root";
static final String PASS = "Administrator.123";
```

It is also used when reading text fields:

```java
String studentName = txtName.getText().trim();
String searchText = txtSearch == null ? "" : txtSearch.getText().trim();
```

### int

`int` is used for numbers such as margins, breakpoints, table indexes, row indexes, and database IDs.

Examples:

```java
static final int WINDOW_MARGIN = 18;
static final int RESPONSIVE_BREAKPOINT = 980;
int parameterIndex = 1;
```

### boolean

`boolean` stores true or false values.

Example from `CRUD_GUI.java`:

```java
boolean databaseReady = false;
```

This tells the program if the database connection is ready.

### LocalDate, LocalTime, YearMonth

These are Java date and time data types.

Examples:

```java
LocalDate selectedDate = LocalDate.now();
LocalTime.now().format(TIME_FORMATTER);
YearMonth.from(initial == null ? LocalDate.now() : initial);
```

`LocalDate` is used for attendance dates. `LocalTime` is used for time in and time out. `YearMonth` is used by the calendar popup.

### Connection, PreparedStatement, ResultSet

These are database-related data types from `java.sql`.

```java
Connection con;
PreparedStatement pst = con.prepareStatement(sql.toString());
ResultSet rs = pst.executeQuery();
```

`Connection` represents the MySQL connection. `PreparedStatement` prepares an SQL command. `ResultSet` stores the records returned by a SELECT query.

## Arrays Used

Arrays store multiple values of the same type.

In `CRUD_GUI.java`, the attendance options and subject options are stored in `String[]` arrays:

```java
static final String[] ATTENDANCE_OPTIONS = {"Present", "Absent", "Late", "Excuse"};
static final String[] SUBJECTS = {
    "General / Events / All Day",
    "Object Oriented Programming",
    "CpE as a Discipline",
    "Differential Equations",
    "Art Appreciation",
    "Computer-Aided Drafting",
    "Data Structures and Algorithms",
    "Discrete Mathematics",
    "Purposive Communication"
};
```

These arrays are used in combo boxes:

```java
cmbAttendance = new JComboBox<>(ATTENDANCE_OPTIONS);
cmbSubjectEntry = new JComboBox<>(SUBJECTS);
```

`TeacherDashboard.java` and `AttendanceSummaryWindow.java` also reuse these arrays:

```java
new JComboBox<>(prependAll(CRUD_GUI.ATTENDANCE_OPTIONS));
new JComboBox<>(prependAll(CRUD_GUI.SUBJECTS));
```

This means the subject and attendance choices are centralized in `CRUD_GUI.java`.

## ArrayList And List Handling

The program uses `ArrayList` when the number of values can change while the program is running.

In `AttendanceSummaryWindow.java`, a list of dates is created for the weekly or monthly report:

```java
List<LocalDate> dates = new ArrayList<>();
for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
    dates.add(date);
}
```

This means the program creates a flexible list of dates between `startDate` and `endDate`.

It also uses a parameter list for SQL values:

```java
List<Object> params = new ArrayList<>();
params.add(Date.valueOf(startDate));
params.add(Date.valueOf(endDate));
```

Then the values are assigned to the `PreparedStatement` using a loop:

```java
for (int i = 0; i < params.size(); i++) {
    Object value = params.get(i);
    if (value instanceof Date) {
        pst.setDate(i + 1, (Date) value);
    } else {
        pst.setObject(i + 1, value);
    }
}
```

`TeacherDashboard.java` uses the same idea. It builds a `List<Object> params` and passes it to `bindParameters()`.

## Map Handling

`AttendanceSummaryWindow.java` uses a `Map` to group attendance records by student.

```java
Map<String, String[]> rows = new LinkedHashMap<>();
```

The key is based on student ID and student name. The value is a `String[]` row for the summary table.

```java
String[] row = rows.computeIfAbsent(key, ignored -> {
    String[] values = new String[2 + dates.size()];
    values[0] = studentIdentifier;
    values[1] = studentName;
    for (int col = 2; col < values.length; col++) {
        values[col] = "";
    }
    return values;
});
```

This lets the report show one row per student instead of repeating the student many times.

## Operators Used

### Assignment Operator

The `=` operator assigns a value.

```java
databaseReady = false;
selectedSubject = "General";
selectedDate = picked;
```

### Comparison Operators

Comparison operators check conditions.

```java
if (databaseReady && con != null) {
```

Examples:

- `==` means equal
- `!=` means not equal
- `>` means greater than
- `<` means less than
- `>=` means greater than or equal
- `<=` means less than or equal

### Logical Operators

Logical operators combine conditions.

```java
if (databaseReady && con != null) {
```

`&&` means AND. Both conditions must be true.

```java
if (currentTimeOut != null && !currentTimeOut.trim().isEmpty()) {
```

`!` means NOT.

### String Concatenation

The `+` operator combines text.

```java
JOptionPane.showMessageDialog(this, "Database setup failed: " + e.getMessage());
```

## If Else Statements

`if` statements are used to make decisions.

Example from database checking:

```java
if (databaseReady && con != null) {
    return true;
}
JOptionPane.showMessageDialog(this, "Database is not ready.");
return false;
```

Example from login:

```java
if (LOGIN_USERNAME.equals(txtUsername.getText().trim()) &&
        LOGIN_PASSWORD.equals(new String(txtPassword.getPassword()))) {
    authenticated[0] = true;
    dialog.dispose();
} else {
    JOptionPane.showMessageDialog(dialog, "Invalid credentials.");
}
```

Example from insert/time-in logic:

```java
if (!rs.next()) {
    // insert new attendance record
} else {
    // update existing record with time out
}
```

## Loops Used

### for Loop

`for` loops are used when the program needs to repeat a task.

Example from `CRUD_GUI.java` button styling:

```java
for (AbstractButton button : buttons) {
    AppTheme.styleButton(button);
}
```

Example from `AttendanceSummaryWindow.java` date generation:

```java
for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
    dates.add(date);
}
```

Example from the calendar popup:

```java
for (int day = 1; day <= length; day++) {
    LocalDate d = ym.atDay(day);
    JButton b = new JButton(String.valueOf(day));
}
```

### while Loop

`while` loops are used when reading database records from a `ResultSet`.

Example from `CRUD_GUI.java`:

```java
ResultSet rs = pst.executeQuery();
while (rs.next()) {
    model.addRow(new Object[]{
        rs.getInt("id"),
        rs.getString("student_identifier"),
        rs.getString("student_name")
    });
}
```

The loop continues while there are database records to read.

### do while Loop

`CRUD_GUI.java` uses a `do while` loop when generating a student ID to make sure the generated ID is unique.

```java
do {
    candidate = base + String.format("%02d", nextNumber++);
} while (findStudentNameByIdentifier(candidate) != null);
```

## Input Handling

The program gets user input from Swing components such as `JTextField`, `JComboBox`, and `JPasswordField`.

### Text Fields

Text fields are declared in `CRUD_GUI.java`:

```java
JTextField txtName;
JTextField txtDate;
JTextField txtRemarks;
JTextField txtSearch;
JTextField txtFilterDate;
```

The program gets the value using `getText()`:

```java
String studentName = txtName.getText().trim();
String searchText = txtSearch.getText().trim();
String remarks = txtRemarks.getText().trim();
```

The `.trim()` removes extra spaces before and after the text.

### Combo Boxes

Combo boxes are used for attendance status and subject selection.

```java
cmbAttendance = new JComboBox<>(ATTENDANCE_OPTIONS);
cmbSubjectEntry = new JComboBox<>(SUBJECTS);
```

The selected value is taken using:

```java
String status = cmbAttendance.getSelectedItem().toString();
selectedSubject = cmbSubjectEntry.getSelectedItem().toString();
```

### Password Field

The login dialog uses a password field:

```java
LOGIN_PASSWORD.equals(new String(txtPassword.getPassword()))
```

`getPassword()` returns the typed password.

## How Text Field Values Go To The Database

When the user clicks Insert, the Insert button calls `insertData()`.

```java
btnInsert.addActionListener(e -> insertData());
```

Inside `insertData()`, values are taken from the form:

```java
String studentName = txtName.getText().trim();
String status = cmbAttendance.getSelectedItem().toString();
String remarks = txtRemarks.getText().trim();
```

Then those values are passed to a `PreparedStatement`:

```java
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
```

The question marks `?` are placeholders. The `pst.setString()` and `pst.setDate()` lines replace those placeholders safely.

## Button Functions

Buttons use `addActionListener()` to run a method when clicked.

In `CRUD_GUI.java`:

```java
btnInsert.addActionListener(e -> insertData());
btnUpdate.addActionListener(e -> updateData());
btnDelete.addActionListener(e -> deleteData());
btnClear.addActionListener(e -> clearFields());
btnSearch.addActionListener(e -> loadData());
btnReset.addActionListener(e -> resetFilters());
```

This means:

- Insert button calls `insertData()`
- Update button calls `updateData()`
- Delete button calls `deleteData()`
- Clear button calls `clearFields()`
- Search button calls `loadData()`
- Reset Filter button calls `resetFilters()`

The Teacher View and Attendance Summary buttons open other windows:

```java
btnTeacherView.addActionListener(e -> openTeacherDashboard());
btnAttendanceSummary.addActionListener(e -> openAttendanceSummary());
```

The methods open these windows:

```java
new TeacherDashboard(con).setVisible(true);
new AttendanceSummaryWindow(con).setVisible(true);
```

## Table Input Handling

When the user clicks a row in the main table, the values from the selected row are copied back into the input fields.

```java
int i = table.getSelectedRow();
txtId.setText(model.getValueAt(i, 0).toString());
txtStudentIdentifier.setText(model.getValueAt(i, 1).toString());
txtName.setText(model.getValueAt(i, 2).toString());
txtRemarks.setText(remarksValue == null ? "" : remarksValue.toString());
```

This allows the user to edit or delete an existing record.

## Calendar Popup Handling

The date text field is not typed manually. It opens a calendar popup when clicked.

```java
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
```

The selected date is stored in:

```java
LocalDate selectedDate = LocalDate.now();
```

Then it is displayed using:

```java
txtDate.setText(selectedDate.format(DATE_FORMATTER));
```

## Search And Filter Handling

Search and filter are handled inside `loadData()` in `CRUD_GUI.java`.

The program reads the search and filter values:

```java
String searchText = txtSearch == null ? "" : txtSearch.getText().trim();
String filterDate = txtFilterDate == null ? "" : txtFilterDate.getText().trim();
String filterStatus = (cmbFilterAttendance == null || cmbFilterAttendance.getSelectedIndex() == 0)
        ? "" : cmbFilterAttendance.getSelectedItem().toString();
String filterSubject = (cmbFilterSubject == null || cmbFilterSubject.getSelectedIndex() == 0)
        ? "" : cmbFilterSubject.getSelectedItem().toString();
```

Then it adds SQL conditions only when the user entered a filter:

```java
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
```

The search value uses `LIKE` so it can match partial text:

```java
String keyword = "%" + searchText + "%";
pst.setString(parameterIndex++, keyword);
```

## Database Manipulation

The program uses MySQL and JDBC.

### Database Connection

The database connection is created in `setupDatabase()`:

```java
Connection tempCon = DriverManager.getConnection(URL, USER, PASS);
Statement stmt = tempCon.createStatement();
stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
tempCon.close();

con = DriverManager.getConnection(URL + DB_NAME, USER, PASS);
```

First, the program connects to MySQL. Then it creates the database if it does not exist. After that, it connects to the specific database.

### CREATE DATABASE

```java
stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
```

This creates the database if it does not already exist.

### CREATE TABLE

The program creates the required tables using `CREATE TABLE IF NOT EXISTS`.

Example:

```java
dbStmt.executeUpdate(
    "CREATE TABLE IF NOT EXISTS students (" +
    "student_identifier VARCHAR(50) PRIMARY KEY, " +
    "student_name VARCHAR(100) NOT NULL UNIQUE)"
);
```

### INSERT

Insert is used when saving a new attendance record:

```java
PreparedStatement pst = con.prepareStatement(
    "INSERT INTO attendance_records(student_identifier, student_name, subject_name, attendance_status, attendance_date, attendance_time, time_out, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
);
pst.executeUpdate();
```

### SELECT

Select is used when loading records:

```java
PreparedStatement pst = con.prepareStatement(sql.toString());
ResultSet rs = pst.executeQuery();
```

Then the program reads each row:

```java
while (rs.next()) {
    model.addRow(new Object[]{
        rs.getInt("id"),
        rs.getString("student_identifier"),
        rs.getString("student_name")
    });
}
```

### UPDATE

Update is used when editing an existing attendance record:

```java
PreparedStatement pst = con.prepareStatement(
    "UPDATE attendance_records SET student_identifier=?, student_name=?, subject_name=?, attendance_status=?, attendance_date=?, remarks=? WHERE id=?"
);
int updated = pst.executeUpdate();
```

It is also used for time out:

```java
PreparedStatement timeOutPst = con.prepareStatement(
    "UPDATE attendance_records SET time_out=?, attendance_status=?, remarks=? WHERE id=?"
);
timeOutPst.executeUpdate();
```

### DELETE

Delete is used when removing a selected attendance record:

```java
PreparedStatement pst = con.prepareStatement("DELETE FROM attendance_records WHERE id=?");
pst.setInt(1, Integer.parseInt(txtId.getText().trim()));
int deleted = pst.executeUpdate();
```

### ALTER TABLE

The program can update older database structures using `ALTER TABLE`.

```java
ensureColumnExists("attendance_records", "remarks", "ALTER TABLE attendance_records ADD COLUMN remarks TEXT NULL");
```

This checks if a column exists. If it does not exist, the program adds it.

## Validation

Validation prevents bad input before saving.

### Empty Student Name

```java
if (txtName.getText().trim().isEmpty()) {
    JOptionPane.showMessageDialog(this, "Student name is required.");
    return false;
}
```

### Student Name Format

```java
if (!isValidStudentNameFormat(txtName.getText().trim())) {
    JOptionPane.showMessageDialog(this, "Student name must follow this format: Surname, Firstname");
    return false;
}
```

The actual rule:

```java
boolean isValidStudentNameFormat(String studentName) {
    return studentName.matches("[A-Za-z .'-]+,\\s*[A-Za-z .'-]+");
}
```

This means the required format is:

```text
Surname, Firstname
```

### Date Format

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

The required date format is:

```text
YYYY-MM-DD
```

## AppTheme Styling

`AppTheme.java` centralizes the design of the whole application.

It defines colors:

```java
static final Color MAROON = new Color(120, 18, 32);
static final Color WHITE = Color.WHITE;
static final Color OFF_WHITE = new Color(252, 250, 250);
```

It installs global Swing design settings:

```java
UIManager.put("Button.background", MAROON);
UIManager.put("Button.foreground", WHITE);
UIManager.put("Table.selectionBackground", MAROON);
```

It has reusable style methods:

```java
static void styleButton(AbstractButton button)
static void styleInput(JComponent component)
static void stylePanel(JComponent panel)
static void styleTable(JTable table)
```

The other files call these methods so all windows have the same visual style.

## Teacher Dashboard Database Handling

`TeacherDashboard.java` receives the database connection:

```java
public TeacherDashboard(Connection con) {
    this.con = con;
}
```

It loads attendance dates:

```java
PreparedStatement pst = con.prepareStatement(
    "SELECT DISTINCT attendance_date FROM attendance_records ORDER BY attendance_date DESC"
);
ResultSet rs = pst.executeQuery();
while (rs.next()) {
    // add date to schedule tree
}
```

It loads students for a selected date and filters:

```java
List<Object> params = new ArrayList<>();
PreparedStatement pst = con.prepareStatement(sql.toString());
bindParameters(pst, params);
ResultSet rs = pst.executeQuery();
```

It uses `DefaultListModel<String>` to manage the list of students shown in the GUI:

```java
private final DefaultListModel<String> studentModel = new DefaultListModel<>();
private final JList<String> studentList = new JList<>(studentModel);
```

Records are added using:

```java
studentModel.addElement(rs.getString("student_name"));
```

## Attendance Summary Database Handling

`AttendanceSummaryWindow.java` also receives the database connection:

```java
public AttendanceSummaryWindow(Connection con) {
    this.con = con;
}
```

It calculates weekly or monthly ranges, builds a dynamic list of dates, then queries attendance records.

```java
List<LocalDate> dates = new ArrayList<>();
for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
    dates.add(date);
}
```

It uses a `Map<String, String[]>` to combine multiple attendance records into one row per student:

```java
Map<String, String[]> rows = new LinkedHashMap<>();
```

Then it displays those rows in a table:

```java
for (String[] row : rows.values()) {
    frozenModel.addRow(new Object[]{row[0], row[1]});
}
```

## Summary

The program uses basic Java concepts like data types, arrays, lists, maps, operators, conditions, loops, methods, and event handling. The main window collects input from text fields and combo boxes, then uses button listeners to call methods for inserting, updating, deleting, searching, filtering, and opening other windows.

Database manipulation is done through JDBC using `Connection`, `PreparedStatement`, `ResultSet`, `executeQuery()`, and `executeUpdate()`. The program uses prepared statements so user input is passed safely into SQL commands.
