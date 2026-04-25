import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;

public final class AppTheme {

    static final Color MAROON = new Color(120, 18, 32);
    static final Color MAROON_DARK = new Color(88, 12, 24);
    static final Color MAROON_SOFT = new Color(245, 232, 235);
    static final Color WHITE = Color.WHITE;
    static final Color OFF_WHITE = new Color(252, 250, 250);
    static final Color BORDER = new Color(188, 150, 158);
    static final Color TEXT = new Color(55, 18, 25);

    private static boolean installed;

    private AppTheme() {
    }

    static void install() {
        if (installed) {
            return;
        }
        installed = true;

        UIManager.put("Panel.background", OFF_WHITE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", MAROON);
        UIManager.put("Button.foreground", WHITE);
        UIManager.put("Button.select", MAROON_DARK);
        UIManager.put("Button.focus", MAROON);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("TextField.background", WHITE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextArea.background", WHITE);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("Table.background", WHITE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", MAROON);
        UIManager.put("Table.selectionForeground", WHITE);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", MAROON);
        UIManager.put("TableHeader.foreground", WHITE);
        UIManager.put("OptionPane.background", OFF_WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT);
    }

    static Border createSectionBorder(String title, int margin) {
        TitledBorder titled = BorderFactory.createTitledBorder(new LineBorder(BORDER), title);
        titled.setTitleColor(MAROON_DARK);
        titled.setTitleFont(new Font("Arial", Font.BOLD, 16));
        return new CompoundBorder(titled, new EmptyBorder(margin, margin, margin, margin));
    }

    static void styleTitle(JLabel label) {
        label.setForeground(MAROON_DARK);
    }

    static void styleButton(AbstractButton button) {
        button.setBackground(MAROON);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MAROON_DARK),
                new EmptyBorder(6, 12, 6, 12)
        ));
        button.setOpaque(true);
    }

    static void styleInput(JComponent component) {
        component.setBackground(WHITE);
        component.setForeground(TEXT);
        component.setBorder(new CompoundBorder(new LineBorder(BORDER), new EmptyBorder(4, 6, 4, 6)));
        if (component instanceof JTextComponent) {
            ((JTextComponent) component).setCaretColor(MAROON_DARK);
        }
    }

    static void stylePanel(JComponent panel) {
        panel.setBackground(OFF_WHITE);
        panel.setForeground(TEXT);
    }

    static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getViewport().setBackground(WHITE);
        scrollPane.setBorder(new LineBorder(BORDER));
    }

    static void styleTable(JTable table) {
        table.setBackground(WHITE);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(MAROON);
        table.setSelectionForeground(WHITE);
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(MAROON);
            header.setForeground(WHITE);
            header.setReorderingAllowed(false);
        }
    }
}
