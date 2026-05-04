# complex syntax guide

this guide explains the less basic syntax used in the attendance system.

## preparedstatement and question mark parameters

example

```java
PreparedStatement pst = con.prepareStatement(
        "SELECT * FROM attendance_records WHERE attendance_date = ?"
);
pst.setDate(1, Date.valueOf(selectedDate));
ResultSet rs = pst.executeQuery();
```

meaning

`?` is a placeholder. the real value is added later using `setDate` or `setString`.
this is safer than joining user input directly into sql text because the value is treated as data and not as sql code.

## dynamic sql with stringbuilder

example

```java
StringBuilder sql = new StringBuilder(
        "SELECT * FROM attendance_records WHERE 1=1"
);
if (!searchText.isEmpty()) {
    sql.append(" AND student_name LIKE ?");
}
```

meaning

`StringBuilder` is used when the query changes depending on filters.
`WHERE 1=1` is always true, so later filters can simply add `AND ...` without checking if it is the first condition.

## array copying for all filter option

example

```java
String[] result = new String[values.length + 1];
result[0] = "All";
System.arraycopy(values, 0, result, 1, values.length);
```

meaning

this creates a new array that has one extra item.
`All` is placed first, then the original values are copied after it.
this is used for combo box filters.

## anonymous inner class for read only table model

example

```java
DefaultTableModel model = new DefaultTableModel(columns, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
};
```

meaning

this creates a `DefaultTableModel` and immediately overrides one method.
returning `false` makes table cells read only.
this is useful for reports and record lists.

## lambda expression for button actions

example

```java
btnSearch.addActionListener(e -> loadData());
```

meaning

this is a shorter way to write an event listener.
when the button is clicked, java runs `loadData()`.
`e` represents the action event object.

## try catch for input and database errors

example

```java
try {
    Date.valueOf(value);
    return true;
} catch (Exception e) {
    return false;
}
```

meaning

the code inside `try` may fail.
if it fails, the `catch` block handles the error instead of crashing the program.
this is used for date validation and database operations.

## instanceof type checking

example

```java
if (value instanceof Date) {
    pst.setDate(i + 1, (Date) value);
} else {
    pst.setString(i + 1, value.toString());
}
```

meaning

`instanceof` checks the real type of an object while the program is running.
the code uses it to bind dates differently from text values in prepared statements.

## ternary operator

example

```java
return value == null || value.trim().isEmpty() ? "-" : value;
```

meaning

this is a short if else expression.
if the condition is true it returns `-`.
otherwise it returns the original value.

## localdate and date conversion

example

```java
params.add(Date.valueOf(selectedDate));
LocalDate attendanceDate = rs.getDate("attendance_date").toLocalDate();
```

meaning

`LocalDate` is the modern java date type.
`java.sql.Date` is used when talking to mysql.
the program converts between them when saving and reading dates.

## jtree nodes and user object

example

```java
DefaultMutableTreeNode dateNode =
        new DefaultMutableTreeNode(new DateSelection(date));
Object userObject = node.getUserObject();
```

meaning

a tree node can store an object.
the object controls both the hidden data and the displayed text.
in the dashboard, the hidden value is the date and the display text includes the weekday name.

## custom tostring method

example

```java
@Override
public String toString() {
    return date + " - " + dayName;
}
```

meaning

swing calls `toString()` when it needs to display an object in a component.
overriding it lets the program show friendly labels without changing the stored value.

## linkedhashmap for report rows

example

```java
Map<String, String[]> rows = new LinkedHashMap<>();
```

meaning

`LinkedHashMap` stores key value pairs like a normal map but keeps insertion order.
the attendance summary uses it so each student appears once and in the same order returned by mysql.

## computeifabsent

example

```java
String[] row = rows.computeIfAbsent(key, ignored -> {
    String[] values = new String[2 + dates.size()];
    return values;
});
```

meaning

`computeIfAbsent` checks if a key already has a value.
if it does, the existing value is returned.
if it does not, java runs the lambda and stores the new value.
this is used to create one summary row per student.

## custom table cell renderer

example

```java
class SummaryCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(...) {
        Component component = super.getTableCellRendererComponent(...);
        setHorizontalAlignment(CENTER);
        return component;
    }
}
```

meaning

a renderer controls how table cells look.
it does not change the saved data.
the report uses a renderer to center symbols and color present absent late and excuse values.

## swing layout managers

examples

```java
new BorderLayout(10, 10)
new GridLayout(1, 3, 12, 12)
new GridBagLayout()
```

meaning

layout managers decide how components are placed inside panels.
`BorderLayout` uses north south east west and center.
`GridLayout` creates equal sized cells.
`GridBagLayout` gives detailed control for forms.

## metadata query with information schema

example

```java
SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ? LIMIT 1
```

meaning

this asks mysql if a column already exists.
the program uses this before running `ALTER TABLE`.
that makes startup safe even when the app is opened many times.
