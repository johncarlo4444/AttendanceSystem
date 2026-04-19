# Student Attendance Monitoring System

A Java-based desktop application for managing student attendance records using a MySQL database.

## Features
- **Centralized Title:** Large, centered "Student Attendance Monitoring System" header.
- **Modern Layout:** Optimized 0.8cm (30px) margins for a clean, professional look.
- **CRUD Operations:**
    - **Insert:** Add new attendance records.
    - **Update:** Modify existing records by selecting them from the table.
    - **Delete:** Remove records from the database.
    - **Clear:** Reset input fields quickly.
- **Search & Filter:** Search students by name or status, and filter the list by specific dates.
- **Real-time Synchronization:** The table automatically refreshes after every database operation.

## Requirements
- **Java Development Kit (JDK):** Version 8 or higher.
- **MySQL Server:** Must be running with the credentials specified in the code.
- **MySQL Connector/J:** Required for database connectivity.

## Logic Flowchart

```mermaid
graph TD
    Start([Start Application]) --> InitUI[Initialize UI Components & Layout]
    InitUI --> DBSetup[Setup Database: Create DB & Table if not exists]
    DBSetup --> LoadData[Load Records from Database into Table]
    LoadData --> UserIdle[Wait for User Interaction]

    %% User Actions
    UserIdle --> ClickInsert[Click 'Insert']
    UserIdle --> ClickUpdate[Click 'Update']
    UserIdle --> ClickDelete[Click 'Delete']
    UserIdle --> ClickClear[Click 'Clear']
    UserIdle --> ClickSearch[Click 'Search/Filter']
    UserIdle --> SelectRow[Click Table Row]

    %% Insert Flow
    ClickInsert --> ValInsert{Validate: Name & Date filled?}
    ValInsert -- No --> ShowErr[Show Error Message] --> UserIdle
    ValInsert -- Yes --> SQLInsert[Execute SQL INSERT Statement]
    SQLInsert --> SuccessMsg[Show Success Message] --> Refresh[Reload Data & Clear Fields] --> UserIdle

    %% Update Flow
    ClickUpdate --> ValUpdate{ID Selected & Data Valid?}
    ValUpdate -- No --> ShowErr
    ValUpdate -- Yes --> SQLUpdate[Execute SQL UPDATE Statement]
    SQLUpdate --> SuccessMsg --> Refresh

    %% Delete Flow
    ClickDelete --> ValDelete{ID Selected?}
    ValDelete -- No --> ShowErr
    ValDelete -- Yes --> SQLDelete[Execute SQL DELETE Statement]
    SQLDelete --> SuccessMsg --> Refresh

    %% Search/Filter Flow
    ClickSearch --> ValFilter{Valid Date Format?}
    ValFilter -- No --> ShowErr
    ValFilter -- Yes --> SQLSearch[Execute SQL SELECT with Filters]
    SQLSearch --> UpdateTable[Update JTable Display] --> UserIdle

    %% Other Actions
    ClickClear --> ResetFields[Clear all Input Fields] --> UserIdle
    SelectRow --> MapFields[Map Row Data to Input Fields] --> UserIdle
```

## Setup Instructions
1. Ensure MySQL is running on `localhost:3306`.
2. Update the `USER` and `PASS` constants in `CRUD_GUI.java` if your MySQL credentials differ.
3. Compile and run `CRUD_GUI.java`. The database `crud_gui_db` and table `attendance_records` will be created automatically.
