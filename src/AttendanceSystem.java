import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AttendanceSystem {

    static DefaultTableModel model;
    static JTable table;
    static int days = 10; // change to 31 if needed

    public static void main(String[] args) {

        JFrame frame = new JFrame("Attendance System");
        frame.setSize(1000, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // TOP PANEL (input)
        JPanel topPanel = new JPanel();
        JTextField nameField = new JTextField(15);
        JButton addBtn = new JButton("Add Student");

        topPanel.add(new JLabel("Name:"));
        topPanel.add(nameField);
        topPanel.add(addBtn);

        frame.add(topPanel, BorderLayout.NORTH);

        // TABLE SETUP
        String[] columns = new String[days + 2];
        columns[0] = "Name";

        for (int i = 1; i <= days; i++) {
            columns[i] = "D" + i;
        }

        columns[days + 1] = "Total";

        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        // ADD STUDENT BUTTON
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();

            if (!name.isEmpty()) {
                Object[] row = new Object[days + 2];
                row[0] = name;

                for (int i = 1; i <= days; i++) {
                    row[i] = "✖";
                }

                row[days + 1] = 0;
                model.addRow(row);

                nameField.setText("");
            } else {
                JOptionPane.showMessageDialog(frame, "Enter a name!");
            }
        });

        // CLICK CELL TO TOGGLE
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {

                int row = table.getSelectedRow();
                int col = table.getSelectedColumn();

                // ignore name and total
                if (col == 0 || col == days + 1) return;

                String value = model.getValueAt(row, col).toString();

                if (value.equals("✖")) {
                    model.setValueAt("✔", row, col);
                } else {
                    model.setValueAt("✖", row, col);
                }

                // UPDATE TOTAL
                int total = 0;
                for (int i = 1; i <= days; i++) {
                    if (model.getValueAt(row, i).equals("✔")) {
                        total++;
                    }
                }

                model.setValueAt(total, row, days + 1);
            }
        });

        frame.setVisible(true);
    }
}
