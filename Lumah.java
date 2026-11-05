import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lumah Cafe POS and Management System.
 *
 * Tap-based Point of Sale, recipe-driven inventory with Oatside milk
 * substitution, cash drawer, void/refund control, audit trail, sales reporting,
 * staff commission payouts with date adjustments, and Light/Dark mode support.
 * Single file, standard Java Swing/AWT only. All data is persisted as CSV in
 * ./data.
 *
 * Compile: javac Lumah.java
 * Run: java Lumah
 */
@SuppressWarnings("serial")
public final class Lumah extends JFrame {
    private static final String APP_NAME = "Lumah Cafe POS & Management System";
    static final String ADMIN = "Admin";
    static final String BARISTA = "Barista";

    static final String CAT_COFFEE = "Coffee Based";
    static final String CAT_NONCOFFEE = "Non-Coffee Based";
    static final String CAT_SPECIALS = "Specials";
    static final String CAT_SODA = "Soda";
    static final String CAT_ADDONS = "Add-ons";
    static final String[] DEFAULT_CATS = { CAT_COFFEE, CAT_NONCOFFEE, CAT_SPECIALS, CAT_SODA };
    static final String[] STATUSES = { "Completed", "Pending", "Cancelled", "Voided", "Refunded" };

    // Dynamic Theme Color Engine
    private static boolean isDarkMode = false;

    private static Color BG = new Color(0xF9F6F0);
    private static Color PANEL = new Color(0xEFEBE4);
    private static Color SIDEBAR = new Color(0x2C1E16);
    private static Color SIDEBAR_ACTIVE = new Color(0xC87A53);
    private static Color PRIMARY = new Color(0xC87A53);
    private static Color ACCENT_DARK = new Color(0xA66E4E);
    private static Color SUCCESS = new Color(0x5E8A5A);
    private static Color DANGER = new Color(0xB5483A);
    private static Color WARNING = new Color(0xC9923A);
    private static Color TEXT = new Color(0x2C1E16);
    private static Color MUTED = new Color(0x7A6B62);
    private static Color BORDER = new Color(0xDDD4C8);
    private static Color LIGHT = new Color(0xE4DDD2);
    private static Color FIELD = new Color(0xFFFDF9);

    private static void updateThemeColors(boolean dark) {
        isDarkMode = dark;
        if (isDarkMode) {
            BG = new Color(0x1A1412);
            PANEL = new Color(0x261E1A);
            SIDEBAR = new Color(0x140D0A);
            SIDEBAR_ACTIVE = new Color(0xD98A63);
            PRIMARY = new Color(0xD98A63);
            ACCENT_DARK = new Color(0xE0A382);
            SUCCESS = new Color(0x6A9E66);
            DANGER = new Color(0xC95446);
            WARNING = new Color(0xD9A048);
            TEXT = new Color(0xF2EAE1);
            MUTED = new Color(0xAA9A90);
            BORDER = new Color(0x3D312A);
            LIGHT = new Color(0x362B25);
            FIELD = new Color(0x201916);
        } else {
            BG = new Color(0xF9F6F0);
            PANEL = new Color(0xEFEBE4);
            SIDEBAR = new Color(0x2C1E16);
            SIDEBAR_ACTIVE = new Color(0xC87A53);
            PRIMARY = new Color(0xC87A53);
            ACCENT_DARK = new Color(0xA66E4E);
            SUCCESS = new Color(0x5E8A5A);
            DANGER = new Color(0xB5483A);
            WARNING = new Color(0xC9923A);
            TEXT = new Color(0x2C1E16);
            MUTED = new Color(0x7A6B62);
            BORDER = new Color(0xDDD4C8);
            LIGHT = new Color(0xE4DDD2);
            FIELD = new Color(0xFFFDF9);
        }
    }

    private final Store store = new Store();
    private final CardLayout rootCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private JPanel appView;
    private CardLayout pageCards;
    private JPanel mainPanel;
    private final Map<String, TapButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private String role;
    private PosPanel pos;
    private TapButton themeToggleBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            configureUiDefaults();
            new Lumah().setVisible(true);
        });
    }

    private static Font F(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private static void configureUiDefaults() {
        Font base = F(Font.PLAIN, 14);
        for (String k : new String[] { "Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "Table.font", "TextArea.font", "OptionPane.messageFont", "OptionPane.buttonFont",
                "Spinner.font", "ToggleButton.font" }) {
            UIManager.put(k, base);
        }
        UIManager.put("TableHeader.font", F(Font.BOLD, 13));
        UIManager.put("TableHeader.background", LIGHT);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TabbedPane.font", F(Font.BOLD, 14));
        UIManager.put("TabbedPane.tabInsets", new Insets(12, 28, 12, 28));
        // System look-and-feels use several separate keys for tab states.
        UIManager.put("TabbedPane.background", PANEL);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", PANEL);
        UIManager.put("TabbedPane.selectedForeground", TEXT);
        UIManager.put("TabbedPane.unselectedBackground", LIGHT);
        UIManager.put("TabbedPane.contentAreaColor", BG);
        UIManager.put("TabbedPane.borderHightlightColor", BORDER);
        UIManager.put("TabbedPane.darkShadow", BORDER);
        UIManager.put("TabbedPane.light", BORDER);
        UIManager.put("ComboBox.selectionBackground", LIGHT);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.disabledBackground", PANEL);
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("ComboBox.buttonBackground", LIGHT);
        UIManager.put("ComboBox.buttonDarkShadow", BORDER);
        UIManager.put("ComboBox.buttonHighlight", LIGHT);
        UIManager.put("ComboBox.buttonShadow", BORDER);
        UIManager.put("TabbedPane.selectedForeground", TEXT);
        UIManager.put("TabbedPane.unselectedForeground", TEXT);
        UIManager.put("TabbedPane.focus", BORDER);
        UIManager.put("TabbedPane.contentAreaColor", BG);
        UIManager.put("TabbedPane.borderColor", BORDER);
        UIManager.put("List.selectionBackground", LIGHT);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("List.background", FIELD);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("Panel.background", BG);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("Viewport.background", FIELD);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TextField.background", FIELD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("PasswordField.background", FIELD);
        UIManager.put("PasswordField.foreground", TEXT);
        UIManager.put("TextArea.background", FIELD);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("ComboBox.background", FIELD);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("Table.background", FIELD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("PasswordField.caretForeground", TEXT);
    }

    /**
     * Apply the active palette to native Swing controls whose UI delegates may
     * ignore UIManager changes made after the application starts.
     */
    private void applyThemeToTree(Component component) {
        if (component instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) component;
            // Force theme-aware tab painting instead of inheriting pale native-LAF colors.
            tabs.setUI(new BasicTabbedPaneUI() {
                @Override
                protected void paintTabBackground(Graphics g, int placement, int index,
                        int x, int y, int w, int h, boolean selected) {
                    g.setColor(selected ? PANEL : LIGHT);
                    g.fillRect(x, y, w, h);
                }

                @Override
                protected void paintText(Graphics g, int placement, Font font,
                        FontMetrics metrics, int index, String title, Rectangle textRect,
                        boolean selected) {
                    g.setFont(font);
                    g.setColor(TEXT);
                    super.paintText(g, placement, font, metrics, index, title, textRect, selected);
                }

                @Override
                protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
                    g.setColor(BORDER);
                    int y = calculateTabAreaHeight(placement, runCount, maxTabHeight)
                            + tabPane.getInsets().top;
                    g.drawLine(tabPane.getInsets().left, y,
                            tabPane.getWidth() - tabPane.getInsets().right, y);
                }
            });
            tabs.setBackground(PANEL);
            tabs.setForeground(TEXT);
            tabs.setOpaque(true);
        } else if (component instanceof JComboBox<?>) {
            @SuppressWarnings("unchecked")
            JComboBox<Object> combo = (JComboBox<Object>) component;
            combo.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton arrow = new JButton("\u25BE");
                    arrow.setFont(F(Font.BOLD, 12));
                    arrow.setForeground(TEXT);
                    arrow.setBackground(LIGHT);
                    arrow.setOpaque(true);
                    arrow.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
                    arrow.setFocusPainted(false);
                    return arrow;
                }
            });
            combo.setBackground(FIELD);
            combo.setForeground(TEXT);
            combo.setOpaque(true);
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    JLabel cell = (JLabel) super.getListCellRendererComponent(
                            list, value, index, selected, focus);
                    cell.setOpaque(true);
                    cell.setBackground(selected ? LIGHT : FIELD);
                    cell.setForeground(TEXT);
                    cell.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                    return cell;
                }
            });
        } else if (component instanceof JTextField || component instanceof JPasswordField
                || component instanceof JTextArea) {
            JComponent field = (JComponent) component;
            field.setBackground(FIELD);
            field.setForeground(TEXT);
            if (component instanceof JTextField)
                ((JTextField) component).setCaretColor(TEXT);
            if (component instanceof JPasswordField)
                ((JPasswordField) component).setCaretColor(TEXT);
            if (component instanceof JTextArea)
                ((JTextArea) component).setCaretColor(TEXT);
        } else if (component instanceof JTable) {
            JTable table = (JTable) component;
            table.setBackground(FIELD);
            table.setForeground(TEXT);
            table.setGridColor(BORDER);
            table.setSelectionBackground(isDarkMode ? new Color(0x4A362B) : new Color(0xF0D9CB));
            table.setSelectionForeground(TEXT);
            if (table.getTableHeader() != null) {
                table.getTableHeader().setBackground(LIGHT);
                table.getTableHeader().setForeground(TEXT);
            }
        } else if (component instanceof JViewport) {
            component.setBackground(FIELD);
        } else if (component instanceof JScrollPane) {
            component.setBackground(BG);
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents())
                applyThemeToTree(child);
        }
    }

    public Lumah() {
        super(APP_NAME);
        store.onError = msg -> SwingUtilities
                .invokeLater(() -> JOptionPane.showMessageDialog(this, msg, "Data Error", JOptionPane.ERROR_MESSAGE));
        store.load();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setMinimumSize(new Dimension(1000, 640));
        setSize(Math.max(1100, (int) (screen.width * 0.92)), Math.max(700, (int) (screen.height * 0.88)));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        root.add(buildLoginPanel(), "login");
        add(root, BorderLayout.CENTER);
        rootCards.show(root, "login");
    }

    boolean isAdmin() {
        return ADMIN.equals(role);
    }

    String currentUser() {
        return role == null ? "System" : role;
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static class TapButton extends JButton {
        private Color base;
        private boolean card;

        TapButton(String text, Color base) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setFont(F(Font.BOLD, 14));
            setBorder(new EmptyBorder(10, 16, 10, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBase(base);
        }

        void setBase(Color c) {
            this.base = c;
            double lum = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
            // Choose a contrasting label color from the button fill, not from
            // the old theme's TEXT value (which changes when the theme toggles).
            setForeground(lum > 170 ? (isDarkMode ? BG : SIDEBAR) : new Color(0xFBF8F3));
            repaint();
        }

        void setCard(boolean card) {
            this.card = card;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = base;
            if (!isEnabled())
                c = isDarkMode ? new Color(0x3E342E) : new Color(0xD8D0C4);
            else if (getModel().isPressed())
                c = mix(base, Color.BLACK, 0.12);
            else if (getModel().isRollover())
                c = mix(base, Color.WHITE, 0.25);
            int w = getWidth(), h = getHeight();
            if (card) {
                for (int k = 0; k < 4; k++) {
                    g2.setColor(new Color(0, 0, 0, 18 - k * 4));
                    g2.fillRoundRect(2 - k / 2, 3 - k / 2 + 1, w - 4 + k, h - 5 + k, 18, 18);
                }
                g2.setColor(c);
                g2.fillRoundRect(2, 1, w - 4, h - 6, 16, 16);
                g2.setColor(BORDER);
                g2.drawRoundRect(2, 1, w - 5, h - 7, 16, 16);
            } else {
                g2.setColor(c);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedPanel extends JPanel {
        RoundedPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBackground(PANEL);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            for (int k = 0; k < 5; k++) {
                g2.setColor(new Color(0, 0, 0, 12 - k * 2));
                g2.fillRoundRect(3 - k / 2, 4 - k / 2, w - 6 + k, h - 7 + k, 24, 24);
            }
            g2.setColor(getBackground());
            g2.fillRoundRect(3, 2, w - 6, h - 8, 20, 20);
            g2.setColor(BORDER);
            g2.drawRoundRect(3, 2, w - 7, h - 9, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private JLabel serif(JLabel l) {
        l.setFont(new Font("Georgia", l.getFont().getStyle(), l.getFont().getSize()));
        return l;
    }

    private TapButton btn(String text, Color c) {
        TapButton b = new TapButton(text, c);
        b.setFont(F(Font.BOLD, 13));
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        return b;
    }

    private TapButton secondary(String text) {
        return btn(text, LIGHT);
    }

    private void setChoice(TapButton b, boolean on) {
        b.setBase(on ? PRIMARY : LIGHT);
    }

    private JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(F(style, size));
        l.setForeground(color);
        return l;
    }

    private void onChange(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        });
    }

    private void alert(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private int confirmWarning(String msg, String title) {
        return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    }

    private static final Pattern NUM = Pattern.compile("^\\u20B1?\\s?-?[\\d,]+(\\.\\d+)?$");
    private static final Comparator<Object> SMART = (a, b) -> {
        String x = String.valueOf(a), y = String.valueOf(b);
        if (NUM.matcher(x).matches() && NUM.matcher(y).matches()) {
            return Double.compare(Double.parseDouble(x.replaceAll("[^0-9.\\-]", "")),
                    Double.parseDouble(y.replaceAll("[^0-9.\\-]", "")));
        }
        return x.compareToIgnoreCase(y);
    };

    private void styleTable(JTable t) {
        t.setRowHeight(34);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDER);
        t.setSelectionBackground(isDarkMode ? new Color(0x4A362B) : new Color(0xF0D9CB));
        t.setSelectionForeground(TEXT);

        // Fix for System LaF table headers ignoring background colors
        javax.swing.table.JTableHeader header = t.getTableHeader();
        header.setFont(F(Font.BOLD, 13));
        header.setBackground(LIGHT);
        header.setForeground(TEXT);
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(LIGHT);
                c.setForeground(TEXT);
                setFont(F(Font.BOLD, 13));
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER),
                            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                }
                return c;
            }
        });

        t.setBackground(FIELD);
        t.setForeground(TEXT);
        t.setFillsViewportHeight(true);

        TableRowSorter<javax.swing.table.TableModel> s = new TableRowSorter<>(t.getModel());
        for (int c = 0; c < t.getModel().getColumnCount(); c++)
            s.setComparator(c, SMART);
        t.setRowSorter(s);
    }

    private void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
    }

    private DefaultTableModel tableModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private JPanel wrapTable(String title, DefaultTableModel model) {
        JPanel p = new RoundedPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(14, 14, 18, 14));
        JLabel l = label(title, Font.BOLD, 14, TEXT);
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        JTable t = new JTable(model);
        styleTable(t);
        p.add(l, BorderLayout.NORTH);
        JScrollPane tsp = new JScrollPane(t);
        tsp.setBorder(BorderFactory.createLineBorder(BORDER));
        tsp.getViewport().setBackground(FIELD);
        p.add(tsp, BorderLayout.CENTER);
        return p;
    }

    private JPanel statCard(String name, JLabel val, int size) {
        JPanel p = new RoundedPanel(new BorderLayout(4, 4));
        p.setBorder(new EmptyBorder(16, 14, 22, 14));
        JLabel n = label(name, Font.BOLD, 12, MUTED);
        n.setHorizontalAlignment(SwingConstants.CENTER);
        val.setHorizontalAlignment(SwingConstants.CENTER);
        val.setForeground(TEXT);
        val.setFont(F(Font.BOLD, size));
        p.add(n, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private void addRow(JPanel p, int y, String text, JComponent comp) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = y;
        g.weightx = 0;
        p.add(new JLabel(text), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(comp, g);
    }

    static String money(double v) {
        return String.format(Locale.US, "\u20B1 %,.2f", v);
    }

    static String fmt(LocalDateTime d) {
        return d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US));
    }

    static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9)
            return String.valueOf((long) Math.rint(v));
        return String.format(Locale.US, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    static double parseNum(String s) {
        return Double.parseDouble(s.trim().replace("\u20B1", "").replace(",", "").trim());
    }

    static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private JPanel buildLoginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(SIDEBAR);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PANEL);
        card.setBorder(new EmptyBorder(40, 56, 36, 56));

        JLabel brand = serif(label("LUMAH CAFE", Font.BOLD, 48, TEXT));
        JLabel sub = label("Point of Sale & Management System", Font.PLAIN, 16, MUTED);
        JLabel prompt = label("Select your role to continue", Font.BOLD, 15, TEXT);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel roles = new JPanel(new GridLayout(1, 2, 20, 0));
        roles.setOpaque(false);
        TapButton admin = roleButton("Admin", "Password required<br>Full access", PRIMARY);
        TapButton emp = roleButton("Barista", "POS Register<br>Order History", SUCCESS);
        admin.addActionListener(e -> loginAdmin());
        emp.addActionListener(e -> enterApp(BARISTA));
        roles.add(admin);
        roles.add(emp);
        roles.setAlignmentX(Component.CENTER_ALIGNMENT);

        TapButton exit = secondary("Exit Application");
        exit.setAlignmentX(Component.CENTER_ALIGNMENT);
        exit.addActionListener(e -> dispose());

        card.add(brand);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(30));
        card.add(prompt);
        card.add(Box.createVerticalStrut(16));
        card.add(roles);
        card.add(Box.createVerticalStrut(26));
        card.add(exit);
        outer.add(card);
        return outer;
    }

    private TapButton roleButton(String title, String desc, Color c) {
        TapButton b = new TapButton("<html><div style='text-align:center'><span style='font-size:22px'>" + title
                + "</span><br><br><span style='font-size:12px'>" + desc + "</span></div></html>", c);
        b.setPreferredSize(new Dimension(250, 160));
        return b;
    }

    private void loginAdmin() {
        for (;;) {
            char[] pw = promptPassword("Enter the admin password.", "Admin Login");
            if (pw == null)
                return;
            boolean ok = store.checkPassword(pw);
            Arrays.fill(pw, ' ');
            if (ok) {
                store.log(ADMIN, "LOGIN", "Admin signed in");
                enterApp(ADMIN);
                return;
            }
            store.log("Unknown", "LOGIN_FAILED", "Incorrect admin password entered");
            alert("Incorrect password. Please try again.");
        }
    }

    private char[] promptPassword(String message, String title) {
        JPasswordField f = new JPasswordField(14);
        f.setFont(F(Font.BOLD, 24));
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(new JLabel("<html>" + message.replace("\n", "<br>") + "</html>"), BorderLayout.NORTH);
        p.add(f, BorderLayout.CENTER);
        JOptionPane op = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog d = op.createDialog(this, title);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                f.requestFocusInWindow();
            }
        });
        d.setVisible(true);
        Object v = op.getValue();
        d.dispose();
        return (v instanceof Integer && (Integer) v == JOptionPane.OK_OPTION) ? f.getPassword() : null;
    }

    boolean authorizeAdmin(String action) {
        if (!isAdmin()) {
            alert("This action is restricted to Admin.");
            return false;
        }
        char[] pw = promptPassword("Admin authorization required to " + action + ".\nEnter admin password:",
                "Admin Authorization");
        if (pw == null)
            return false;
        boolean ok = store.checkPassword(pw);
        Arrays.fill(pw, ' ');
        if (!ok) {
            store.log(currentUser(), "AUTH_FAILED", "Failed authorization to " + action);
            alert("Incorrect admin password. Action not authorized.");
        }
        return ok;
    }

    private void changePassword() {
        if (!isAdmin())
            return;
        JPasswordField cur = new JPasswordField(16), n1 = new JPasswordField(16), n2 = new JPasswordField(16);
        JPanel p = new JPanel(new GridBagLayout());
        addRow(p, 0, "Current password:", cur);
        addRow(p, 1, "New password:", n1);
        addRow(p, 2, "Confirm new password:", n2);
        if (JOptionPane.showConfirmDialog(this, p, "Change Admin Password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
            return;
        char[] c = cur.getPassword(), a = n1.getPassword(), b = n2.getPassword();
        try {
            if (!store.checkPassword(c)) {
                alert("Current password is incorrect.");
                store.log(ADMIN, "AUTH_FAILED", "Wrong current password on password change");
                return;
            }
            if (a.length < 4) {
                alert("The new password must be at least 4 characters.");
                return;
            }
            if (!Arrays.equals(a, b)) {
                alert("New password and confirmation do not match.");
                return;
            }
            store.setPassword(a);
            store.log(ADMIN, "PASSWORD_CHANGED", "Admin password changed");
            alert("Admin password updated.");
        } finally {
            Arrays.fill(c, ' ');
            Arrays.fill(a, ' ');
            Arrays.fill(b, ' ');
        }
    }

    private void toggleDarkMode() {
        updateThemeColors(!isDarkMode);
        configureUiDefaults();
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(isDarkMode ? "Light Mode \u2600" : "Dark Mode \uD83C\uDF1A");
        }
        if (role != null) {
            enterApp(role);
        } else {
            root.removeAll();
            JPanel login = buildLoginPanel();
            SwingUtilities.updateComponentTreeUI(login);
            applyThemeToTree(login);
            root.add(login, "login");
            rootCards.show(root, "login");
            root.revalidate();
            root.repaint();
        }
    }

    private void enterApp(String r) {
        this.role = r;
        if (appView != null)
            root.remove(appView);
        navButtons.clear();
        pages.clear();
        appView = buildAppView();
        SwingUtilities.updateComponentTreeUI(appView);
        applyThemeToTree(appView);
        root.add(appView, "app");
        rootCards.show(root, "app");
        root.revalidate();
        showPage(isAdmin() ? "Dashboard" : "New Order");
    }

    private void logout() {
        if (pos != null && pos.hasItems()
                && confirmWarning("The current cart has items that will be discarded. Continue?",
                        "Switch User") != JOptionPane.YES_OPTION) {
            return;
        }
        if (isAdmin())
            store.log(ADMIN, "LOGOUT", "Admin signed out");
        role = null;
        pos = null;
        rootCards.show(root, "login");
    }

    private JPanel buildAppView() {
        JPanel v = new JPanel(new BorderLayout());
        v.add(createTopBar(), BorderLayout.NORTH);
        v.add(createSidebar(), BorderLayout.WEST);
        pageCards = new CardLayout();
        mainPanel = new JPanel(pageCards);
        for (String n : navFor()) {
            Page p = makePage(n);
            pages.put(n, p);
            mainPanel.add(p, n);
        }
        v.add(mainPanel, BorderLayout.CENTER);
        return v;
    }

    private String[] navFor() {
        return isAdmin()
                ? new String[] { "Dashboard", "Inventory", "Staff Salary", "New Order", "Orders", "Sales Report",
                        "Search", "Cash Drawer", "Sync Data", "Audit Log" }
                : new String[] { "New Order", "Orders" };
    }

    private Page makePage(String name) {
        switch (name) {
            case "Dashboard":
                return new DashboardPanel();
            case "Inventory":
                return new InventoryPanel();
            case "Staff Salary":
                return new SalaryPanel();
            case "New Order":
                pos = new PosPanel();
                return pos;
            case "Orders":
                return new OrdersPanel();
            case "Sales Report":
                return new ReportPanel();
            case "Search":
                return new SearchPanel();
            case "Cash Drawer":
                return new DrawerPanel();
            case "Sync Data":
                return new DataTransferPanel();
            default:
                return new AuditPanel();
        }
    }

    private JPanel createTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(10, 18, 10, 18)));
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(serif(label("Lumah Cafe", Font.BOLD, 30, TEXT)));
        titleBox.add(label("Point of Sale & Management System", Font.PLAIN, 12, MUTED));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        right.setOpaque(false);
        right.add(label("Signed in: " + role, Font.BOLD, 13, MUTED));

        themeToggleBtn = secondary(isDarkMode ? "Light Mode \u2600" : "Dark Mode \uD83C\uDF1A");
        themeToggleBtn.addActionListener(e -> toggleDarkMode());
        right.add(themeToggleBtn);

        if (isAdmin()) {
            TapButton pw = secondary("Change Password");
            pw.addActionListener(e -> changePassword());
            right.add(pw);
        }
        TapButton out = btn("Logout / Switch User", DANGER);
        out.addActionListener(e -> logout());
        right.add(out);
        p.add(titleBox, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel createSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDEBAR);
        side.setBorder(new EmptyBorder(18, 12, 18, 12));
        side.setPreferredSize(new Dimension(210, 0));
        JLabel menu = label("NAVIGATION", Font.BOLD, 11, isDarkMode ? new Color(0x99887A) : new Color(0xB9A998));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(menu);
        side.add(Box.createVerticalStrut(12));
        for (String page : navFor()) {
            TapButton b = new TapButton(page, SIDEBAR);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.addActionListener(e -> showPage(page));
            navButtons.put(page, b);
            side.add(b);
            side.add(Box.createVerticalStrut(8));
        }
        return side;
    }

    private void showPage(String name) {
        Page p = pages.get(name);
        if (p == null)
            return;
        p.refresh();
        pageCards.show(mainPanel, name);
        navButtons.forEach((k, v) -> v.setBase(k.equals(name) ? SIDEBAR_ACTIVE : SIDEBAR));
    }

    private void refreshAll() {
        pages.values().forEach(Page::refresh);
    }

    abstract class Page extends JPanel {
        Page() {
            setLayout(new BorderLayout(14, 14));
            setBorder(new EmptyBorder(22, 22, 22, 22));
            setBackground(BG);
        }

        abstract void refresh();

        JLabel heading(String text) {
            JLabel l = serif(label(text, Font.BOLD, 26, TEXT));
            l.setBorder(new EmptyBorder(0, 0, 8, 0));
            return l;
        }
    }

    static class CartLine {
        final MenuItem item;
        final List<MenuItem> addons;
        final String temp;
        int qty;

        CartLine(MenuItem item, List<MenuItem> addons, int qty, String temp) {
            this.item = item;
            this.addons = addons;
            this.qty = qty;
            this.temp = temp == null ? "" : temp;
        }

        double unit() {
            return item.price + addons.stream().mapToDouble(a -> a.price).sum();
        }

        double total() {
            return unit() * qty;
        }

        String key() {
            return item.id + "|" + temp + "|"
                    + addons.stream().map(a -> a.id).sorted().collect(Collectors.joining(","));
        }
    }

    static class PaymentInfo {
        String method;
        double tendered, change;
        String ref = "";
    }

    static String catLabel(String cat) {
        if (CAT_COFFEE.equals(cat))
            return "Coffee";
        if (CAT_NONCOFFEE.equals(cat))
            return "Non-Coffee";
        return cat;
    }

    static String glyph(int codePoint, String fallback) {
        return new Font(Font.DIALOG, Font.PLAIN, 14).canDisplay(codePoint)
                ? new String(Character.toChars(codePoint))
                : fallback;
    }

    class PosPanel extends Page {
        private final JPanel catBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        private final CardLayout gridCards = new CardLayout();
        private final JPanel gridHolder = new JPanel(gridCards);
        private final Map<String, TapButton> catButtons = new LinkedHashMap<>();
        private String selectedCat;
        private final List<CartLine> cart = new ArrayList<>();
        private String orderType = "Takeout";
        private final TapButton takeBtn = btn("Takeout", PRIMARY);
        private final TapButton dineBtn = btn("Dine-In", LIGHT);
        private final JPanel cartRows = new JPanel();
        private final JLabel itemsLbl = label("0 item(s)", Font.PLAIN, 13, MUTED);
        private final JLabel totalLbl = label(money(0), Font.BOLD, 30, TEXT);
        private final TapButton checkoutBtn = new TapButton("CHECKOUT", PRIMARY);

        PosPanel() {
            setLayout(new BorderLayout(18, 0));
            setBorder(new EmptyBorder(16, 18, 14, 14));
            catBar.setOpaque(false);
            gridHolder.setOpaque(false);
            JPanel left = new JPanel(new BorderLayout(0, 14));
            left.setOpaque(false);
            left.add(catBar, BorderLayout.NORTH);
            left.add(gridHolder, BorderLayout.CENTER);
            add(left, BorderLayout.CENTER);
            add(buildCartPanel(), BorderLayout.EAST);
        }

        boolean hasItems() {
            return !cart.isEmpty();
        }

        @Override
        void refresh() {
            catBar.removeAll();
            gridHolder.removeAll();
            catButtons.clear();
            List<String> cats = store.drinkCategories();
            if (selectedCat == null || !cats.contains(selectedCat))
                selectedCat = cats.get(0);
            for (String cat : cats) {
                TapButton b = btn(catLabel(cat), cat.equals(selectedCat) ? PRIMARY : LIGHT);
                b.setFont(F(Font.BOLD, 16));
                b.setBorder(new EmptyBorder(14, 28, 14, 28));
                b.addActionListener(e -> {
                    selectedCat = cat;
                    catButtons.forEach((k, v) -> setChoice(v, k.equals(cat)));
                    gridCards.show(gridHolder, cat);
                });
                catButtons.put(cat, b);
                catBar.add(b);
                gridHolder.add(menuGrid(cat), cat);
            }
            gridCards.show(gridHolder, selectedCat);
            catBar.revalidate();
            catBar.repaint();
            refreshCart();
        }

        private JComponent menuGrid(String cat) {
            List<MenuItem> list = store.items.stream().filter(i -> !i.addon && i.category.equals(cat))
                    .collect(Collectors.toList());
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(4, 2, 10, 2));
            if (list.isEmpty()) {
                JLabel l = label("No items in this category yet.", Font.PLAIN, 15, MUTED);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                wrap.add(l, BorderLayout.CENTER);
                return wrap;
            }
            JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
            grid.setOpaque(false);
            for (MenuItem it : list) {
                String priceHex = String.format("#%06X", (ACCENT_DARK.getRGB() & 0xFFFFFF));
                TapButton t = new TapButton(
                        "<html><div style='text-align:center;width:130px'><span style='font-size:16px'>"
                                + escHtml(it.name) + "</span><br><br><span style='font-size:14px;color:" + priceHex
                                + "'>"
                                + money(it.price).replace(" ", "&nbsp;") + "</span></div></html>",
                        PANEL);
                t.setCard(true);
                t.setPreferredSize(new Dimension(180, 122));
                t.addActionListener(e -> openAddOns(it));
                grid.add(t);
            }
            wrap.add(grid, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(wrap);
            sp.setBorder(null);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.getVerticalScrollBar().setUnitIncrement(24);
            final int[] cols = { 3 };
            sp.getViewport().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int n = Math.max(2, Math.min(5, sp.getViewport().getWidth() / 190));
                    if (n != cols[0]) {
                        cols[0] = n;
                        grid.setLayout(new GridLayout(0, n, 14, 14));
                        grid.revalidate();
                    }
                }
            });
            return sp;
        }

        private JPanel buildCartPanel() {
            RoundedPanel p = new RoundedPanel(new BorderLayout(8, 8));
            p.setBorder(new EmptyBorder(18, 18, 22, 18));
            p.setPreferredSize(new Dimension(390, 0));

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            JLabel h = serif(label("Current Order", Font.BOLD, 22, TEXT));
            h.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel type = new JPanel(new GridLayout(1, 2, 8, 0));
            type.setOpaque(false);
            takeBtn.setPreferredSize(new Dimension(100, 50));
            type.add(takeBtn);
            type.add(dineBtn);
            type.setAlignmentX(Component.LEFT_ALIGNMENT);
            takeBtn.addActionListener(e -> setOrderType("Takeout"));
            dineBtn.addActionListener(e -> setOrderType("Dine-In"));
            top.add(h);
            top.add(Box.createVerticalStrut(10));
            top.add(type);
            p.add(top, BorderLayout.NORTH);

            cartRows.setLayout(new BoxLayout(cartRows, BoxLayout.Y_AXIS));
            cartRows.setOpaque(false);
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(cartRows, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(holder);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.getVerticalScrollBar().setUnitIncrement(20);
            p.add(sp, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(6, 10));
            bottom.setOpaque(false);
            JPanel totals = new JPanel(new BorderLayout());
            totals.setOpaque(false);
            totals.add(label("TOTAL", Font.BOLD, 16, MUTED), BorderLayout.WEST);
            totals.add(totalLbl, BorderLayout.EAST);
            JPanel sub = new JPanel(new BorderLayout());
            sub.setOpaque(false);
            sub.add(itemsLbl, BorderLayout.WEST);
            JPanel tb = new JPanel(new BorderLayout());
            tb.setOpaque(false);
            tb.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                    new EmptyBorder(10, 0, 0, 0)));
            tb.add(totals, BorderLayout.NORTH);
            tb.add(sub, BorderLayout.SOUTH);
            bottom.add(tb, BorderLayout.NORTH);
            JPanel actions = new JPanel(new GridLayout(2, 1, 0, 10));
            actions.setOpaque(false);
            TapButton clear = btn("Clear Cart", LIGHT);
            clear.setPreferredSize(new Dimension(100, 46));
            clear.addActionListener(e -> {
                if (!cart.isEmpty()
                        && confirmWarning("Remove all items from the cart?", "Clear Cart") == JOptionPane.YES_OPTION) {
                    cart.clear();
                    refreshCart();
                }
            });
            checkoutBtn.setFont(F(Font.BOLD, 20));
            checkoutBtn.setPreferredSize(new Dimension(100, 64));
            checkoutBtn.addActionListener(e -> checkout());
            actions.add(clear);
            actions.add(checkoutBtn);
            bottom.add(actions, BorderLayout.SOUTH);
            p.add(bottom, BorderLayout.SOUTH);
            return p;
        }

        private void setOrderType(String t) {
            orderType = t;
            refreshCart();
        }

        void refreshCart() {
            cartRows.removeAll();
            if (cart.isEmpty()) {
                JLabel e = label(
                        "<html><div style='text-align:center'>Cart is empty.<br>Tap a menu item to add it.</div></html>",
                        Font.PLAIN, 14, MUTED);
                e.setAlignmentX(Component.CENTER_ALIGNMENT);
                e.setBorder(new EmptyBorder(40, 10, 10, 10));
                cartRows.add(e);
            }
            int count = 0;
            for (CartLine cl : cart) {
                cartRows.add(cartRow(cl));
                count += cl.qty;
            }
            totalLbl.setText(money(cartTotal()));
            itemsLbl.setText(count + " item(s)");
            checkoutBtn.setEnabled(!cart.isEmpty());
            boolean dine = orderType.equals("Dine-In");
            setChoice(dineBtn, dine);
            setChoice(takeBtn, !dine);
            cartRows.revalidate();
            cartRows.repaint();
        }

        private JPanel cartRow(CartLine cl) {
            JPanel r = new JPanel(new BorderLayout(4, 2));
            r.setOpaque(false);
            r.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(10, 4, 10, 4)));
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel head = new JPanel(new BorderLayout(8, 0));
            head.setOpaque(false);
            String accentHex = String.format("#%06X", (ACCENT_DARK.getRGB() & 0xFFFFFF));
            String nm = "<html>" + escHtml(cl.item.name)
                    + (cl.temp.isEmpty() ? "" : " <span style='color:" + accentHex + "'>[" + cl.temp + "]</span>")
                    + "</html>";
            head.add(label(nm, Font.BOLD, 15, TEXT), BorderLayout.CENTER);
            head.add(label(money(cl.total()), Font.BOLD, 15, TEXT), BorderLayout.EAST);
            r.add(head, BorderLayout.NORTH);
            String sub = (cl.addons.isEmpty() ? ""
                    : "+ " + cl.addons.stream().map(a -> a.name).collect(Collectors.joining(", + ")) + "   ")
                    + "x" + cl.qty;
            r.add(label(sub, Font.PLAIN, 12, MUTED), BorderLayout.CENTER);
            JPanel ctl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            ctl.setOpaque(false);
            TapButton minus = qtyButton("\u2212");
            TapButton plus = qtyButton("+");
            JLabel q = label(String.valueOf(cl.qty), Font.BOLD, 18, TEXT);
            q.setBorder(new EmptyBorder(0, 6, 0, 6));
            minus.addActionListener(e -> {
                if (cl.qty > 1)
                    cl.qty--;
                else
                    cart.remove(cl);
                refreshCart();
            });
            plus.addActionListener(e -> {
                cl.qty++;
                refreshCart();
            });
            TapButton rm = btn("Remove", DANGER);
            rm.setPreferredSize(new Dimension(112, 40));
            rm.setBorder(new EmptyBorder(6, 8, 6, 8));
            rm.addActionListener(e -> {
                cart.remove(cl);
                refreshCart();
            });
            ctl.add(minus);
            ctl.add(q);
            ctl.add(plus);
            ctl.add(Box.createHorizontalStrut(16));
            ctl.add(rm);
            r.add(ctl, BorderLayout.SOUTH);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height + 10));
            return r;
        }

        private TapButton qtyButton(String s) {
            TapButton b = new TapButton(s, LIGHT);
            b.setFont(F(Font.BOLD, 20));
            b.setPreferredSize(new Dimension(48, 40));
            b.setBorder(new EmptyBorder(0, 0, 0, 0));
            return b;
        }

        private double cartTotal() {
            return round2(cart.stream().mapToDouble(CartLine::total).sum());
        }

        void addToCart(MenuItem item, List<MenuItem> addons, int qty, String temp) {
            CartLine nl = new CartLine(item, addons, qty, temp);
            for (CartLine cl : cart) {
                if (cl.key().equals(nl.key())) {
                    cl.qty += qty;
                    refreshCart();
                    return;
                }
            }
            cart.add(nl);
            refreshCart();
        }

        private JLabel sectionLabel(String text) {
            JLabel l = label(text, Font.BOLD, 12, MUTED);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            l.setBorder(new EmptyBorder(8, 2, 8, 0));
            return l;
        }

        private void openAddOns(MenuItem it) {
            List<MenuItem> addons = store.addons();
            boolean needsTemp = !CAT_SODA.equals(it.category);
            JDialog d = new JDialog(Lumah.this, "Customize Order", true);
            d.getContentPane().setBackground(BG);
            d.setLayout(new BorderLayout(14, 14));
            ((JComponent) d.getContentPane()).setBorder(new EmptyBorder(22, 26, 22, 26));

            JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
            head.setOpaque(false);
            head.add(serif(label(it.name, Font.BOLD, 26, TEXT)));
            head.add(label(money(it.price), Font.PLAIN, 16, MUTED));
            d.add(head, BorderLayout.NORTH);

            final String[] temp = { needsTemp ? "Iced" : "" };
            final Set<MenuItem> chosen = new LinkedHashSet<>();
            final int[] qty = { 1 };
            JLabel total = label("", Font.BOLD, 22, TEXT);
            Runnable upd = () -> total
                    .setText("Total: " + money((it.price + chosen.stream().mapToDouble(a -> a.price).sum()) * qty[0]));

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            if (needsTemp) {
                body.add(sectionLabel("TEMPERATURE  (required)"));
                String hotLbl = ("Hot " + glyph(0x2615, "")).trim();
                String icedLbl = ("Iced " + glyph(0x1F9CA, "")).trim();
                TapButton hot = new TapButton(hotLbl, LIGHT);
                TapButton iced = new TapButton(icedLbl, LIGHT);
                for (TapButton b : new TapButton[] { hot, iced }) {
                    b.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
                    b.setPreferredSize(new Dimension(150, 62));
                }
                Runnable sync = () -> {
                    setChoice(hot, "Hot".equals(temp[0]));
                    setChoice(iced, "Iced".equals(temp[0]));
                };
                hot.addActionListener(e -> {
                    temp[0] = "Hot";
                    sync.run();
                });
                iced.addActionListener(e -> {
                    temp[0] = "Iced";
                    sync.run();
                });
                sync.run();
                JPanel seg = new JPanel(new GridLayout(1, 2, 10, 0));
                seg.setOpaque(false);
                seg.add(hot);
                seg.add(iced);
                seg.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(seg);
                body.add(Box.createVerticalStrut(14));
            }

            body.add(sectionLabel("OPTIONAL ADD-ONS"));
            JPanel grid = new JPanel(new GridLayout(0, 3, 10, 10));
            grid.setOpaque(false);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (addons.isEmpty())
                grid.add(label("No add-ons configured.", Font.PLAIN, 14, MUTED));
            String tick = glyph(0x2713, "");
            for (MenuItem a : addons) {
                TapButton b = new TapButton("", LIGHT);
                Runnable paint = () -> {
                    boolean on = chosen.contains(a);
                    b.setText("<html><div style='text-align:center;width:104px'>"
                            + (on && !tick.isEmpty() ? tick + " " : "")
                            + escHtml(a.name) + "<br>+" + money(a.price).replace(" ", "&nbsp;") + "</div></html>");
                    b.setBase(on ? TEXT : LIGHT);
                };
                paint.run();
                b.setPreferredSize(new Dimension(140, 72));
                b.addActionListener(e -> {
                    if (!chosen.remove(a))
                        chosen.add(a);
                    paint.run();
                    upd.run();
                });
                grid.add(b);
            }
            body.add(grid);
            d.add(body, BorderLayout.CENTER);

            JPanel foot = new JPanel(new BorderLayout(10, 12));
            foot.setOpaque(false);
            JPanel step = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            step.setOpaque(false);
            JLabel ql = label("1", Font.BOLD, 24, TEXT);
            ql.setBorder(new EmptyBorder(0, 10, 0, 10));
            TapButton m = qtyButton("\u2212"), pl = qtyButton("+");
            m.addActionListener(e -> {
                if (qty[0] > 1)
                    qty[0]--;
                ql.setText(String.valueOf(qty[0]));
                upd.run();
            });
            pl.addActionListener(e -> {
                if (qty[0] < 99)
                    qty[0]++;
                ql.setText(String.valueOf(qty[0]));
                upd.run();
            });
            step.add(label("Qty", Font.BOLD, 14, MUTED));
            step.add(m);
            step.add(ql);
            step.add(pl);
            foot.add(step, BorderLayout.WEST);
            foot.add(total, BorderLayout.EAST);
            JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
            btns.setOpaque(false);
            TapButton cancel = btn("Cancel", LIGHT);
            TapButton add = btn("Add to Cart", TEXT);
            add.setFont(F(Font.BOLD, 18));
            add.setPreferredSize(new Dimension(100, 58));
            cancel.addActionListener(e -> d.dispose());
            add.addActionListener(e -> {
                addToCart(it, new ArrayList<>(chosen), qty[0], temp[0]);
                d.dispose();
            });
            btns.add(cancel);
            btns.add(add);
            foot.add(btns, BorderLayout.SOUTH);
            d.add(foot, BorderLayout.SOUTH);
            upd.run();
            d.pack();
            d.setMinimumSize(new Dimension(500, 0));
            d.setLocationRelativeTo(Lumah.this);
            d.setVisible(true);
        }

        private void checkout() {
            if (cart.isEmpty())
                return;
            double total = cartTotal();
            PaymentInfo pay = askPayment(total);
            if (pay == null)
                return;

            List<OrderLine> lines = new ArrayList<>();
            for (CartLine cl : cart) {
                List<AddOn> ao = cl.addons.stream().map(a -> new AddOn(a.id, a.name, a.price))
                        .collect(Collectors.toList());
                OrderLine ol = new OrderLine(cl.item.id, cl.item.name, cl.item.price, cl.qty, ao);
                ol.temp = cl.temp;
                lines.add(ol);
            }
            Map<String, Double> need = store.consumption(lines, orderType);
            List<String> short_ = store.shortages(need);
            if (!short_.isEmpty() && JOptionPane.showConfirmDialog(Lumah.this,
                    "Not enough stock recorded for:\n  " + String.join("\n  ", short_)
                            + "\n\nComplete the order anyway?",
                    "Insufficient Stock", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
            Order o = new Order(store.nextOrderId());
            o.date = LocalDateTime.now();
            o.customer = "Walk-in";
            o.lines = lines;
            o.total = total;
            o.payment = pay.tendered;
            o.change = pay.change;
            o.status = "Completed";
            o.notes = "Cashier: " + currentUser();
            o.orderType = orderType;
            o.table = 0;
            o.payMethod = pay.method;
            o.ref = pay.ref;
            store.orders.add(o);
            store.deduct(need);
            store.saveOrders();
            store.saveStock();
            cart.clear();
            orderType = "Takeout";
            refreshAll();
            showReceipt(o);
        }

        private PaymentInfo askPayment(double total) {
            final PaymentInfo[] result = { null };
            final String[] method = { "Cash" };
            final StringBuilder buf = new StringBuilder();
            JDialog d = new JDialog(Lumah.this, "Payment", true);
            d.setLayout(new BorderLayout(12, 12));
            ((JComponent) d.getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

            JLabel due = label("Amount Due: " + money(total), Font.BOLD, 28, TEXT);
            due.setHorizontalAlignment(SwingConstants.CENTER);
            TapButton cashB = btn("Cash", PRIMARY), qrB = btn("QR Payment (GCash / QRPH)", LIGHT);
            cashB.setPreferredSize(new Dimension(100, 52));
            JPanel mrow = new JPanel(new GridLayout(1, 2, 10, 0));
            mrow.add(cashB);
            mrow.add(qrB);
            JPanel north = new JPanel(new BorderLayout(8, 10));
            north.add(due, BorderLayout.NORTH);
            north.add(mrow, BorderLayout.SOUTH);
            d.add(north, BorderLayout.NORTH);

            TapButton ok = new TapButton("Complete Order", SUCCESS);
            ok.setFont(F(Font.BOLD, 18));
            ok.setPreferredSize(new Dimension(100, 56));
            JLabel tenderLbl = label("Tendered: " + money(0), Font.BOLD, 20, TEXT);
            JLabel changeLbl = label("", Font.BOLD, 20, SUCCESS);
            JTextField refField = new JTextField(18);
            refField.setFont(F(Font.BOLD, 22));

            Runnable upd = () -> {
                boolean cash = method[0].equals("Cash");
                double t = 0;
                try {
                    if (buf.length() > 0)
                        t = Double.parseDouble(buf.toString());
                } catch (NumberFormatException ignored) {
                    t = 0;
                }
                tenderLbl.setText("Tendered: " + money(t));
                double ch = t - total;
                changeLbl.setText(ch >= 0 ? "Change: " + money(ch) : "Short: " + money(-ch));
                changeLbl.setForeground(ch >= -0.005 ? SUCCESS : DANGER);
                ok.setEnabled(cash ? t + 0.005 >= total : true);
            };
            onChange(refField, upd);

            JPanel cash = new JPanel(new BorderLayout(10, 10));
            JPanel presets = new JPanel(new GridLayout(1, 5, 8, 0));
            String[] pl = { "Exact", "\u20B1100", "\u20B1200", "\u20B1500", "\u20B11000" };
            double[] pv = { -1, 100, 200, 500, 1000 };
            for (int i = 0; i < pl.length; i++) {
                final double v = pv[i];
                TapButton b = btn(pl[i], SIDEBAR);
                b.setFont(F(Font.BOLD, 16));
                b.setPreferredSize(new Dimension(80, 58));
                b.addActionListener(e -> {
                    buf.setLength(0);
                    buf.append(v < 0 ? String.format(Locale.US, "%.2f", total) : num(v));
                    upd.run();
                });
                presets.add(b);
            }
            cash.add(presets, BorderLayout.NORTH);
            JPanel keys = new JPanel(new GridLayout(4, 3, 8, 8));
            String[] kl = { "7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0", "DEL" };
            for (String k : kl) {
                TapButton b = btn(k, LIGHT);
                b.setFont(F(Font.BOLD, 20));
                b.setPreferredSize(new Dimension(80, 50));
                b.addActionListener(e -> {
                    if (k.equals("DEL")) {
                        if (buf.length() > 0)
                            buf.setLength(buf.length() - 1);
                    } else if (k.equals(".")) {
                        if (buf.indexOf(".") < 0)
                            buf.append(buf.length() == 0 ? "0." : ".");
                    } else if (buf.length() < 9)
                        buf.append(k);
                    upd.run();
                });
                keys.add(b);
            }
            JPanel keyWrap = new JPanel(new BorderLayout(10, 0));
            keyWrap.add(keys, BorderLayout.CENTER);
            JPanel readout = new JPanel(new GridLayout(3, 1, 0, 8));
            readout.setPreferredSize(new Dimension(230, 0));
            TapButton clr = btn("Clear", WARNING);
            clr.addActionListener(e -> {
                buf.setLength(0);
                upd.run();
            });
            readout.add(tenderLbl);
            readout.add(changeLbl);
            readout.add(clr);
            keyWrap.add(readout, BorderLayout.EAST);
            cash.add(keyWrap, BorderLayout.CENTER);

            JPanel qr = new JPanel();
            qr.setLayout(new BoxLayout(qr, BoxLayout.Y_AXIS));
            qr.setBorder(new EmptyBorder(20, 10, 10, 10));
            JLabel q1 = label("Customer scans the store QR and pays " + money(total), Font.PLAIN, 16, TEXT);
            JLabel q2 = label("Enter the GCash / QRPH reference number (Optional):", Font.BOLD, 15, TEXT);
            q1.setAlignmentX(Component.LEFT_ALIGNMENT);
            q2.setAlignmentX(Component.LEFT_ALIGNMENT);
            refField.setAlignmentX(Component.LEFT_ALIGNMENT);
            refField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            qr.add(q1);
            qr.add(Box.createVerticalStrut(18));
            qr.add(q2);
            qr.add(Box.createVerticalStrut(8));
            qr.add(refField);

            CardLayout cl = new CardLayout();
            JPanel body = new JPanel(cl);
            body.add(cash, "Cash");
            body.add(qr, "QR");
            d.add(body, BorderLayout.CENTER);

            cashB.addActionListener(e -> {
                method[0] = "Cash";
                setChoice(cashB, true);
                setChoice(qrB, false);
                cl.show(body, "Cash");
                upd.run();
            });
            qrB.addActionListener(e -> {
                method[0] = "QR";
                setChoice(cashB, false);
                setChoice(qrB, true);
                cl.show(body, "QR");
                upd.run();
                refField.requestFocusInWindow();
            });

            TapButton cancel = btn("Cancel", LIGHT);
            cancel.setPreferredSize(new Dimension(100, 56));
            cancel.addActionListener(e -> d.dispose());
            ok.addActionListener(e -> {
                PaymentInfo p = new PaymentInfo();
                p.method = method[0];
                if (p.method.equals("Cash")) {
                    p.tendered = round2(Double.parseDouble(buf.toString()));
                    p.change = round2(Math.max(0, p.tendered - total));
                } else {
                    p.tendered = total;
                    p.change = 0;
                    p.ref = refField.getText().trim();
                }
                result[0] = p;
                d.dispose();
            });
            JPanel south = new JPanel(new GridLayout(1, 2, 10, 0));
            south.add(cancel);
            south.add(ok);
            d.add(south, BorderLayout.SOUTH);
            upd.run();
            d.pack();
            d.setMinimumSize(new Dimension(680, 0));
            d.setLocationRelativeTo(Lumah.this);
            d.setVisible(true);
            return result[0];
        }
    }

    static String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String receiptText(Order o) {
        final int W = 40;
        StringBuilder sb = new StringBuilder();
        String eq = "=".repeat(W), dash = "-".repeat(W);
        sb.append(center("LUMAH CAFE", W)).append('\n');
        sb.append(center("OFFICIAL RECEIPT", W)).append('\n');
        sb.append(eq).append('\n');
        sb.append(lr("Order ID:", o.id, W)).append('\n');
        sb.append(lr("Date:", fmt(o.date), W)).append('\n');
        sb.append(lr("Order Type:", o.orderType, W)).append('\n');
        sb.append(lr("Payment:", o.payMethod.equals("QR") ? "QR (GCash/QRPH)" : "Cash", W)).append('\n');
        if (!o.ref.isEmpty())
            sb.append(lr("Ref No:", o.ref, W)).append('\n');
        sb.append(dash).append('\n');
        double sub = 0;
        for (OrderLine l : o.lines) {
            double base = l.price * l.quantity;
            sub += base;
            sb.append(lr(l.quantity + " x " + l.displayName(), pesos(base), W)).append('\n');
            for (AddOn a : l.addons) {
                double ap = a.price * l.quantity;
                sub += ap;
                sb.append(lr("    + " + a.name, pesos(ap), W)).append('\n');
            }
        }
        sb.append(dash).append('\n');
        sb.append(lr("TOTAL", "PHP " + pesos(o.total), W)).append('\n');
        if (o.payMethod.equals("QR")) {
            sb.append(lr("PAID (QR)", "PHP " + pesos(o.payment), W)).append('\n');
        } else {
            sb.append(lr("CASH TENDERED", "PHP " + pesos(o.payment), W)).append('\n');
            sb.append(lr("CHANGE", "PHP " + pesos(o.change), W)).append('\n');
        }
        sb.append(eq).append('\n');
        if (!o.status.equals("Completed"))
            sb.append(center("*** " + o.status.toUpperCase() + " ***", W)).append('\n');
        sb.append(center("Thank you for choosing", W)).append('\n');
        sb.append(center("Lumah Cafe!", W)).append('\n');
        sb.append(center("This serves as your official receipt", W)).append('\n');
        return sb.toString();
    }

    private static String pesos(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private static String center(String s, int w) {
        if (s.length() >= w)
            return s;
        return " ".repeat((w - s.length()) / 2) + s;
    }

    private static String lr(String l, String r, int w) {
        int room = w - r.length() - 1;
        if (l.length() > room)
            l = l.substring(0, Math.max(0, room));
        return l + " ".repeat(Math.max(1, w - l.length() - r.length())) + r;
    }

    void showReceipt(Order o) {
        JDialog d = new JDialog(this, "Official Receipt - " + o.id, true);
        d.setLayout(new BorderLayout(8, 8));
        JTextArea area = new JTextArea(receiptText(o));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(FIELD);
        area.setForeground(TEXT);
        area.setBorder(new EmptyBorder(14, 14, 14, 14));
        JScrollPane sp = new JScrollPane(area);
        sp.getViewport().setBackground(FIELD);
        d.add(sp, BorderLayout.CENTER);
        JPanel b = new JPanel(new GridLayout(1, 2, 10, 0));
        b.setBorder(new EmptyBorder(8, 10, 10, 10));
        TapButton print = btn("Print Receipt", PRIMARY);
        TapButton close = btn("Close", LIGHT);
        print.addActionListener(e -> {
            try {
                area.print();
            } catch (PrinterException ex) {
                alert("Unable to print: " + ex.getMessage());
            }
        });
        close.addActionListener(e -> d.dispose());
        b.add(print);
        b.add(close);
        d.add(b, BorderLayout.SOUTH);
        d.setSize(430, 650);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    class DashboardPanel extends Page {
        private final JLabel sales = new JLabel(), orders = new JLabel(), cash = new JLabel(), qr = new JLabel(),
                items = new JLabel(), low = new JLabel();
        private final DefaultTableModel recent = tableModel("Order ID", "Type", "Payment", "Total", "Status");
        private final DefaultTableModel lowModel = tableModel("Ingredient", "In Stock", "Alert Level", "Status");

        DashboardPanel() {
            add(heading("Dashboard"), BorderLayout.NORTH);
            JPanel center = new JPanel(new BorderLayout(12, 12));
            center.setOpaque(false);
            JPanel stats = new JPanel(new GridLayout(2, 3, 10, 10));
            stats.setOpaque(false);
            stats.add(statCard("Total Sales Today", sales, 26));
            stats.add(statCard("Cash Sales Today", cash, 26));
            stats.add(statCard("QR Sales Today", qr, 26));
            stats.add(statCard("Orders Today", orders, 26));
            stats.add(statCard("Menu Items", items, 26));
            stats.add(statCard("Low-Stock Ingredients", low, 26));
            center.add(stats, BorderLayout.NORTH);
            JPanel tables = new JPanel(new GridLayout(1, 2, 12, 0));
            tables.setOpaque(false);
            tables.add(wrapTable("Recent Orders", recent));
            tables.add(wrapTable("Restocking Alerts (at or below alert level)", lowModel));
            center.add(tables, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            LocalDate today = LocalDate.now();
            List<Order> done = store.orders.stream().filter(o -> o.isSale() && o.date.toLocalDate().equals(today))
                    .collect(Collectors.toList());
            sales.setText(money(done.stream().mapToDouble(o -> o.total).sum()));
            cash.setText(money(done.stream().filter(o -> !o.payMethod.equals("QR")).mapToDouble(o -> o.total).sum()));
            qr.setText(money(done.stream().filter(o -> o.payMethod.equals("QR")).mapToDouble(o -> o.total).sum()));
            orders.setText(String.valueOf(done.size()));
            items.setText(String.valueOf(store.items.stream().filter(i -> !i.addon).count()));
            List<Ingredient> lows = store.lowStock();
            low.setText(String.valueOf(lows.size()));
            low.setForeground(lows.isEmpty() ? TEXT : DANGER);
            recent.setRowCount(0);
            store.orders.stream().sorted(Comparator.comparing((Order o) -> o.date).reversed()).limit(10)
                    .forEach(o -> recent
                            .addRow(new Object[] { o.id, o.typeLabel(), o.payLabel(), money(o.total), o.status }));
            lowModel.setRowCount(0);
            for (Ingredient g : lows)
                lowModel.addRow(new Object[] { g.name, num(g.stock) + " " + g.unit, num(g.threshold) + " " + g.unit,
                        g.status() });
        }
    }

    class OrdersPanel extends Page {
        private final JTextField search = new JTextField(18);
        private final JComboBox<String> status = new JComboBox<>(prepend("All", STATUSES));
        private final DefaultTableModel model = tableModel("Order ID", "Date", "Type", "Payment", "Total", "Status",
                "Items");
        private final JTable table = new JTable(model);

        OrdersPanel() {
            styleTable(table);
            widths(table, 80, 150, 100, 70, 100, 90, 520);
            add(heading(isAdmin() ? "Orders / Order History" : "Order History (View Only)"), BorderLayout.NORTH);
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            f.setOpaque(false);
            f.add(new JLabel("Search:"));
            f.add(search);
            f.add(new JLabel("Status:"));
            f.add(status);
            TapButton apply = secondary("Apply");
            apply.addActionListener(e -> refresh());
            f.add(apply);
            TapButton view = btn("View Receipt", PRIMARY);
            view.addActionListener(e -> viewSelected());
            f.add(view);
            if (isAdmin()) {
                TapButton edit = btn("Edit Order", SUCCESS);
                edit.addActionListener(e -> {
                    Order o = selected();
                    if (o != null)
                        editOrder(o);
                });
                TapButton vd = btn("Void Order", WARNING);
                vd.addActionListener(e -> voidSelected());
                TapButton rf = btn("Refund", WARNING);
                rf.addActionListener(e -> refundSelected());
                TapButton del = btn("Delete", DANGER);
                del.addActionListener(e -> deleteSelected());
                f.add(edit);
                f.add(vd);
                f.add(rf);
                f.add(del);
            }
            onChange(search, this::refresh);
            status.addActionListener(e -> refresh());
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        viewSelected();
                }
            });
            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setOpaque(false);
            content.add(f, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(FIELD);
            content.add(sp, BorderLayout.CENTER);
            add(content, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            String q = search.getText().trim().toLowerCase();
            String st = String.valueOf(status.getSelectedItem());
            model.setRowCount(0);
            for (Order o : store.orders.stream().sorted(Comparator.comparing((Order x) -> x.date).reversed())
                    .collect(Collectors.toList())) {
                if (!st.equals("All") && !o.status.equals(st))
                    continue;
                if (!q.isEmpty() && !(o.id.toLowerCase().contains(q) || o.customer.toLowerCase().contains(q)
                        || o.typeLabel().toLowerCase().contains(q) || o.ref.toLowerCase().contains(q)))
                    continue;
                model.addRow(new Object[] { o.id, fmt(o.date), o.typeLabel(), o.payLabel(), money(o.total), o.status,
                        o.itemsSummary() });
            }
        }

        private Order selected() {
            int r = table.getSelectedRow();
            if (r < 0) {
                alert("Select an order first.");
                return null;
            }
            return store.findOrder(String.valueOf(model.getValueAt(table.convertRowIndexToModel(r), 0)));
        }

        private void viewSelected() {
            Order o = selected();
            if (o != null)
                showReceipt(o);
        }

        private String askReason(String title) {
            String r = JOptionPane.showInputDialog(Lumah.this, "Reason (required):", title,
                    JOptionPane.QUESTION_MESSAGE);
            if (r == null)
                return null;
            r = r.trim();
            if (r.isEmpty()) {
                alert("A reason is required.");
                return null;
            }
            return r.replace('\n', ' ');
        }

        private void voidSelected() {
            if (!isAdmin())
                return;
            Order o = selected();
            if (o == null)
                return;
            if (!(o.status.equals("Completed") || o.status.equals("Pending"))) {
                alert("Only Completed or Pending orders can be voided. This order is " + o.status + ".");
                return;
            }
            String reason = askReason("Void " + o.id);
            if (reason == null)
                return;
            if (!authorizeAdmin("void order " + o.id + " (" + money(o.total) + ")"))
                return;
            String old = o.status;
            o.status = "Voided";
            o.notes = (o.notes + " | Voided: " + reason).trim();
            store.restore(store.consumption(o.lines, o.orderType));
            store.saveOrders();
            store.saveStock();
            store.log(ADMIN, "ORDER_VOID", o.id + " " + money(o.total) + " (was " + old + "). Reason: " + reason
                    + ". Ingredients restored to stock.");
            refreshAll();
            alert(o.id + " has been voided and ingredients returned to stock.");
        }

        private void refundSelected() {
            if (!isAdmin())
                return;
            Order o = selected();
            if (o == null)
                return;
            if (!o.status.equals("Completed")) {
                alert("Only Completed orders can be refunded.");
                return;
            }
            String reason = askReason("Refund " + o.id);
            if (reason == null)
                return;
            if (!authorizeAdmin("refund order " + o.id + " (" + money(o.total) + ")"))
                return;
            o.status = "Refunded";
            o.notes = (o.notes + " | Refunded: " + reason).trim();
            store.saveOrders();
            store.log(ADMIN, "ORDER_REFUND", o.id + " " + money(o.total) + " via " + o.payLabel() + ". Reason: "
                    + reason + ". Stock not restored (item was prepared).");
            refreshAll();
            alert(o.id + " marked as refunded. Return " + money(o.total) + " to the customer"
                    + (o.payMethod.equals("QR") ? " via QR/e-wallet." : " from the drawer."));
        }

        private void deleteSelected() {
            if (!isAdmin())
                return;
            Order o = selected();
            if (o == null)
                return;
            if (confirmWarning("Permanently delete order " + o.id + "?\nThis cannot be undone. Consider Void instead.",
                    "Delete Order") != JOptionPane.YES_OPTION)
                return;
            if (!authorizeAdmin("permanently delete order " + o.id))
                return;
            store.orders.remove(o);
            store.saveOrders();
            store.log(ADMIN, "ORDER_DELETE",
                    o.id + " " + money(o.total) + " status " + o.status + " permanently deleted");
            refreshAll();
        }

        private List<OrderLine> collectLines(JTable lt, DefaultTableModel lm, List<OrderLine> rowOrig,
                List<String> rowIds) {
            if (lt.isEditing())
                lt.getCellEditor().stopCellEditing();
            List<OrderLine> out = new ArrayList<>();
            for (int r = 0; r < lm.getRowCount(); r++) {
                String name = String.valueOf(lm.getValueAt(r, 0)).trim();
                if (name.isEmpty())
                    throw new IllegalArgumentException("Row " + (r + 1) + ": item name is empty.");
                double price;
                int qty;
                try {
                    price = parseNum(String.valueOf(lm.getValueAt(r, 2)));
                    qty = Integer.parseInt(String.valueOf(lm.getValueAt(r, 3)).trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Row " + (r + 1) + ": invalid price or quantity.");
                }
                if (price < 0 || qty < 1)
                    throw new IllegalArgumentException("Row " + (r + 1) + ": price must be >= 0 and quantity >= 1.");
                String id = rowIds.get(r);
                if (id == null || id.isEmpty()) {
                    MenuItem m = store.findItemByName(name, false);
                    id = m == null ? "" : m.id;
                }
                OrderLine orig = rowOrig.get(r);
                List<AddOn> addons = new ArrayList<>();
                String txt = String.valueOf(lm.getValueAt(r, 1)).trim();
                for (String a : txt.split(",")) {
                    a = a.trim();
                    if (a.isEmpty() || a.equals("null"))
                        continue;
                    AddOn found = null;
                    if (orig != null)
                        for (AddOn x : orig.addons)
                            if (x.name.equalsIgnoreCase(a))
                                found = x;
                    if (found == null) {
                        MenuItem m = store.findItemByName(a, true);
                        found = m != null ? new AddOn(m.id, m.name, m.price) : new AddOn("", esc(a), 0);
                    }
                    addons.add(found);
                }
                String tt = String.valueOf(lm.getValueAt(r, 4)).trim();
                if (tt.equalsIgnoreCase("null"))
                    tt = "";
                if (tt.equalsIgnoreCase("hot"))
                    tt = "Hot";
                else if (tt.equalsIgnoreCase("iced") || tt.equalsIgnoreCase("ice"))
                    tt = "Iced";
                else if (!tt.isEmpty())
                    throw new IllegalArgumentException("Row " + (r + 1) + ": temperature must be Hot, Iced or blank.");
                OrderLine nlLine = new OrderLine(id, esc(name), price, qty, addons);
                nlLine.temp = tt;
                out.add(nlLine);
            }
            return out;
        }

        private void editOrder(Order o) {
            if (!isAdmin())
                return;
            JDialog d = new JDialog(Lumah.this, "Edit Order " + o.id + " (Admin)", true);
            d.setLayout(new BorderLayout(10, 10));
            ((JComponent) d.getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

            JTextField dateF = new JTextField(o.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 16);
            JComboBox<String> typeCb = new JComboBox<>(new String[] { "Dine-In", "Takeout" });
            typeCb.setSelectedItem(o.orderType);
            JComboBox<String> payCb = new JComboBox<>(new String[] { "Cash", "QR" });
            payCb.setSelectedItem(o.payMethod);
            JTextField refF = new JTextField(o.ref, 16);
            JTextField totalF = new JTextField(num(o.total), 10);
            JTextField tenderF = new JTextField(num(o.payment), 10);
            JTextField changeF = new JTextField(num(o.change), 10);
            JComboBox<String> statusCb = new JComboBox<>(STATUSES);
            if (!Arrays.asList(STATUSES).contains(o.status))
                statusCb.insertItemAt(o.status, 0);
            statusCb.setSelectedItem(o.status);
            JTextField notesF = new JTextField(o.notes, 24);

            JPanel form = new JPanel(new GridBagLayout());
            addRow(form, 0, "Date & time (yyyy-MM-dd HH:mm):", dateF);
            addRow(form, 1, "Order type:", typeCb);
            addRow(form, 2, "Payment method:", payCb);
            addRow(form, 3, "QR reference no.:", refF);
            addRow(form, 4, "Total amount:", totalF);
            addRow(form, 5, "Amount tendered:", tenderF);
            addRow(form, 6, "Change:", changeF);
            addRow(form, 7, "Status:", statusCb);
            addRow(form, 8, "Notes:", notesF);

            DefaultTableModel lm = new DefaultTableModel(
                    new String[] { "Item", "Add-ons (comma separated)", "Unit Price", "Qty", "Temp (Hot/Iced)" }, 0);
            JTable lt = new JTable(lm);
            lt.setRowHeight(30);
            List<OrderLine> rowOrig = new ArrayList<>();
            List<String> rowIds = new ArrayList<>();
            for (OrderLine l : o.lines) {
                lm.addRow(new Object[] { l.name, l.addons.stream().map(a -> a.name).collect(Collectors.joining(", ")),
                        num(l.price), String.valueOf(l.quantity), l.temp });
                rowOrig.add(l);
                rowIds.add(l.itemId);
            }
            JPanel linesPanel = new JPanel(new BorderLayout(6, 6));
            linesPanel.setBorder(BorderFactory.createTitledBorder("Items (double-click a cell to edit)"));
            JScrollPane lsp = new JScrollPane(lt);
            lsp.getViewport().setBackground(FIELD);
            lsp.setPreferredSize(new Dimension(520, 180));
            linesPanel.add(lsp, BorderLayout.CENTER);
            JPanel lb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            JComboBox<MenuItem> menuCb = new JComboBox<>(
                    store.items.stream().filter(i -> !i.addon).toArray(MenuItem[]::new));
            TapButton addLine = btn("Add Line", PRIMARY);
            TapButton rmLine = btn("Remove Line", DANGER);
            TapButton recalc = btn("Recalculate Total", WARNING);
            addLine.addActionListener(e -> {
                MenuItem m = (MenuItem) menuCb.getSelectedItem();
                if (m == null)
                    return;
                lm.addRow(new Object[] { m.name, "", num(m.price), "1", CAT_SODA.equals(m.category) ? "" : "Iced" });
                rowOrig.add(null);
                rowIds.add(m.id);
            });
            rmLine.addActionListener(e -> {
                if (lt.isEditing())
                    lt.getCellEditor().stopCellEditing();
                int r = lt.getSelectedRow();
                if (r >= 0) {
                    lm.removeRow(r);
                    rowOrig.remove(r);
                    rowIds.remove(r);
                }
            });
            recalc.addActionListener(e -> {
                try {
                    List<OrderLine> ls = collectLines(lt, lm, rowOrig, rowIds);
                    totalF.setText(num(round2(ls.stream().mapToDouble(OrderLine::total).sum())));
                } catch (IllegalArgumentException ex) {
                    alert(ex.getMessage());
                }
            });
            lb.add(menuCb);
            lb.add(addLine);
            lb.add(rmLine);
            lb.add(recalc);
            linesPanel.add(lb, BorderLayout.SOUTH);

            JPanel center = new JPanel(new BorderLayout(8, 8));
            center.add(form, BorderLayout.NORTH);
            center.add(linesPanel, BorderLayout.CENTER);
            d.add(center, BorderLayout.CENTER);
            d.add(label("Note: editing does not change ingredient stock. Use Void to return stock.", Font.PLAIN, 12,
                    MUTED), BorderLayout.NORTH);

            TapButton cancel = btn("Cancel", LIGHT);
            TapButton save = btn("Save Changes", SUCCESS);
            cancel.addActionListener(e -> d.dispose());
            save.addActionListener(e -> {
                try {
                    LocalDateTime nd = LocalDateTime.parse(dateF.getText().trim().replace(' ', 'T'));
                    double nt = parseNum(totalF.getText()), np = parseNum(tenderF.getText()),
                            nc = parseNum(changeF.getText());
                    if (nt < 0 || np < 0 || nc < 0)
                        throw new IllegalArgumentException("Amounts cannot be negative.");
                    List<OrderLine> nl = collectLines(lt, lm, rowOrig, rowIds);
                    if (nl.isEmpty())
                        throw new IllegalArgumentException("An order needs at least one item.");
                    String nType = String.valueOf(typeCb.getSelectedItem());
                    StringBuilder ch = new StringBuilder();
                    diff(ch, "date", fmt(o.date), fmt(nd));
                    diff(ch, "type", o.orderType, nType);
                    diff(ch, "payment", o.payMethod, String.valueOf(payCb.getSelectedItem()));
                    diff(ch, "ref", o.ref, refF.getText().trim());
                    diff(ch, "total", num(o.total), num(nt));
                    diff(ch, "tendered", num(o.payment), num(np));
                    diff(ch, "change", num(o.change), num(nc));
                    diff(ch, "status", o.status, String.valueOf(statusCb.getSelectedItem()));
                    diff(ch, "items", o.itemsSummary(), summary(nl));
                    o.date = nd;
                    o.orderType = nType;
                    o.table = 0;
                    o.payMethod = String.valueOf(payCb.getSelectedItem());
                    o.ref = refF.getText().trim();
                    o.total = round2(nt);
                    o.payment = round2(np);
                    o.change = round2(nc);
                    o.status = String.valueOf(statusCb.getSelectedItem());
                    o.notes = notesF.getText().trim();
                    o.lines = nl;
                    store.saveOrders();
                    store.log(ADMIN, "ORDER_EDIT",
                            o.id + (ch.length() == 0 ? " saved (no field changes)" : " changed: " + ch));
                    d.dispose();
                    refreshAll();
                } catch (DateTimeParseException ex) {
                    alert("Invalid date/time. Use yyyy-MM-dd HH:mm, for example 2026-10-31 14:30.");
                } catch (NumberFormatException ex) {
                    alert("Please enter valid numeric amounts.");
                } catch (IllegalArgumentException ex) {
                    alert(ex.getMessage());
                }
            });
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            south.add(cancel);
            south.add(save);
            d.add(south, BorderLayout.SOUTH);
            d.pack();
            d.setLocationRelativeTo(Lumah.this);
            d.setVisible(true);
        }

        private void diff(StringBuilder sb, String field, String a, String b) {
            if (!a.equals(b))
                sb.append(sb.length() > 0 ? "; " : "").append(field).append(" [").append(a).append(" -> ").append(b)
                        .append("]");
        }
    }

    private static String[] prepend(String first, String[] rest) {
        String[] r = new String[rest.length + 1];
        r[0] = first;
        System.arraycopy(rest, 0, r, 1, rest.length);
        return r;
    }

    class RecipeEditor extends JPanel {
        private final JPanel rows = new JPanel();

        RecipeEditor(Map<String, Double> recipe) {
            setLayout(new BorderLayout(4, 4));
            setBorder(BorderFactory.createTitledBorder("Recipe (ingredients consumed per serving)"));
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            if (recipe != null) {
                for (Map.Entry<String, Double> e : recipe.entrySet()) {
                    Ingredient g = store.findIng(e.getKey());
                    if (g != null)
                        addIngRow(g, e.getValue());
                }
            }
            JScrollPane sp = new JScrollPane(rows);
            sp.getViewport().setBackground(FIELD);
            sp.setPreferredSize(new Dimension(440, 170));
            TapButton add = btn("+ Add Ingredient", PRIMARY);
            add.addActionListener(e -> {
                if (store.ingredients.isEmpty()) {
                    alert("Add ingredients in the Ingredient Stock tab first.");
                    return;
                }
                addIngRow(store.ingredients.get(0), 1);
                rows.revalidate();
                rows.repaint();
            });
            add(sp, BorderLayout.CENTER);
            JPanel s = new JPanel(new FlowLayout(FlowLayout.LEFT));
            s.add(add);
            add(s, BorderLayout.SOUTH);
        }

        private void addIngRow(Ingredient sel, double amt) {
            JPanel r = new JPanel(new BorderLayout(6, 0));
            r.setBorder(new EmptyBorder(2, 2, 2, 2));
            JComboBox<Ingredient> cb = new JComboBox<>(store.ingredients.toArray(new Ingredient[0]));
            cb.setSelectedItem(sel);
            JTextField tf = new JTextField(num(amt), 6);
            JButton rm = new JButton("\u00D7");
            rm.addActionListener(e -> {
                rows.remove(r);
                rows.revalidate();
                rows.repaint();
            });
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            right.add(tf);
            right.add(rm);
            r.add(cb, BorderLayout.CENTER);
            r.add(right, BorderLayout.EAST);
            r.putClientProperty("cb", cb);
            r.putClientProperty("tf", tf);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            rows.add(r);
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> value() {
            Map<String, Double> m = new LinkedHashMap<>();
            for (Component c : rows.getComponents()) {
                JPanel r = (JPanel) c;
                Ingredient g = (Ingredient) ((JComboBox<Ingredient>) r.getClientProperty("cb")).getSelectedItem();
                double v = parseNum(((JTextField) r.getClientProperty("tf")).getText());
                if (g == null || v <= 0)
                    throw new NumberFormatException("Recipe amounts must be greater than zero.");
                m.merge(g.id, v, Double::sum);
            }
            return m;
        }
    }

    class InventoryPanel extends Page {
        private final JTextField search = new JTextField(16);
        private final JComboBox<String> category = new JComboBox<>(new String[] { "All" });
        private final DefaultTableModel menuModel = tableModel("Item ID", "Name", "Category", "Price", "Recipe");
        private final JTable menuTable = new JTable(menuModel);
        private final DefaultTableModel stockModel = tableModel("ID", "Ingredient", "Unit", "In Stock", "Alert Level",
                "Status");
        private final JTable stockTable = new JTable(stockModel);
        private boolean updating;

        InventoryPanel() {
            styleTable(menuTable);
            widths(menuTable, 80, 160, 130, 80, 480);
            styleTable(stockTable);
            add(heading("Inventory & Menu Management"), BorderLayout.NORTH);
            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(F(Font.BOLD, 14));
            tabs.addTab("Menu Items & Add-ons", menuTab());
            tabs.addTab("Ingredient Stock", stockTab());
            add(tabs, BorderLayout.CENTER);
            onChange(search, this::refresh);
            category.addActionListener(e -> {
                if (!updating)
                    refresh();
            });
        }

        private JPanel menuTab() {
            JPanel p = new JPanel(new BorderLayout(8, 8));
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            f.add(new JLabel("Search:"));
            f.add(search);
            f.add(new JLabel("Category:"));
            f.add(category);

            TapButton addCatBtn = btn("+ Add Category", PRIMARY);
            TapButton rmCatBtn = btn("Remove Category", DANGER);
            addCatBtn.addActionListener(e -> addCategory());
            rmCatBtn.addActionListener(e -> removeCategory());

            f.add(addCatBtn);
            f.add(rmCatBtn);

            TapButton addI = btn("+ Add Item", PRIMARY), addA = btn("+ Add Add-on", PRIMARY);
            TapButton edit = secondary("Edit / Price / Recipe"), del = btn("Delete", DANGER);
            addI.addActionListener(e -> itemDialog(null, false));
            addA.addActionListener(e -> itemDialog(null, true));
            edit.addActionListener(e -> editSelectedItem());
            del.addActionListener(e -> deleteSelectedItem());
            f.add(addI);
            f.add(addA);
            f.add(edit);
            f.add(del);
            menuTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2)
                        editSelectedItem();
                }
            });
            p.add(f, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(menuTable);
            sp.getViewport().setBackground(FIELD);
            p.add(sp, BorderLayout.CENTER);
            return p;
        }

        private void addCategory() {
            String newCat = JOptionPane.showInputDialog(Lumah.this, "Enter new category name:");
            if (newCat == null || newCat.trim().isEmpty())
                return;
            newCat = newCat.trim();
            if (store.drinkCategories().contains(newCat)) {
                alert("Category already exists.");
                return;
            }
            store.commissions.putIfAbsent(newCat, 10.00);
            store.saveConfig();
            store.log(ADMIN, "CATEGORY_ADD", "Added inventory/commission category: " + newCat);
            refreshAll();
        }

        private void removeCategory() {
            String selCat = String.valueOf(category.getSelectedItem());
            if (selCat.equals("All") || selCat.equals(CAT_ADDONS)) {
                alert("Cannot remove 'All' or the 'Add-ons' category.");
                return;
            }
            long itemCount = store.items.stream().filter(i -> i.category.equals(selCat)).count();
            if (itemCount > 0) {
                alert("Cannot remove category '" + selCat + "' because it contains " + itemCount
                        + " item(s). Reassign or delete them first.");
                return;
            }
            if (confirmWarning("Remove category \"" + selCat + "\"?", "Remove Category") != JOptionPane.YES_OPTION) {
                return;
            }
            store.commissions.remove(selCat);
            store.saveConfig();
            store.log(ADMIN, "CATEGORY_REMOVE", "Removed inventory/commission category: " + selCat);
            refreshAll();
        }

        private JPanel stockTab() {
            JPanel p = new JPanel(new BorderLayout(8, 8));
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            TapButton add = btn("+ Add Ingredient", PRIMARY), edit = secondary("Edit"),
                    re = btn("Restock (+)", SUCCESS), del = btn("Delete", DANGER);
            add.addActionListener(e -> ingDialog(null));
            edit.addActionListener(e -> {
                Ingredient g = selIng();
                if (g != null)
                    ingDialog(g);
            });
            re.addActionListener(e -> restock());
            del.addActionListener(e -> deleteIng());
            f.add(add);
            f.add(re);
            f.add(edit);
            f.add(del);
            p.add(f, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(stockTable);
            sp.getViewport().setBackground(FIELD);
            p.add(sp, BorderLayout.CENTER);
            return p;
        }

        @Override
        void refresh() {
            updating = true;
            Object sel = category.getSelectedItem();
            List<String> cats = new ArrayList<>();
            cats.add("All");
            cats.addAll(store.drinkCategories());
            cats.add(CAT_ADDONS);
            category.setModel(new DefaultComboBoxModel<>(cats.toArray(new String[0])));
            category.setSelectedItem(sel != null && cats.contains(String.valueOf(sel)) ? sel : "All");
            updating = false;
            String q = search.getText().trim().toLowerCase();
            String cat = String.valueOf(category.getSelectedItem());
            menuModel.setRowCount(0);
            for (MenuItem i : store.items) {
                if (!cat.equals("All") && !i.category.equals(cat))
                    continue;
                if (!q.isEmpty() && !(i.name.toLowerCase().contains(q) || i.id.toLowerCase().contains(q)))
                    continue;
                menuModel.addRow(new Object[] { i.id, i.name, i.category, money(i.price), store.recipeText(i.recipe) });
            }
            stockModel.setRowCount(0);
            for (Ingredient g : store.ingredients) {
                stockModel.addRow(new Object[] { g.id, g.name, g.unit, num(g.stock), num(g.threshold), g.status() });
            }
        }

        private MenuItem selItem() {
            int r = menuTable.getSelectedRow();
            if (r < 0) {
                alert("Select an item first.");
                return null;
            }
            return store.findItem(String.valueOf(menuModel.getValueAt(menuTable.convertRowIndexToModel(r), 0)));
        }

        private Ingredient selIng() {
            int r = stockTable.getSelectedRow();
            if (r < 0) {
                alert("Select an ingredient first.");
                return null;
            }
            return store.findIng(String.valueOf(stockModel.getValueAt(stockTable.convertRowIndexToModel(r), 0)));
        }

        private void editSelectedItem() {
            MenuItem m = selItem();
            if (m != null)
                itemDialog(m, m.addon);
        }

        private void deleteSelectedItem() {
            MenuItem m = selItem();
            if (m == null)
                return;
            if (confirmWarning("Delete \"" + m.name + "\" from the menu?", "Delete Item") != JOptionPane.YES_OPTION)
                return;
            store.items.remove(m);
            store.saveMenu();
            store.log(ADMIN, m.addon ? "ADDON_DELETE" : "MENU_DELETE",
                    m.id + " " + m.name + " (" + money(m.price) + ")");
            refreshAll();
        }

        private void itemDialog(MenuItem existing, boolean addon) {
            boolean isNew = existing == null;
            JTextField name = new JTextField(isNew ? "" : existing.name, 22);
            JComboBox<String> cat = new JComboBox<>(store.drinkCategories().toArray(new String[0]));
            cat.setEditable(true);
            if (!isNew && !addon)
                cat.setSelectedItem(existing.category);
            JTextField price = new JTextField(isNew ? "" : num(existing.price), 10);
            RecipeEditor re = new RecipeEditor(isNew ? null : existing.recipe);
            JPanel form = new JPanel(new GridBagLayout());
            addRow(form, 0, addon ? "Add-on name:" : "Item name:", name);
            addRow(form, 1, "Category:", addon ? new JLabel(CAT_ADDONS) : cat);
            addRow(form, 2, "Price (\u20B1):", price);
            JPanel wrap = new JPanel(new BorderLayout(6, 6));
            wrap.add(form, BorderLayout.NORTH);
            wrap.add(re, BorderLayout.CENTER);
            String title = (isNew ? "Add " : "Edit ") + (addon ? "Add-on" : "Menu Item");
            for (;;) {
                if (JOptionPane.showConfirmDialog(Lumah.this, wrap, title, JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
                    return;
                try {
                    String n = esc(name.getText().trim());
                    if (n.isEmpty())
                        throw new IllegalArgumentException("Name is required.");
                    double pr = parseNum(price.getText());
                    if (pr < 0)
                        throw new IllegalArgumentException("Price cannot be negative.");
                    String c = addon ? CAT_ADDONS : esc(String.valueOf(cat.getSelectedItem()).trim());
                    if (c.isEmpty())
                        throw new IllegalArgumentException("Category is required.");
                    MenuItem dup = store.findItemByName(n, addon);
                    if (dup != null && dup != existing)
                        throw new IllegalArgumentException("An item with that name already exists.");
                    Map<String, Double> rec = re.value();
                    MenuItem m = existing;
                    String detail;
                    if (isNew) {
                        m = new MenuItem(store.nextItemId(addon));
                        m.addon = addon;
                        store.items.add(m);
                        detail = "created " + n + " (" + c + ") " + money(pr);
                    } else {
                        detail = existing.name + " " + money(existing.price) + " -> " + n + " " + money(pr);
                    }
                    m.name = n;
                    m.category = c;
                    m.price = round2(pr);
                    m.recipe = rec;
                    store.saveMenu();
                    store.log(ADMIN, (addon ? "ADDON_" : "MENU_") + (isNew ? "ADD" : "EDIT"), m.id + " " + detail);
                    refreshAll();
                    return;
                } catch (NumberFormatException ex) {
                    alert("Please enter valid numbers for the price and recipe amounts.");
                } catch (IllegalArgumentException ex) {
                    alert(ex.getMessage());
                }
            }
        }

        private void ingDialog(Ingredient existing) {
            boolean isNew = existing == null;
            JTextField name = new JTextField(isNew ? "" : existing.name, 18);
            JComboBox<String> unit = new JComboBox<>(new String[] { "ml", "g", "shot", "pump", "pcs", "scoop" });
            unit.setEditable(true);
            unit.setSelectedItem(isNew ? "ml" : existing.unit);
            JTextField stock = new JTextField(isNew ? "0" : num(existing.stock), 10);
            JTextField thr = new JTextField(isNew ? "0" : num(existing.threshold), 10);
            JPanel form = new JPanel(new GridBagLayout());
            addRow(form, 0, "Ingredient name:", name);
            addRow(form, 1, "Unit:", unit);
            addRow(form, 2, "Current stock:", stock);
            addRow(form, 3, "Low-stock alert level:", thr);
            for (;;) {
                if (JOptionPane.showConfirmDialog(Lumah.this, form, isNew ? "Add Ingredient" : "Edit Ingredient",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
                    return;
                try {
                    String n = esc(name.getText().trim());
                    String u = esc(String.valueOf(unit.getSelectedItem()).trim());
                    double s = parseNum(stock.getText()), t = parseNum(thr.getText());
                    if (n.isEmpty() || u.isEmpty())
                        throw new IllegalArgumentException("Name and unit are required.");
                    if (s < 0 || t < 0)
                        throw new IllegalArgumentException("Stock and alert level cannot be negative.");
                    Ingredient g = existing;
                    String detail;
                    if (isNew) {
                        g = new Ingredient(store.nextIngId());
                        store.ingredients.add(g);
                        detail = "created " + n + " stock " + num(s) + " " + u;
                    } else {
                        detail = existing.name + " stock " + num(existing.stock) + " -> " + num(s) + ", alert "
                                + num(existing.threshold) + " -> " + num(t);
                    }
                    g.name = n;
                    g.unit = u;
                    g.stock = round2(s);
                    g.threshold = round2(t);
                    store.saveStock();
                    store.log(ADMIN, isNew ? "INGREDIENT_ADD" : "INGREDIENT_EDIT", g.id + " " + detail);
                    refreshAll();
                    return;
                } catch (NumberFormatException ex) {
                    alert("Please enter valid numbers.");
                } catch (IllegalArgumentException ex) {
                    alert(ex.getMessage());
                }
            }
        }

        private void restock() {
            Ingredient g = selIng();
            if (g == null)
                return;
            String s = JOptionPane.showInputDialog(Lumah.this, "Quantity received for " + g.name + " (" + g.unit + "):",
                    "Restock", JOptionPane.QUESTION_MESSAGE);
            if (s == null)
                return;
            try {
                double v = parseNum(s);
                if (v <= 0)
                    throw new NumberFormatException();
                double before = g.stock;
                g.stock = round2(g.stock + v);
                store.saveStock();
                store.log(ADMIN, "RESTOCK", g.name + " " + num(before) + " -> " + num(g.stock) + " " + g.unit);
                refreshAll();
            } catch (NumberFormatException ex) {
                alert("Enter a quantity greater than zero.");
            }
        }

        private void deleteIng() {
            Ingredient g = selIng();
            if (g == null)
                return;
            long used = store.items.stream().filter(i -> i.recipe.containsKey(g.id)).count();
            String msg = "Delete ingredient \"" + g.name + "\"?"
                    + (used > 0 ? "\nIt will also be removed from " + used + " recipe(s)." : "");
            if (confirmWarning(msg, "Delete Ingredient") != JOptionPane.YES_OPTION)
                return;
            store.ingredients.remove(g);
            store.items.forEach(i -> i.recipe.remove(g.id));
            store.saveStock();
            store.saveMenu();
            store.log(ADMIN, "INGREDIENT_DELETE", g.id + " " + g.name);
            refreshAll();
        }
    }

    class SalaryPanel extends Page {
        private final DefaultTableModel rateModel = tableModel("Category", "Commission Rate (per cup/item)");
        private final JTable rateTable = new JTable(rateModel);
        private final DefaultTableModel summaryModel = tableModel("Staff Member / Cashier", "Total Cups/Items Sold",
                "Total Commission (Payout)");
        private final JTable summaryTable = new JTable(summaryModel);

        private final DefaultTableModel historyModel = tableModel("Payout ID", "Staff Member", "Start Date", "End Date",
                "Extra Dates", "Total Cups", "Total Payout", "Recorded At");
        private final JTable historyTable = new JTable(historyModel);

        private final JTextField startDate = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
        private final JTextField extraDatesField = new JTextField("", 18);
        private final JLabel periodEndDateLbl = new JLabel();

        SalaryPanel() {
            styleTable(rateTable);
            styleTable(summaryTable);
            styleTable(historyTable);
            add(heading("Staff Salary & 7-Day Payout (Mon-Thu Schedule + Adjustments)"), BorderLayout.NORTH);

            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(F(Font.BOLD, 14));
            tabs.addTab("7-Working-Day Payout Calculator", reportTab());
            tabs.addTab("Payout History & Archives", historyTab());
            tabs.addTab("Configure Category Rates", ratesTab());
            add(tabs, BorderLayout.CENTER);
            refresh();
        }

        private JPanel reportTab() {
            JPanel p = new JPanel(new BorderLayout(8, 8));
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            controls.add(new JLabel("Cycle Start (YYYY-MM-DD):"));
            controls.add(startDate);
            controls.add(new JLabel("Extra/Adjusted Dates:"));
            extraDatesField.setEditable(false);
            controls.add(extraDatesField);

            TapButton addDateBtn = btn("+ Add Working Date", PRIMARY);
            addDateBtn.addActionListener(e -> openAddDateDialog());
            controls.add(addDateBtn);

            TapButton rmDateBtn = btn("- Remove Date", WARNING);
            rmDateBtn.addActionListener(e -> openRemoveDateDialog());
            controls.add(rmDateBtn);

            periodEndDateLbl.setFont(F(Font.BOLD, 13));
            periodEndDateLbl.setForeground(PRIMARY);
            controls.add(periodEndDateLbl);

            JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            TapButton calc = btn("Calculate Payout", PRIMARY);
            calc.addActionListener(e -> refreshReports());
            actionRow.add(calc);

            TapButton record = btn("Record / Save Payout to History", SUCCESS);
            record.addActionListener(e -> recordPayouts());
            actionRow.add(record);

            JPanel topWrap = new JPanel(new BorderLayout());
            topWrap.setOpaque(false);
            topWrap.add(controls, BorderLayout.NORTH);
            topWrap.add(actionRow, BorderLayout.SOUTH);

            JPanel center = new JPanel(new BorderLayout(8, 8));
            center.setOpaque(false);
            center.add(topWrap, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(summaryTable);
            sp.getViewport().setBackground(FIELD);
            center.add(sp, BorderLayout.CENTER);
            p.add(center, BorderLayout.CENTER);
            return p;
        }

        private void openAddDateDialog() {
            JDialog d = new JDialog(Lumah.this, "Add Working Date to Payout", true);
            d.setLayout(new BorderLayout(10, 10));
            ((JComponent) d.getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

            List<String> availableDates = store.orders.stream()
                    .map(o -> o.date.toLocalDate().toString())
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            if (availableDates.isEmpty()) {
                LocalDate now = LocalDate.now();
                for (int i = 0; i < 14; i++) {
                    availableDates.add(now.minusDays(i).toString());
                }
            }

            JComboBox<String> dateCombo = new JComboBox<>(availableDates.toArray(new String[0]));
            dateCombo.setEditable(true);

            JPanel panel = new JPanel(new GridBagLayout());
            addRow(panel, 0, "Select or type working date (YYYY-MM-DD):", dateCombo);

            TapButton ok = btn("Add Date", SUCCESS);
            TapButton cancel = btn("Cancel", LIGHT);

            ok.addActionListener(e -> {
                String selected = String.valueOf(dateCombo.getSelectedItem()).trim();
                if (!selected.isEmpty()) {
                    try {
                        LocalDate.parse(selected);
                        String current = extraDatesField.getText().trim();
                        if (current.isEmpty()) {
                            extraDatesField.setText(selected);
                        } else {
                            Set<String> set = new TreeSet<>(Arrays.asList(current.split("\\s*,\\s*")));
                            set.add(selected);
                            extraDatesField.setText(String.join(", ", set));
                        }
                        refreshReports();
                        d.dispose();
                    } catch (Exception ex) {
                        alert("Invalid date format. Use YYYY-MM-DD.");
                    }
                }
            });
            cancel.addActionListener(e -> d.dispose());

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btns.add(cancel);
            btns.add(ok);

            d.add(panel, BorderLayout.CENTER);
            d.add(btns, BorderLayout.SOUTH);
            d.pack();
            d.setLocationRelativeTo(Lumah.this);
            d.setVisible(true);
        }

        private void openRemoveDateDialog() {
            String current = extraDatesField.getText().trim();
            if (current.isEmpty()) {
                alert("No extra/adjusted working dates currently added.");
                return;
            }
            List<String> extraList = Arrays.stream(current.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (extraList.isEmpty()) {
                alert("No extra dates to remove.");
                return;
            }

            JDialog d = new JDialog(Lumah.this, "Remove Extra Working Date", true);
            d.setLayout(new BorderLayout(10, 10));
            ((JComponent) d.getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

            JComboBox<String> dateCombo = new JComboBox<>(extraList.toArray(new String[0]));
            JPanel panel = new JPanel(new GridBagLayout());
            addRow(panel, 0, "Select extra date to remove:", dateCombo);

            TapButton ok = btn("Remove", DANGER);
            TapButton cancel = btn("Cancel", LIGHT);

            ok.addActionListener(e -> {
                String sel = (String) dateCombo.getSelectedItem();
                if (sel != null) {
                    extraList.remove(sel);
                    extraDatesField.setText(String.join(", ", extraList));
                    refreshReports();
                    d.dispose();
                }
            });
            cancel.addActionListener(e -> d.dispose());

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btns.add(cancel);
            btns.add(ok);

            d.add(panel, BorderLayout.CENTER);
            d.add(btns, BorderLayout.SOUTH);
            d.pack();
            d.setLocationRelativeTo(Lumah.this);
            d.setVisible(true);
        }

        private JPanel historyTab() {
            JPanel p = new JPanel(new BorderLayout(8, 8));
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

            TapButton viewDetail = btn("View Daily Breakdown", PRIMARY);
            viewDetail.addActionListener(e -> viewPayoutDailyBreakdown());
            f.add(viewDetail);

            TapButton delHist = btn("Delete Payout Record", DANGER);
            delHist.addActionListener(e -> deletePayoutRecord());
            f.add(delHist);

            p.add(f, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(historyTable);
            sp.getViewport().setBackground(FIELD);
            p.add(sp, BorderLayout.CENTER);
            return p;
        }

        private void viewPayoutDailyBreakdown() {
            int r = historyTable.getSelectedRow();
            if (r < 0) {
                alert("Select a payout history record first.");
                return;
            }
            String id = String.valueOf(historyModel.getValueAt(historyTable.convertRowIndexToModel(r), 0));
            PayoutRecord ph = store.findPayoutRecord(id);
            if (ph == null)
                return;

            List<LocalDate> cycleDates = new ArrayList<>();
            try {
                LocalDate cur = LocalDate.parse(ph.startDate);
                LocalDate targetEnd = LocalDate.parse(ph.endDate);
                while (!cur.isAfter(targetEnd)) {
                    DayOfWeek dow = cur.getDayOfWeek();
                    if (dow != DayOfWeek.FRIDAY && dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                        cycleDates.add(cur);
                    }
                    cur = cur.plusDays(1);
                }
            } catch (Exception ignored) {
            }

            if (ph.extraDates != null && !ph.extraDates.trim().isEmpty()) {
                for (String extra : ph.extraDates.split(",")) {
                    String exTrim = extra.trim();
                    if (!exTrim.isEmpty()) {
                        try {
                            LocalDate exDate = LocalDate.parse(exTrim);
                            if (!cycleDates.contains(exDate))
                                cycleDates.add(exDate);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            Collections.sort(cycleDates);

            DefaultTableModel dailyModel = tableModel("Date", "Day of Week", "Cups Sold", "Commission Earned");
            JTable dailyTable = new JTable(dailyModel);
            styleTable(dailyTable);

            int totalCupsAll = 0;
            double totalCommAll = 0;

            for (LocalDate d : cycleDates) {
                int dayCups = 0;
                double dayComm = 0;

                for (Order o : store.orders) {
                    if (!o.isSale())
                        continue;
                    if (!o.date.toLocalDate().equals(d))
                        continue;

                    String staff = "General Staff";
                    if (o.notes.contains("Cashier: ")) {
                        int idx = o.notes.indexOf("Cashier: ");
                        staff = o.notes.substring(idx + 9).split(" \\|")[0].trim();
                    }
                    if (!staff.equalsIgnoreCase(ph.staffName))
                        continue;

                    for (OrderLine l : o.lines) {
                        MenuItem m = store.findItem(l.itemId);
                        String cat = m != null ? m.category : "Uncategorized";
                        double rate = store.commissions.getOrDefault(cat, 0.0);
                        dayCups += l.quantity;
                        dayComm += (rate * l.quantity);
                    }
                }
                totalCupsAll += dayCups;
                totalCommAll += dayComm;
                dailyModel.addRow(new Object[] { d.toString(), d.getDayOfWeek().name(), dayCups, money(dayComm) });
            }

            JDialog d = new JDialog(Lumah.this, "Daily Breakdown: " + ph.staffName + " (" + ph.id + ")", true);
            d.setLayout(new BorderLayout(10, 10));
            ((JComponent) d.getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));

            JPanel topInfo = new JPanel(new GridLayout(2, 1));
            topInfo.add(label("Staff Member: " + ph.staffName, Font.BOLD, 16, TEXT));
            topInfo.add(label("Cycle: " + ph.startDate + " to " + ph.endDate
                    + (ph.extraDates.isEmpty() ? "" : " + Extras: " + ph.extraDates), Font.PLAIN, 13, MUTED));

            JPanel bottomSummary = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomSummary.add(label("Total Cups: " + totalCupsAll + "   |   Total Payout: " + money(totalCommAll),
                    Font.BOLD, 15, SUCCESS));

            JPanel centerWrap = new JPanel(new BorderLayout(6, 6));
            JScrollPane dsp = new JScrollPane(dailyTable);
            dsp.getViewport().setBackground(FIELD);
            centerWrap.add(dsp, BorderLayout.CENTER);
            centerWrap.add(bottomSummary, BorderLayout.SOUTH);

            TapButton closeBtn = btn("Close", LIGHT);
            closeBtn.addActionListener(e -> d.dispose());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(closeBtn);

            d.add(topInfo, BorderLayout.NORTH);
            d.add(centerWrap, BorderLayout.CENTER);
            d.add(south, BorderLayout.SOUTH);
            d.setSize(560, 420);
            d.setLocationRelativeTo(Lumah.this);
            d.setVisible(true);
        }

        private JPanel ratesTab() {
            JPanel p = new JPanel(new BorderLayout(8, 8));
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            TapButton edit = btn("Edit Rate", PRIMARY);
            TapButton addCat = btn("Add New Category Rate", SUCCESS);
            edit.addActionListener(e -> editRate());
            addCat.addActionListener(e -> addCategoryRate());
            f.add(edit);
            f.add(addCat);

            p.add(f, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(rateTable);
            sp.getViewport().setBackground(FIELD);
            p.add(sp, BorderLayout.CENTER);
            return p;
        }

        @Override
        void refresh() {
            rateModel.setRowCount(0);
            store.commissions.forEach((cat, rate) -> rateModel.addRow(new Object[] { cat, money(rate) }));
            refreshReports();
            refreshHistory();
        }

        private LocalDate calculateEndDate(LocalDate start) {
            LocalDate current = start;
            int counted = 0;
            while (counted < 7) {
                DayOfWeek dow = current.getDayOfWeek();
                if (dow != DayOfWeek.FRIDAY && dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    counted++;
                    if (counted == 7) {
                        return current;
                    }
                }
                current = current.plusDays(1);
            }
            return current;
        }

        private Set<LocalDate> parseExtraDates() {
            Set<LocalDate> dates = new HashSet<>();
            String text = extraDatesField.getText().trim();
            if (text.isEmpty())
                return dates;
            for (String part : text.split(",")) {
                part = part.trim();
                if (!part.isEmpty()) {
                    try {
                        dates.add(LocalDate.parse(part));
                    } catch (Exception ignored) {
                    }
                }
            }
            return dates;
        }

        private void refreshReports() {
            summaryModel.setRowCount(0);
            LocalDate start;
            try {
                start = LocalDate.parse(startDate.getText().trim());
            } catch (Exception e) {
                alert("Invalid start date format. Use YYYY-MM-DD.");
                return;
            }

            LocalDate end = calculateEndDate(start);
            Set<LocalDate> extraDates = parseExtraDates();
            periodEndDateLbl.setText("  -> Payout End: " + end.toString()
                    + (extraDates.isEmpty() ? "" : " (+ " + extraDates.size() + " adjusted day(s))"));

            Map<String, Integer> counts = new HashMap<>();
            Map<String, Double> earnings = new HashMap<>();

            for (Order o : store.orders) {
                if (!o.isSale())
                    continue;
                LocalDate od = o.date.toLocalDate();

                boolean inRange = (od.compareTo(start) >= 0 && od.compareTo(end) <= 0) || extraDates.contains(od);
                if (!inRange)
                    continue;

                String staff = "General Staff";
                if (o.notes.contains("Cashier: ")) {
                    int idx = o.notes.indexOf("Cashier: ");
                    staff = o.notes.substring(idx + 9).split(" \\|")[0].trim();
                }

                for (OrderLine l : o.lines) {
                    MenuItem m = store.findItem(l.itemId);
                    String cat = m != null ? m.category : "Uncategorized";
                    double rate = store.commissions.getOrDefault(cat, 0.0);

                    counts.put(staff, counts.getOrDefault(staff, 0) + l.quantity);
                    earnings.put(staff, earnings.getOrDefault(staff, 0.0) + (rate * l.quantity));
                }
            }

            if (earnings.isEmpty()) {
                summaryModel.addRow(new Object[] { "No transactions recorded for this payout period", 0, money(0) });
            } else {
                earnings.forEach((staff, totalEarned) -> {
                    int totalCups = counts.getOrDefault(staff, 0);
                    summaryModel.addRow(new Object[] { staff, totalCups, money(totalEarned) });
                });
            }
        }

        private void refreshHistory() {
            historyModel.setRowCount(0);
            for (PayoutRecord ph : store.payoutHistory) {
                List<String> cycleDates = new ArrayList<>();
                try {
                    LocalDate cur = LocalDate.parse(ph.startDate);
                    LocalDate targetEnd = LocalDate.parse(ph.endDate);
                    while (!cur.isAfter(targetEnd)) {
                        DayOfWeek dow = cur.getDayOfWeek();
                        if (dow != DayOfWeek.FRIDAY && dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                            cycleDates.add(cur.toString());
                        }
                        cur = cur.plusDays(1);
                    }
                } catch (Exception ignored) {
                }

                if (ph.extraDates != null && !ph.extraDates.trim().isEmpty()) {
                    for (String extra : ph.extraDates.split(",")) {
                        String exTrim = extra.trim();
                        if (!exTrim.isEmpty() && !cycleDates.contains(exTrim)) {
                            cycleDates.add(exTrim);
                        }
                    }
                }
                Collections.sort(cycleDates);
                String listedDates = String.join(", ", cycleDates);

                historyModel.addRow(new Object[] { ph.id, ph.staffName, ph.startDate, ph.endDate,
                        (ph.extraDates.isEmpty() ? "None (" + listedDates + ")"
                                : ph.extraDates + " [All: " + listedDates + "]"),
                        ph.totalCups, money(ph.totalPayout), fmt(ph.recordedAt) });
            }
        }

        private void recordPayouts() {
            LocalDate start;
            try {
                start = LocalDate.parse(startDate.getText().trim());
            } catch (Exception e) {
                alert("Invalid start date format. Use YYYY-MM-DD.");
                return;
            }
            LocalDate end = calculateEndDate(start);
            Set<LocalDate> extraDates = parseExtraDates();
            String extraStr = extraDatesField.getText().trim();

            Map<String, Integer> counts = new HashMap<>();
            Map<String, Double> earnings = new HashMap<>();

            for (Order o : store.orders) {
                if (!o.isSale())
                    continue;
                LocalDate od = o.date.toLocalDate();
                boolean inRange = (od.compareTo(start) >= 0 && od.compareTo(end) <= 0) || extraDates.contains(od);
                if (!inRange)
                    continue;

                String staff = "General Staff";
                if (o.notes.contains("Cashier: ")) {
                    int idx = o.notes.indexOf("Cashier: ");
                    staff = o.notes.substring(idx + 9).split(" \\|")[0].trim();
                }

                for (OrderLine l : o.lines) {
                    MenuItem m = store.findItem(l.itemId);
                    String cat = m != null ? m.category : "Uncategorized";
                    double rate = store.commissions.getOrDefault(cat, 0.0);

                    counts.put(staff, counts.getOrDefault(staff, 0) + l.quantity);
                    earnings.put(staff, earnings.getOrDefault(staff, 0.0) + (rate * l.quantity));
                }
            }

            if (earnings.isEmpty()) {
                alert("No earnings calculated for this cycle to record.");
                return;
            }

            if (confirmWarning(
                    "Save payout records for cycle " + start + " to " + end
                            + (extraStr.isEmpty() ? "" : " (+ extra: " + extraStr + ")") + "?",
                    "Record Payouts") != JOptionPane.YES_OPTION) {
                return;
            }

            earnings.forEach((staff, totalEarned) -> {
                int totalCups = counts.getOrDefault(staff, 0);
                PayoutRecord ph = new PayoutRecord(store.nextPayoutId());
                ph.staffName = staff;
                ph.startDate = start.toString();
                ph.endDate = end.toString();
                ph.extraDates = extraStr;
                ph.totalCups = totalCups;
                ph.totalPayout = round2(totalEarned);
                ph.recordedAt = LocalDateTime.now();
                store.payoutHistory.add(ph);
            });

            store.savePayoutHistory();
            store.log(ADMIN, "PAYOUT_RECORD",
                    "Recorded payouts for cycle " + start + " to " + end + " with extras: " + extraStr);
            refreshHistory();
            alert("Payout records successfully saved to history!");
        }

        private void deletePayoutRecord() {
            int r = historyTable.getSelectedRow();
            if (r < 0) {
                alert("Select a payout history record first.");
                return;
            }
            String id = String.valueOf(historyModel.getValueAt(historyTable.convertRowIndexToModel(r), 0));
            PayoutRecord ph = store.findPayoutRecord(id);
            if (ph == null)
                return;

            if (confirmWarning(
                    "Delete payout record " + ph.id + " for " + ph.staffName + " (" + money(ph.totalPayout) + ")?",
                    "Delete Payout") != JOptionPane.YES_OPTION) {
                return;
            }
            if (!authorizeAdmin("delete payout record " + ph.id))
                return;

            store.payoutHistory.remove(ph);
            store.savePayoutHistory();
            store.log(ADMIN, "PAYOUT_DELETE", "Deleted payout record " + ph.id + " (" + ph.staffName + ")");
            refreshHistory();
        }

        private void editRate() {
            int r = rateTable.getSelectedRow();
            if (r < 0) {
                alert("Select a category rate first.");
                return;
            }
            String cat = String.valueOf(rateModel.getValueAt(rateTable.convertRowIndexToModel(r), 0));
            String val = JOptionPane.showInputDialog(Lumah.this, "Enter commission rate per cup for " + cat + " (PHP):",
                    store.commissions.getOrDefault(cat, 0.0));
            if (val == null)
                return;
            try {
                double rate = parseNum(val);
                if (rate < 0)
                    throw new NumberFormatException();
                store.commissions.put(cat, round2(rate));
                store.saveConfig();
                store.log(ADMIN, "COMMISSION_EDIT", cat + " commission rate updated to " + money(rate));
                refresh();
            } catch (NumberFormatException e) {
                alert("Please enter a valid non-negative number.");
            }
        }

        private void addCategoryRate() {
            String cat = JOptionPane.showInputDialog(Lumah.this,
                    "Enter new category name (e.g. Pastries, Merchandise):");
            if (cat == null || cat.trim().isEmpty())
                return;
            cat = cat.trim();
            String val = JOptionPane.showInputDialog(Lumah.this,
                    "Enter commission rate per item for " + cat + " (PHP):", "10.00");
            if (val == null)
                return;
            try {
                double rate = parseNum(val);
                if (rate < 0)
                    throw new NumberFormatException();
                store.commissions.put(cat, round2(rate));
                store.saveConfig();
                store.log(ADMIN, "COMMISSION_ADD", "Added category " + cat + " with rate " + money(rate));
                refresh();
            } catch (NumberFormatException e) {
                alert("Please enter a valid non-negative number.");
            }
        }
    }

    class ReportPanel extends Page {
        private final JComboBox<String> type = new JComboBox<>(new String[] { "Daily", "Monthly", "All" });
        private final JTextField date = new JTextField(LocalDate.now().toString(), 10);
        private final JLabel total = new JLabel(), cash = new JLabel(), qr = new JLabel(), count = new JLabel(),
                avg = new JLabel(), itemsSold = new JLabel(), voided = new JLabel(), lowCount = new JLabel();
        private final DefaultTableModel best = tableModel("Product", "Qty Sold", "Revenue");
        private final DefaultTableModel addonModel = tableModel("Add-on", "Qty Sold", "Revenue");
        private final DefaultTableModel lowModel = tableModel("Ingredient", "In Stock", "Alert Level", "Status");

        ReportPanel() {
            add(heading("Sales Report"), BorderLayout.NORTH);
            JPanel c = new JPanel(new BorderLayout(10, 10));
            c.setOpaque(false);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            controls.setOpaque(false);
            controls.add(new JLabel("Report:"));
            controls.add(type);
            controls.add(new JLabel("Date (YYYY-MM-DD):"));
            controls.add(date);
            TapButton gen = btn("Generate Report", PRIMARY);
            gen.addActionListener(e -> refresh());
            controls.add(gen);
            JPanel stats = new JPanel(new GridLayout(2, 4, 10, 10));
            stats.setOpaque(false);
            stats.add(statCard("Total Revenue", total, 20));
            stats.add(statCard("Cash Sales", cash, 20));
            stats.add(statCard("QR Sales", qr, 20));
            stats.add(statCard("Completed Orders", count, 20));
            stats.add(statCard("Average Order Value", avg, 20));
            stats.add(statCard("Items Sold", itemsSold, 20));
            stats.add(statCard("Voided / Refunded", voided, 20));
            stats.add(statCard("Low-Stock Ingredients", lowCount, 20));
            JPanel top = new JPanel(new BorderLayout(8, 8));
            top.setOpaque(false);
            top.add(controls, BorderLayout.NORTH);
            top.add(stats, BorderLayout.CENTER);
            c.add(top, BorderLayout.NORTH);
            JPanel tables = new JPanel(new GridLayout(1, 3, 10, 0));
            tables.setOpaque(false);
            tables.add(wrapTable("Best-Selling Products", best));
            tables.add(wrapTable("Add-on Breakdown", addonModel));
            tables.add(wrapTable("Restocking Notifications", lowModel));
            c.add(tables, BorderLayout.CENTER);
            add(c, BorderLayout.CENTER);
        }

        private List<Order> filtered() {
            String t = String.valueOf(type.getSelectedItem());
            if (t.equals("All"))
                return new ArrayList<>(store.orders);
            try {
                LocalDate d = LocalDate.parse(date.getText().trim());
                if (t.equals("Daily"))
                    return store.orders.stream().filter(o -> o.date.toLocalDate().equals(d))
                            .collect(Collectors.toList());
                return store.orders.stream()
                        .filter(o -> o.date.getYear() == d.getYear() && o.date.getMonth() == d.getMonth())
                        .collect(Collectors.toList());
            } catch (DateTimeParseException e) {
                alert("Invalid date. Showing all orders. Use YYYY-MM-DD.");
                return new ArrayList<>(store.orders);
            }
        }

        @Override
        void refresh() {
            List<Order> all = filtered();
            List<Order> done = all.stream().filter(Order::isSale).collect(Collectors.toList());
            double sum = done.stream().mapToDouble(o -> o.total).sum();
            total.setText(money(sum));
            cash.setText(money(done.stream().filter(o -> !o.payMethod.equals("QR")).mapToDouble(o -> o.total).sum()));
            qr.setText(money(done.stream().filter(o -> o.payMethod.equals("QR")).mapToDouble(o -> o.total).sum()));
            count.setText(String.valueOf(done.size()));
            avg.setText(money(done.isEmpty() ? 0 : sum / done.size()));
            itemsSold.setText(
                    String.valueOf(done.stream().flatMap(o -> o.lines.stream()).mapToInt(l -> l.quantity).sum()));
            voided.setText(String.valueOf(
                    all.stream().filter(o -> o.status.equals("Voided") || o.status.equals("Refunded")).count()));
            Map<String, double[]> prod = new HashMap<>(), add = new HashMap<>();
            for (Order o : done) {
                for (OrderLine l : o.lines) {
                    double[] p = prod.computeIfAbsent(l.name, k -> new double[2]);
                    p[0] += l.quantity;
                    p[1] += l.price * l.quantity;
                    for (AddOn a : l.addons) {
                        double[] x = add.computeIfAbsent(a.name, k -> new double[2]);
                        x[0] += l.quantity;
                        x[1] += a.price * l.quantity;
                    }
                }
            }
            fill(best, prod);
            fill(addonModel, add);
            List<Ingredient> lows = store.lowStock();
            lowCount.setText(String.valueOf(lows.size()));
            lowCount.setForeground(lows.isEmpty() ? TEXT : DANGER);
            lowModel.setRowCount(0);
            for (Ingredient g : lows)
                lowModel.addRow(new Object[] { g.name, num(g.stock) + " " + g.unit, num(g.threshold) + " " + g.unit,
                        g.status() });
        }

        private void fill(DefaultTableModel m, Map<String, double[]> data) {
            m.setRowCount(0);
            data.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                    .forEach(
                            e -> m.addRow(new Object[] { e.getKey(), (long) e.getValue()[0], money(e.getValue()[1]) }));
        }
    }

    class SearchPanel extends Page {
        private final JComboBox<String> by = new JComboBox<>(
                new String[] { "Order ID", "Customer", "Status", "Product", "Payment Method", "Order Type" });
        private final JTextField keyword = new JTextField(16), from = new JTextField(9), to = new JTextField(9);
        private final DefaultTableModel model = tableModel("Order ID", "Date", "Type", "Payment", "Total", "Status",
                "Items");

        SearchPanel() {
            add(heading("Search / Transaction History"), BorderLayout.NORTH);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            controls.add(new JLabel("Search by:"));
            controls.add(by);
            controls.add(new JLabel("Keyword:"));
            controls.add(keyword);
            controls.add(new JLabel("From:"));
            controls.add(from);
            controls.add(new JLabel("To (YYYY-MM-DD):"));
            controls.add(to);
            TapButton s = btn("Search", PRIMARY);
            s.addActionListener(e -> refresh());
            controls.add(s);
            JTable t = new JTable(model);
            styleTable(t);
            widths(t, 80, 150, 100, 70, 100, 90, 520);
            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setOpaque(false);
            content.add(controls, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(t);
            sp.getViewport().setBackground(FIELD);
            content.add(sp, BorderLayout.CENTER);
            add(content, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            String field = String.valueOf(by.getSelectedItem());
            String q = keyword.getText().trim().toLowerCase();
            model.setRowCount(0);
            LocalDate f = null, t = null;
            try {
                if (!from.getText().trim().isEmpty())
                    f = LocalDate.parse(from.getText().trim());
                if (!to.getText().trim().isEmpty())
                    t = LocalDate.parse(to.getText().trim());
            } catch (DateTimeParseException ignored) {
            }
            for (Order o : store.orders.stream().sorted(Comparator.comparing((Order x) -> x.date).reversed())
                    .collect(Collectors.toList())) {
                LocalDate od = o.date.toLocalDate();
                if (f != null && od.isBefore(f))
                    continue;
                if (t != null && od.isAfter(t))
                    continue;
                boolean match = q.isEmpty()
                        || (field.equals("Order ID") && o.id.toLowerCase().contains(q))
                        || (field.equals("Customer") && o.customer.toLowerCase().contains(q))
                        || (field.equals("Status") && o.status.toLowerCase().contains(q))
                        || (field.equals("Payment Method")
                                && (o.payLabel().toLowerCase().contains(q) || o.ref.toLowerCase().contains(q)))
                        || (field.equals("Order Type") && o.typeLabel().toLowerCase().contains(q))
                        || (field.equals("Product") && o.lines.stream().anyMatch(l -> l.name.toLowerCase().contains(q)
                                || l.addons.stream().anyMatch(a -> a.name.toLowerCase().contains(q))));
                if (match)
                    model.addRow(new Object[] { o.id, fmt(o.date), o.typeLabel(), o.payLabel(), money(o.total),
                            o.status, o.itemsSummary() });
            }
        }
    }

    class DrawerPanel extends Page {
        private final JTextField dateF = new JTextField(LocalDate.now().toString(), 10);
        private final DefaultTableModel model = tableModel("Time", "Type", "Amount", "Note", "By");
        private final JTable table = new JTable(model);
        private final JLabel fl = new JLabel(), cs = new JLabel(), ci = new JLabel(), po = new JLabel(),
                dr = new JLabel(),
                ex = new JLabel(), cnt = new JLabel(), var = new JLabel();

        DrawerPanel() {
            styleTable(table);
            add(heading("Cash Drawer & Expenses"), BorderLayout.NORTH);
            JPanel c = new JPanel(new BorderLayout(10, 10));
            c.setOpaque(false);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            controls.setOpaque(false);
            controls.add(new JLabel("Date (YYYY-MM-DD):"));
            controls.add(dateF);
            TapButton go = secondary("Load");
            go.addActionListener(e -> refresh());
            TapButton fb = btn("Set Starting Float", PRIMARY), in = btn("Cash In", PRIMARY);
            TapButton drop = btn("Cash Drop", WARNING), pay = btn("Payout / Expense", WARNING);
            TapButton close = btn("Close Drawer (Count)", SUCCESS), rm = btn("Delete Entry", DANGER);
            fb.addActionListener(e -> entry("FLOAT", "Starting float", true));
            in.addActionListener(e -> entry("CASH_IN", "Cash in", false));
            drop.addActionListener(e -> entry("DROP", "Cash drop (moved to safe)", false));
            pay.addActionListener(e -> entry("PAYOUT", "Payout / expense", false));
            close.addActionListener(e -> closeDrawer());
            rm.addActionListener(e -> deleteEntry());
            for (JComponent x : new JComponent[] { go, fb, in, drop, pay, close, rm })
                controls.add(x);

            JPanel stats = new JPanel(new GridLayout(2, 4, 10, 10));
            stats.setOpaque(false);
            stats.add(statCard("Starting Float", fl, 20));
            stats.add(statCard("Cash Sales", cs, 20));
            stats.add(statCard("Cash In", ci, 20));
            stats.add(statCard("Payouts / Expenses", po, 20));
            stats.add(statCard("Cash Drops", dr, 20));
            stats.add(statCard("Expected in Drawer", ex, 20));
            stats.add(statCard("Last Counted", cnt, 20));
            stats.add(statCard("Variance (Counted - Expected)", var, 20));
            JPanel top = new JPanel(new BorderLayout(8, 8));
            top.setOpaque(false);
            top.add(controls, BorderLayout.NORTH);
            top.add(stats, BorderLayout.CENTER);
            c.add(top, BorderLayout.NORTH);
            JPanel tp = new JPanel(new BorderLayout());
            tp.setBackground(PANEL);
            tp.setBorder(BorderFactory.createLineBorder(BORDER));
            JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(FIELD);
            tp.add(sp, BorderLayout.CENTER);
            c.add(tp, BorderLayout.CENTER);
            add(c, BorderLayout.CENTER);
        }

        private LocalDate day() {
            try {
                return LocalDate.parse(dateF.getText().trim());
            } catch (DateTimeParseException e) {
                dateF.setText(LocalDate.now().toString());
                return LocalDate.now();
            }
        }

        private double sum(LocalDate d, String t) {
            return store.drawer.stream().filter(x -> x.when.toLocalDate().equals(d) && x.type.equals(t))
                    .mapToDouble(x -> x.amount).sum();
        }

        private double expected(LocalDate d) {
            double sales = store.orders.stream()
                    .filter(o -> o.isSale() && !o.payMethod.equals("QR") && o.date.toLocalDate().equals(d))
                    .mapToDouble(o -> o.total).sum();
            return round2(sum(d, "FLOAT") + sales + sum(d, "CASH_IN") - sum(d, "PAYOUT") - sum(d, "DROP"));
        }

        @Override
        void refresh() {
            LocalDate d = day();
            double sales = store.orders.stream()
                    .filter(o -> o.isSale() && !o.payMethod.equals("QR") && o.date.toLocalDate().equals(d))
                    .mapToDouble(o -> o.total).sum();
            fl.setText(money(sum(d, "FLOAT")));
            cs.setText(money(sales));
            ci.setText(money(sum(d, "CASH_IN")));
            po.setText(money(sum(d, "PAYOUT")));
            dr.setText(money(sum(d, "DROP")));
            double e = expected(d);
            ex.setText(money(e));
            DrawerEntry last = store.drawer.stream()
                    .filter(x -> x.when.toLocalDate().equals(d) && x.type.equals("COUNT"))
                    .max(Comparator.comparing(x -> x.when)).orElse(null);
            if (last == null) {
                cnt.setText("Not counted");
                var.setText("-");
                var.setForeground(TEXT);
            } else {
                cnt.setText(money(last.amount));
                double v = round2(last.amount - e);
                var.setText((v > 0 ? "+" : "") + money(v));
                var.setForeground(Math.abs(v) < 0.005 ? SUCCESS : DANGER);
            }
            model.setRowCount(0);
            store.drawer.stream().filter(x -> x.when.toLocalDate().equals(d))
                    .sorted(Comparator.comparing((DrawerEntry x) -> x.when).reversed())
                    .forEach(x -> model
                            .addRow(new Object[] { x.when.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US)),
                                    x.typeLabel(), money(x.amount), x.note, x.user }));
        }

        private String[] askAmount(String title, boolean noteRequired) {
            JTextField amt = new JTextField(12), note = new JTextField(22);
            amt.setFont(F(Font.BOLD, 20));
            JPanel p = new JPanel(new GridBagLayout());
            addRow(p, 0, "Amount (\u20B1):", amt);
            addRow(p, 1, noteRequired ? "Note / reason:" : "Note (optional):", note);
            for (;;) {
                if (JOptionPane.showConfirmDialog(Lumah.this, p, title, JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
                    return null;
                try {
                    double v = parseNum(amt.getText());
                    if (v < 0)
                        throw new NumberFormatException();
                    if (noteRequired && note.getText().trim().isEmpty()) {
                        alert("A note / reason is required.");
                        continue;
                    }
                    return new String[] { String.valueOf(round2(v)), note.getText().trim() };
                } catch (NumberFormatException ex) {
                    alert("Enter a valid amount.");
                }
            }
        }

        private void entry(String type, String title, boolean replace) {
            LocalDate d = day();
            String[] r = askAmount(title, type.equals("PAYOUT"));
            if (r == null)
                return;
            if (replace)
                store.drawer.removeIf(x -> x.type.equals("FLOAT") && x.when.toLocalDate().equals(d));
            DrawerEntry e = new DrawerEntry(store.nextDrawerId());
            LocalDateTime now = LocalDateTime.now();
            e.when = d.equals(LocalDate.now()) ? now : d.atTime(now.toLocalTime());
            e.type = type;
            e.amount = Double.parseDouble(r[0]);
            e.note = r[1];
            e.user = currentUser();
            store.drawer.add(e);
            store.saveDrawer();
            store.log(ADMIN, "DRAWER_" + type, money(e.amount) + " on " + d + (e.note.isEmpty() ? "" : " - " + e.note));
            refresh();
        }

        private void closeDrawer() {
            LocalDate d = day();
            double exp = expected(d);
            String[] r = askAmount("Close Drawer - count all cash now.\nExpected: " + money(exp), false);
            if (r == null)
                return;
            double counted = Double.parseDouble(r[0]);
            double v = round2(counted - exp);
            DrawerEntry e = new DrawerEntry(store.nextDrawerId());
            e.when = d.equals(LocalDate.now()) ? LocalDateTime.now() : d.atTime(23, 59);
            e.type = "COUNT";
            e.amount = counted;
            e.note = "Expected " + money(exp) + ", variance " + (v > 0 ? "+" : "") + money(v)
                    + (r[1].isEmpty() ? "" : " - " + r[1]);
            e.user = currentUser();
            store.drawer.add(e);
            store.saveDrawer();
            store.log(ADMIN, "DRAWER_CLOSE",
                    d + " counted " + money(counted) + ", expected " + money(exp) + ", variance " + money(v));
            refresh();
            alert("End-of-day reconciliation for " + d + "\n\nExpected: " + money(exp) + "\nCounted:  " + money(counted)
                    + "\nVariance: " + (v > 0 ? "+" : "") + money(v)
                    + (Math.abs(v) < 0.005 ? "\n\nDrawer balanced." : v > 0 ? "\n\nOVER" : "\n\nSHORT"));
        }

        private void deleteEntry() {
            int r = table.getSelectedRow();
            if (r < 0) {
                alert("Select an entry first.");
                return;
            }
            LocalDate d = day();
            List<DrawerEntry> list = store.drawer.stream().filter(x -> x.when.toLocalDate().equals(d))
                    .sorted(Comparator.comparing((DrawerEntry x) -> x.when).reversed()).collect(Collectors.toList());
            int idx = table.convertRowIndexToModel(r);
            if (idx >= list.size())
                return;
            DrawerEntry e = list.get(idx);
            if (confirmWarning("Delete this " + e.typeLabel() + " entry of " + money(e.amount) + "?",
                    "Delete Entry") != JOptionPane.YES_OPTION)
                return;
            if (!authorizeAdmin("delete a drawer entry"))
                return;
            store.drawer.remove(e);
            store.saveDrawer();
            store.log(ADMIN, "DRAWER_DELETE", e.typeLabel() + " " + money(e.amount) + " " + e.note);
            refresh();
        }
    }

    class DataTransferPanel extends Page {
        DataTransferPanel() {
            add(heading("System Data Transfer & Cross-System Sync"), BorderLayout.NORTH);

            RoundedPanel card = new RoundedPanel(new BorderLayout(16, 16));
            card.setBorder(new EmptyBorder(24, 24, 24, 24));

            JTextArea info = new JTextArea(
                    "This tool packages all saved application records (Inventory, Staff Salaries, Orders, Sales Reports, Cash Drawer, and Settings) into an encrypted clipboard payload.\n\n"
                            +
                            "1. Click 'Copy All Saved Data to Clipboard' on your source system.\n" +
                            "2. Move to the destination system running Lumah Cafe and click 'Paste & Import Data from Clipboard'.\n"
                            +
                            "3. Only Lumah Cafe can read and apply this encrypted data format.");
            info.setFont(F(Font.PLAIN, 15));
            info.setEditable(false);
            info.setOpaque(false);
            info.setLineWrap(true);
            info.setWrapStyleWord(true);
            info.setForeground(TEXT);
            card.add(info, BorderLayout.NORTH);

            JPanel actions = new JPanel(new GridLayout(1, 2, 16, 0));
            actions.setOpaque(false);

            TapButton exportBtn = btn("Copy All Saved Data to Clipboard", PRIMARY);
            exportBtn.setPreferredSize(new Dimension(200, 56));
            exportBtn.addActionListener(e -> {
                if (!authorizeAdmin("export all application data"))
                    return;
                String payload = store.exportAppData();
                if (!payload.isEmpty()) {
                    StringSelection selection = new StringSelection(payload);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    store.log(ADMIN, "DATA_EXPORT", "Copied encrypted application database to clipboard");
                    alert("All saved data (Inventory, Salary, Orders, Reports, Drawer) copied to clipboard!\n\nYou can now switch to another system running Lumah Cafe and paste/import it.");
                }
            });

            TapButton importBtn = btn("Paste & Import Data from Clipboard", SUCCESS);
            importBtn.setPreferredSize(new Dimension(200, 56));
            importBtn.addActionListener(e -> {
                if (!authorizeAdmin("import and overwrite application data"))
                    return;
                try {
                    String clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                            .getData(DataFlavor.stringFlavor);
                    if (clipboardText == null || clipboardText.trim().isEmpty()) {
                        alert("Clipboard is empty.");
                        return;
                    }
                    int confirm = confirmWarning(
                            "Importing data will overwrite all existing records on this system.\nContinue?",
                            "Confirm Import");
                    if (confirm != JOptionPane.YES_OPTION)
                        return;

                    boolean success = store.importAppData(clipboardText);
                    if (success) {
                        store.log(ADMIN, "DATA_IMPORT", "Imported and replaced application database from clipboard");
                        refreshAll();
                        alert("Data successfully imported and synchronized!");
                    } else {
                        alert("Import failed: The clipboard data is invalid or not recognized by Lumah Cafe.");
                    }
                } catch (Exception ex) {
                    alert("Failed to read clipboard: " + ex.getMessage());
                }
            });

            actions.add(exportBtn);
            actions.add(importBtn);
            card.add(actions, BorderLayout.CENTER);

            JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
            centerWrap.setOpaque(false);
            centerWrap.add(card);
            add(centerWrap, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
        }
    }

    class AuditPanel extends Page {
        private final DefaultTableModel model = tableModel("Time", "User", "Action", "Details");
        private final JTextField search = new JTextField(20);

        AuditPanel() {
            add(heading("Admin Audit Log"), BorderLayout.NORTH);
            JTable t = new JTable(model);
            styleTable(t);
            t.getColumnModel().getColumn(0).setPreferredWidth(150);
            t.getColumnModel().getColumn(1).setPreferredWidth(70);
            t.getColumnModel().getColumn(2).setPreferredWidth(140);
            t.getColumnModel().getColumn(3).setPreferredWidth(700);
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            f.setOpaque(false);
            f.add(new JLabel("Filter:"));
            f.add(search);
            TapButton r = secondary("Refresh");
            r.addActionListener(e -> refresh());
            f.add(r);
            onChange(search, this::refresh);
            JPanel c = new JPanel(new BorderLayout(8, 8));
            c.setOpaque(false);
            c.add(f, BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(t);
            sp.getViewport().setBackground(FIELD);
            c.add(sp, BorderLayout.CENTER);
            add(c, BorderLayout.CENTER);
        }

        @Override
        void refresh() {
            String q = search.getText().trim().toLowerCase();
            model.setRowCount(0);
            for (int i = store.audit.size() - 1; i >= 0; i--) {
                AuditEntry a = store.audit.get(i);
                if (!q.isEmpty() && !(a.action.toLowerCase().contains(q) || a.details.toLowerCase().contains(q)
                        || a.user.toLowerCase().contains(q)))
                    continue;
                model.addRow(new Object[] { fmt(a.when), a.user, a.action, a.details });
            }
        }
    }

    static class MenuItem {
        String id, name, category;
        double price;
        boolean addon;
        Map<String, Double> recipe = new LinkedHashMap<>();

        MenuItem(String id) {
            this.id = id;
        }

        String csv() {
            String r = recipe.entrySet().stream().map(e -> e.getKey() + "~" + e.getValue())
                    .collect(Collectors.joining("|"));
            return join(id, name, category, String.valueOf(price), addon ? "ADDON" : "DRINK", r);
        }
    }

    static class Ingredient {
        String id, name, unit;
        double stock, threshold;

        Ingredient(String id) {
            this.id = id;
        }

        boolean isLow() {
            return stock <= threshold;
        }

        String status() {
            return stock <= 0 ? "OUT OF STOCK" : isLow() ? "LOW - RESTOCK" : "OK";
        }

        String csv() {
            return join(id, name, unit, String.valueOf(stock), String.valueOf(threshold));
        }

        @Override
        public String toString() {
            return name + " (" + unit + ")";
        }
    }

    static class AddOn {
        String id, name;
        double price;

        AddOn(String id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    static class OrderLine {
        String itemId, name;
        double price;
        int quantity;
        List<AddOn> addons;
        String temp = "";

        String displayName() {
            return temp.isEmpty() ? name : name + " [" + temp + "]";
        }

        OrderLine(String itemId, String name, double price, int quantity, List<AddOn> addons) {
            this.itemId = itemId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.addons = addons;
        }

        double total() {
            return (price + addons.stream().mapToDouble(a -> a.price).sum()) * quantity;
        }

        String encode() {
            String ao = addons.stream().map(a -> esc(a.id) + "^" + esc(a.name) + "^" + a.price)
                    .collect(Collectors.joining("+"));
            return esc(itemId) + "~" + esc(name) + "~" + price + "~" + quantity + "~" + ao + "~" + esc(temp);
        }
    }

    static class Order {
        String id, customer = "Walk-in", status = "Completed", notes = "", orderType = "Takeout", payMethod = "Cash",
                ref = "";
        LocalDateTime date = LocalDateTime.now();
        List<OrderLine> lines = new ArrayList<>();
        double total, payment, change;
        int table;

        Order(String id) {
            this.id = id;
        }

        boolean isSale() {
            return status.equals("Completed");
        }

        String typeLabel() {
            return orderType;
        }

        String payLabel() {
            return payMethod.equals("QR") ? "QR" : "Cash";
        }

        String itemsSummary() {
            return summary(lines);
        }

        String csv() {
            return join(id, date.toString(), customer, String.valueOf(total), String.valueOf(payment),
                    String.valueOf(change),
                    status, notes, lines.stream().map(OrderLine::encode).collect(Collectors.joining("|")),
                    orderType, String.valueOf(table), payMethod, ref);
        }
    }

    static class PayoutRecord {
        String id, staffName, startDate, endDate, extraDates = "";
        int totalCups;
        double totalPayout;
        LocalDateTime recordedAt = LocalDateTime.now();

        PayoutRecord(String id) {
            this.id = id;
        }

        String csv() {
            return join(id, staffName, startDate, endDate, extraDates, String.valueOf(totalCups),
                    String.valueOf(totalPayout), recordedAt.toString());
        }
    }

    static class DrawerEntry {
        String id, type = "CASH_IN", note = "", user = "";
        LocalDateTime when = LocalDateTime.now();
        double amount;

        DrawerEntry(String id) {
            this.id = id;
        }

        String typeLabel() {
            switch (type) {
                case "FLOAT":
                    return "Starting Float";
                case "CASH_IN":
                    return "Cash In";
                case "DROP":
                    return "Cash Drop";
                case "PAYOUT":
                    return "Payout / Expense";
                case "COUNT":
                    return "Closing Count";
                default:
                    return type;
            }
        }

        String csv() {
            return join(id, when.toString(), type, String.valueOf(amount), note, user);
        }
    }

    static class AuditEntry {
        LocalDateTime when;
        String user, action, details;

        AuditEntry(LocalDateTime when, String user, String action, String details) {
            this.when = when;
            this.user = user;
            this.action = action;
            this.details = details;
        }

        String csv() {
            return join(when.toString(), user, action, details);
        }
    }

    static class Store {
        final Path dataDir = Paths.get("data");
        final Path menuFile = dataDir.resolve("menu.csv");
        final Path stockFile = dataDir.resolve("stock.csv");
        final Path ordersFile = dataDir.resolve("orders.csv");
        final Path drawerFile = dataDir.resolve("drawer.csv");
        final Path auditFile = dataDir.resolve("audit.csv");
        final Path payoutHistoryFile = dataDir.resolve("payout_history.csv");
        final Path configFile = dataDir.resolve("config.properties");
        final List<MenuItem> items = new ArrayList<>();
        final List<Ingredient> ingredients = new ArrayList<>();
        final List<Order> orders = new ArrayList<>();
        final List<DrawerEntry> drawer = new ArrayList<>();
        final List<AuditEntry> audit = new ArrayList<>();
        final List<PayoutRecord> payoutHistory = new ArrayList<>();
        final Map<String, Double> commissions = new LinkedHashMap<>();
        final Properties config = new Properties();
        Consumer<String> onError = System.err::println;

        void load() {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                onError.accept("Unable to create data folder: " + e.getMessage());
            }
            loadConfig();
            loadStock();
            loadMenu();
            loadOrders();
            loadDrawer();
            loadAudit();
            loadPayoutHistory();
            if (items.isEmpty() && ingredients.isEmpty())
                seed();
            saveMenu();
            saveStock();
            saveOrders();
            saveDrawer();
            saveAudit();
            savePayoutHistory();
        }

        String exportAppData() {
            try {
                Map<String, String> filesMap = new LinkedHashMap<>();
                Path[] targets = { menuFile, stockFile, ordersFile, drawerFile, auditFile, payoutHistoryFile,
                        configFile };
                for (Path p : targets) {
                    if (Files.exists(p)) {
                        String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                        filesMap.put(p.getFileName().toString(), content);
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("LUMAH_SECURE_BACKUP_V1\n");
                for (Map.Entry<String, String> e : filesMap.entrySet()) {
                    sb.append("FILE:").append(e.getKey()).append("\n");
                    sb.append("LENGTH:").append(e.getValue().getBytes(StandardCharsets.UTF_8).length).append("\n");
                    sb.append(e.getValue());
                    if (!e.getValue().endsWith("\n"))
                        sb.append("\n");
                    sb.append("END_FILE\n");
                }
                return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                onError.accept("Export failed: " + e.getMessage());
                return "";
            }
        }

        boolean importAppData(String base64Data) {
            try {
                byte[] decoded = Base64.getDecoder().decode(base64Data.trim());
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                if (!decodedStr.startsWith("LUMAH_SECURE_BACKUP_V1")) {
                    return false;
                }

                Files.createDirectories(dataDir);
                Scanner scanner = new Scanner(decodedStr);
                String line;
                String currentFile = null;
                StringBuilder fileContent = new StringBuilder();
                boolean readingFile = false;

                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    if (line.startsWith("FILE:")) {
                        currentFile = line.substring(5).trim();
                        fileContent.setLength(0);
                        readingFile = false;
                    } else if (line.startsWith("LENGTH:")) {
                        readingFile = true;
                    } else if (line.equals("END_FILE")) {
                        if (currentFile != null) {
                            Path targetPath = dataDir.resolve(currentFile);
                            Files.write(targetPath, fileContent.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        currentFile = null;
                        readingFile = false;
                    } else if (readingFile) {
                        fileContent.append(line).append("\n");
                    }
                }
                scanner.close();

                items.clear();
                ingredients.clear();
                orders.clear();
                drawer.clear();
                audit.clear();
                payoutHistory.clear();
                commissions.clear();
                config.clear();

                loadConfig();
                loadStock();
                loadMenu();
                loadOrders();
                loadDrawer();
                loadAudit();
                loadPayoutHistory();
                return true;
            } catch (Exception e) {
                onError.accept("Import failed: " + e.getMessage());
                return false;
            }
        }

        private void write(Path f, List<String> lines) {
            try {
                Files.createDirectories(dataDir);
                Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
                Files.write(tmp, lines, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                onError.accept("Unable to save " + f.getFileName() + ": " + e.getMessage());
            }
        }

        void saveMenu() {
            write(menuFile, items.stream().map(MenuItem::csv).collect(Collectors.toList()));
        }

        void saveStock() {
            write(stockFile, ingredients.stream().map(Ingredient::csv).collect(Collectors.toList()));
        }

        void saveOrders() {
            write(ordersFile, orders.stream().map(Order::csv).collect(Collectors.toList()));
        }

        void saveDrawer() {
            write(drawerFile, drawer.stream().map(DrawerEntry::csv).collect(Collectors.toList()));
        }

        void saveAudit() {
            write(auditFile, audit.stream().map(AuditEntry::csv).collect(Collectors.toList()));
        }

        void savePayoutHistory() {
            write(payoutHistoryFile, payoutHistory.stream().map(PayoutRecord::csv).collect(Collectors.toList()));
        }

        private List<String> lines(Path f) {
            try {
                return Files.exists(f) ? Files.readAllLines(f, StandardCharsets.UTF_8) : new ArrayList<>();
            } catch (IOException e) {
                onError.accept("Unable to read " + f.getFileName() + ": " + e.getMessage());
                return new ArrayList<>();
            }
        }

        private void loadMenu() {
            for (String line : lines(menuFile)) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    List<String> p = split(line);
                    if (p.size() < 4)
                        continue;
                    MenuItem m = new MenuItem(p.get(0));
                    m.name = p.get(1);
                    m.category = p.get(2);
                    m.price = Double.parseDouble(p.get(3));
                    m.addon = p.size() > 4 && p.get(4).equals("ADDON");
                    if (p.size() > 5) {
                        for (String part : p.get(5).split("\\|")) {
                            String[] x = part.split("~");
                            if (x.length == 2)
                                m.recipe.put(x[0], Double.parseDouble(x[1]));
                        }
                    }
                    items.add(m);
                } catch (RuntimeException ex) {
                    System.err.println("Skipping bad menu row: " + line);
                }
            }
        }

        private void loadStock() {
            for (String line : lines(stockFile)) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    List<String> p = split(line);
                    if (p.size() < 5)
                        continue;
                    Ingredient g = new Ingredient(p.get(0));
                    g.name = p.get(1);
                    g.unit = p.get(2);
                    g.stock = Double.parseDouble(p.get(3));
                    g.threshold = Double.parseDouble(p.get(4));
                    ingredients.add(g);
                } catch (RuntimeException ex) {
                    System.err.println("Skipping bad stock row: " + line);
                }
            }
        }

        private void loadOrders() {
            for (String line : lines(ordersFile)) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    List<String> p = split(line);
                    if (p.size() < 9)
                        continue;
                    Order o = new Order(p.get(0));
                    o.date = LocalDateTime.parse(p.get(1));
                    o.customer = p.get(2);
                    o.total = Double.parseDouble(p.get(3));
                    o.payment = Double.parseDouble(p.get(4));
                    o.change = Double.parseDouble(p.get(5));
                    o.status = p.get(6);
                    o.notes = p.get(7);
                    for (String part : p.get(8).split("\\|")) {
                        if (part.isEmpty())
                            continue;
                        String[] x = part.split("~", -1);
                        if (x.length < 4)
                            continue;
                        List<AddOn> ao = new ArrayList<>();
                        if (x.length >= 5 && !x[4].isEmpty()) {
                            for (String a : x[4].split("\\+")) {
                                String[] y = a.split("\\^", -1);
                                if (y.length == 3)
                                    ao.add(new AddOn(y[0], y[1], Double.parseDouble(y[2])));
                            }
                        }
                        OrderLine ol = new OrderLine(x[0], x[1], Double.parseDouble(x[2]), Integer.parseInt(x[3]), ao);
                        if (x.length >= 6)
                            ol.temp = x[5];
                        o.lines.add(ol);
                    }
                    if (p.size() > 9 && !p.get(9).isEmpty())
                        o.orderType = p.get(9);
                    if (p.size() > 10 && !p.get(10).isEmpty())
                        o.table = Integer.parseInt(p.get(10));
                    if (p.size() > 11 && !p.get(11).isEmpty())
                        o.payMethod = p.get(11);
                    if (p.size() > 12)
                        o.ref = p.get(12);
                    orders.add(o);
                } catch (RuntimeException ex) {
                    System.err.println("Skipping bad order row: " + line);
                }
            }
        }

        private void loadDrawer() {
            for (String line : lines(drawerFile)) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    List<String> p = split(line);
                    if (p.size() < 6)
                        continue;
                    DrawerEntry e = new DrawerEntry(p.get(0));
                    e.when = LocalDateTime.parse(p.get(1));
                    e.type = p.get(2);
                    e.amount = Double.parseDouble(p.get(3));
                    e.note = p.get(4);
                    e.user = p.get(5);
                    drawer.add(e);
                } catch (RuntimeException ex) {
                    System.err.println("Skipping bad drawer row: " + line);
                }
            }
        }

        private void loadAudit() {
            for (String line : lines(auditFile)) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    List<String> p = split(line);
                    if (p.size() < 4)
                        continue;
                    audit.add(new AuditEntry(LocalDateTime.parse(p.get(0)), p.get(1), p.get(2), p.get(3)));
                } catch (RuntimeException ex) {
                    System.err.println("Skipping bad audit row: " + line);
                }
            }
        }

        private void loadPayoutHistory() {
            for (String line : lines(payoutHistoryFile)) {
                if (line.trim().isEmpty())
                    continue;
                try {
                    List<String> p = split(line);
                    if (p.size() < 7)
                        continue;
                    PayoutRecord ph = new PayoutRecord(p.get(0));
                    ph.staffName = p.get(1);
                    ph.startDate = p.get(2);
                    ph.endDate = p.get(3);
                    if (p.size() >= 8) {
                        ph.extraDates = p.get(4);
                        ph.totalCups = Integer.parseInt(p.get(5));
                        ph.totalPayout = Double.parseDouble(p.get(6));
                        ph.recordedAt = LocalDateTime.parse(p.get(7));
                    } else {
                        ph.totalCups = Integer.parseInt(p.get(4));
                        ph.totalPayout = Double.parseDouble(p.get(5));
                        ph.recordedAt = LocalDateTime.parse(p.get(6));
                    }
                    payoutHistory.add(ph);
                } catch (RuntimeException ex) {
                    System.err.println("Skipping bad payout history row: " + line);
                }
            }
        }

        private void loadConfig() {
            if (Files.exists(configFile)) {
                try (java.io.InputStream in = Files.newInputStream(configFile)) {
                    config.load(in);
                } catch (IOException e) {
                    onError.accept("Unable to read config: " + e.getMessage());
                }
            }
            if (config.getProperty("admin.salt") == null || config.getProperty("admin.hash") == null) {
                setPassword("6900".toCharArray());
            }

            commissions.put(CAT_SODA, Double.parseDouble(config.getProperty("comm.soda", "10.0")));
            commissions.put(CAT_NONCOFFEE, Double.parseDouble(config.getProperty("comm.noncoffee", "11.0")));
            commissions.put(CAT_COFFEE, Double.parseDouble(config.getProperty("comm.coffee", "12.0")));
            commissions.put(CAT_SPECIALS, Double.parseDouble(config.getProperty("comm.specials", "13.0")));

            config.forEach((k, v) -> {
                String key = String.valueOf(k);
                if (key.startsWith("comm.custom.")) {
                    commissions.put(key.substring(12), Double.parseDouble(String.valueOf(v)));
                }
            });
        }

        void saveConfig() {
            commissions.forEach((cat, rate) -> {
                if (cat.equals(CAT_SODA))
                    config.setProperty("comm.soda", String.valueOf(rate));
                else if (cat.equals(CAT_NONCOFFEE))
                    config.setProperty("comm.noncoffee", String.valueOf(rate));
                else if (cat.equals(CAT_COFFEE))
                    config.setProperty("comm.coffee", String.valueOf(rate));
                else if (cat.equals(CAT_SPECIALS))
                    config.setProperty("comm.specials", String.valueOf(rate));
                else
                    config.setProperty("comm.custom." + cat, String.valueOf(rate));
            });
            try {
                Files.createDirectories(dataDir);
                try (java.io.OutputStream out = Files.newOutputStream(configFile)) {
                    config.store(out, "Lumah Cafe configuration & staff commissions.");
                }
            } catch (IOException e) {
                onError.accept("Unable to save config: " + e.getMessage());
            }
        }

        private String sha(String salt, char[] pw) {
            try {
                byte[] b = MessageDigest.getInstance("SHA-256")
                        .digest((salt + new String(pw)).getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte x : b)
                    sb.append(String.format("%02x", x));
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        boolean checkPassword(char[] pw) {
            String salt = config.getProperty("admin.salt", "");
            String h = sha(salt, pw);
            return MessageDigest.isEqual(h.getBytes(StandardCharsets.UTF_8),
                    config.getProperty("admin.hash", "").getBytes(StandardCharsets.UTF_8));
        }

        void setPassword(char[] pw) {
            byte[] s = new byte[16];
            new SecureRandom().nextBytes(s);
            StringBuilder salt = new StringBuilder();
            for (byte x : s)
                salt.append(String.format("%02x", x));
            config.setProperty("admin.salt", salt.toString());
            config.setProperty("admin.hash", sha(salt.toString(), pw));
            saveConfig();
        }

        void log(String user, String action, String details) {
            audit.add(new AuditEntry(LocalDateTime.now(), user, action,
                    details == null ? "" : details.replace('\n', ' ')));
            saveAudit();
        }

        private int numeric(String id, String prefix) {
            try {
                return Integer.parseInt(id.replace(prefix, ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        String nextItemId(boolean addon) {
            String p = addon ? "ADD-" : "MNU-";
            return String.format("%s%04d", p,
                    items.stream().filter(i -> i.id.startsWith(p)).mapToInt(i -> numeric(i.id, p)).max().orElse(0) + 1);
        }

        String nextIngId() {
            return String.format("ING-%04d",
                    ingredients.stream().mapToInt(i -> numeric(i.id, "ING-")).max().orElse(0) + 1);
        }

        String nextOrderId() {
            return String.format("ORD-%04d", orders.stream().mapToInt(o -> numeric(o.id, "ORD-")).max().orElse(0) + 1);
        }

        String nextDrawerId() {
            return String.format("DRW-%04d", drawer.stream().mapToInt(o -> numeric(o.id, "DRW-")).max().orElse(0) + 1);
        }

        String nextPayoutId() {
            return String.format("PAY-%04d",
                    payoutHistory.stream().mapToInt(o -> numeric(o.id, "PAY-")).max().orElse(0) + 1);
        }

        MenuItem findItem(String id) {
            return items.stream().filter(i -> i.id.equals(id)).findFirst().orElse(null);
        }

        MenuItem findItemByName(String n, boolean addon) {
            return items.stream().filter(i -> i.addon == addon && i.name.equalsIgnoreCase(n)).findFirst().orElse(null);
        }

        Ingredient findIng(String id) {
            return ingredients.stream().filter(i -> i.id.equals(id)).findFirst().orElse(null);
        }

        Order findOrder(String id) {
            return orders.stream().filter(o -> o.id.equals(id)).findFirst().orElse(null);
        }

        PayoutRecord findPayoutRecord(String id) {
            return payoutHistory.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
        }

        List<MenuItem> addons() {
            return items.stream().filter(i -> i.addon).collect(Collectors.toList());
        }

        List<String> drinkCategories() {
            List<String> c = new ArrayList<>(Arrays.asList(DEFAULT_CATS));
            for (MenuItem i : items)
                if (!i.addon && !c.contains(i.category))
                    c.add(i.category);
            for (String customCat : commissions.keySet()) {
                if (!c.contains(customCat))
                    c.add(customCat);
            }
            return c;
        }

        String recipeText(Map<String, Double> r) {
            if (r.isEmpty())
                return "(no recipe)";
            return r.entrySet().stream().map(e -> {
                Ingredient g = findIng(e.getKey());
                return g == null ? e.getKey() : num(e.getValue()) + " " + g.unit + " " + g.name;
            }).collect(Collectors.joining(", "));
        }

        Map<String, Double> consumption(List<OrderLine> lines, String orderType) {
            Map<String, Double> need = new LinkedHashMap<>();

            String freshMilkId = ingredients.stream()
                    .filter(g -> "Fresh Milk".equalsIgnoreCase(g.name))
                    .map(g -> g.id).findFirst().orElse(null);

            for (OrderLine l : lines) {
                MenuItem m = findItem(l.itemId);
                boolean hasOatside = l.addons.stream().anyMatch(a -> a.name.equalsIgnoreCase("Oatside"));

                if (m != null) {
                    m.recipe.forEach((k, v) -> {
                        if (hasOatside && k.equals(freshMilkId)) {
                            return;
                        }
                        need.merge(k, v * l.quantity, Double::sum);
                    });
                }
                for (AddOn a : l.addons) {
                    MenuItem am = findItem(a.id);
                    if (am != null)
                        am.recipe.forEach((k, v) -> need.merge(k, v * l.quantity, Double::sum));
                }
            }
            if ("Dine-In".equals(orderType)) {
                ingredients.stream()
                        .filter(g -> "Cup & Lid".equalsIgnoreCase(g.name))
                        .findFirst()
                        .ifPresent(cupIng -> need.remove(cupIng.id));
            }
            return need;
        }

        List<String> shortages(Map<String, Double> need) {
            List<String> out = new ArrayList<>();
            need.forEach((k, v) -> {
                Ingredient g = findIng(k);
                if (g != null && g.stock + 1e-9 < v)
                    out.add(g.name + " (need " + num(v) + " " + g.unit + ", have " + num(g.stock) + ")");
            });
            return out;
        }

        void deduct(Map<String, Double> need) {
            need.forEach((k, v) -> {
                Ingredient g = findIng(k);
                if (g != null)
                    g.stock = round2(Math.max(0, g.stock - v));
            });
        }

        void restore(Map<String, Double> need) {
            need.forEach((k, v) -> {
                Ingredient g = findIng(k);
                if (g != null)
                    g.stock = round2(g.stock + v);
            });
        }

        List<Ingredient> lowStock() {
            return ingredients.stream().filter(Ingredient::isLow)
                    .sorted(Comparator.comparingDouble(g -> g.stock / Math.max(1, g.threshold)))
                    .collect(Collectors.toList());
        }

        private Ingredient ing(String name, String unit, double stock, double threshold) {
            Ingredient g = new Ingredient(nextIngId());
            g.name = name;
            g.unit = unit;
            g.stock = stock;
            g.threshold = threshold;
            ingredients.add(g);
            return g;
        }

        private void menu(String name, String cat, double price, boolean addon, Object... recipe) {
            MenuItem m = new MenuItem(nextItemId(addon));
            m.name = name;
            m.category = cat;
            m.price = price;
            m.addon = addon;
            for (int i = 0; i + 1 < recipe.length; i += 2)
                m.recipe.put(((Ingredient) recipe[i]).id, ((Number) recipe[i + 1]).doubleValue());
            items.add(m);
        }

        void seed() {
            Ingredient espresso = ing("Espresso", "shot", 400, 50);
            Ingredient milk = ing("Fresh Milk", "ml", 24000, 3000);
            Ingredient oat = ing("Oatside Oat Milk", "ml", 6000, 1000);
            Ingredient fructose = ing("Fructose", "pump", 300, 40);
            Ingredient chocSyrup = ing("Chocolate Syrup", "ml", 3000, 400);
            Ingredient saltedCaramel = ing("Salted Caramel Syrup", "pump", 200, 25);
            Ingredient vanilla = ing("Vanilla Syrup", "pump", 300, 40);
            Ingredient condensed = ing("Condensed Milk", "ml", 4000, 600);
            Ingredient saltCream = ing("Sea Salt Cream", "ml", 4000, 600);
            Ingredient strawberryJam = ing("Strawberry Jam", "ml", 3000, 400);
            Ingredient strawberryPowder = ing("Strawberry Powder", "g", 3000, 400);
            Ingredient chocPowder = ing("Chocolate Powder", "ml", 3000, 400);
            Ingredient milo = ing("Milo Powder", "g", 3000, 400);
            Ingredient hojicha = ing("Hojicha Powder", "g", 600, 100);
            Ingredient matcha = ing("Matcha Powder", "g", 600, 100);
            Ingredient hotWater = ing("Hot Water", "ml", 20000, 3000);
            Ingredient agave = ing("Agave Syrup", "ml", 1500, 200);
            Ingredient sodaWater = ing("Soda Water", "ml", 20000, 3000);

            Ingredient genericSodaSyrup = ing("Soda Syrup", "ml", 3000, 400);
            Ingredient strawberrySodaSyrup = ing("Strawberry Syrup", "ml", 3000, 400);
            Ingredient blueLemonadeSyrup = ing("Blue Lemonade Syrup", "ml", 3000, 400);
            Ingredient blueberrySyrup = ing("Blueberry Syrup", "ml", 3000, 400);
            Ingredient greenAppleSyrup = ing("Green Apple Syrup", "ml", 3000, 400);

            Ingredient rainbowJelly = ing("Rainbow Jelly", "scoop", 200, 30);
            Ingredient cup = ing("Cup & Lid", "pcs", 500, 80);

            String cafe = "Caf\u00e9";

            menu(cafe + " Latte", CAT_COFFEE, 79, false, fructose, 1, espresso, 2, milk, 120, cup, 1);
            menu(cafe + " Mocha", CAT_COFFEE, 89, false, chocSyrup, 30, espresso, 2, milk, 120, cup, 1);
            menu("Spanish Latte", CAT_COFFEE, 89, false, condensed, 30, espresso, 2, milk, 120, cup, 1);
            menu("Sea Salt Latte", CAT_COFFEE, 99, false, condensed, 20, milk, 120, espresso, 2, saltCream, 45, cup, 1);
            menu("Salted Caramel", CAT_COFFEE, 89, false, saltedCaramel, 2, vanilla, 1, milk, 120, espresso, 2, cup, 1);

            menu("Choco", CAT_NONCOFFEE, 79, false, chocPowder, 30, milk, 120, cup, 1);
            menu("Strawberry", CAT_NONCOFFEE, 89, false, strawberryJam, 30, strawberryPowder, 20, milk, 120, cup, 1);
            menu("Milo Dino", CAT_NONCOFFEE, 79, false, condensed, 10, milo, 20, milk, 100, cup, 1);

            menu("Hojicha Latte", CAT_SPECIALS, 99, false, hojicha, 4, hotWater, 40, condensed, 20, fructose, 1, milk,
                    100, cup, 1);
            menu("Sea Salt Hojicha", CAT_SPECIALS, 119, false, hojicha, 4, hotWater, 40, agave, 30, milk, 80, saltCream,
                    45, cup, 1);
            menu("Matcha Latte", CAT_SPECIALS, 109, false, matcha, 4, hotWater, 40, condensed, 20, fructose, 1, milk,
                    100, cup, 1);
            menu("Sea Salt Matcha", CAT_SPECIALS, 129, false, matcha, 4, hotWater, 40, agave, 30, milk, 80, saltCream,
                    45, cup, 1);

            menu("Soda", CAT_SODA, 59, false, genericSodaSyrup, 20, sodaWater, 120, rainbowJelly, 1.5, cup, 1);
            menu("Strawberry Soda", CAT_SODA, 59, false, strawberrySodaSyrup, 20, sodaWater, 120, rainbowJelly, 1.5,
                    cup, 1);
            menu("Blue Lemonade Soda", CAT_SODA, 59, false, blueLemonadeSyrup, 20, sodaWater, 120, rainbowJelly, 1.5,
                    cup, 1);
            menu("Blueberry Soda", CAT_SODA, 59, false, blueberrySyrup, 20, sodaWater, 120, rainbowJelly, 1.5, cup, 1);
            menu("Green Apple Soda", CAT_SODA, 59, false, greenAppleSyrup, 20, sodaWater, 120, rainbowJelly, 1.5, cup,
                    1);

            menu("Sea Salt", CAT_ADDONS, 20, true, saltCream, 45);
            menu("Oatside", CAT_ADDONS, 20, true, oat, 60);
            menu("Espresso", CAT_ADDONS, 20, true, espresso, 1);
            menu("Agave", CAT_ADDONS, 20, true, agave, 20);
            menu("Vanilla", CAT_ADDONS, 10, true, vanilla, 1);
        }
    }

    static String summary(List<OrderLine> lines) {
        return lines.stream().map(l -> l.displayName() + " x" + l.quantity
                + (l.addons.isEmpty() ? ""
                        : " (+" + l.addons.stream().map(a -> a.name).collect(Collectors.joining(", +")) + ")"))
                .collect(Collectors.joining("; "));
    }

    static String join(String... vals) {
        return Arrays.stream(vals).map(Lumah::csvEsc).collect(Collectors.joining(","));
    }

    static String csvEsc(String v) {
        if (v == null)
            v = "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else
                    q = !q;
            } else if (c == ',' && !q) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    static String esc(String s) {
        return s == null ? ""
                : s.replace("~", " ").replace("|", " ").replace("^", " ").replace("+", " ").replace("\n", " ");
    }
}