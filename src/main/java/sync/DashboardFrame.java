package sync;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFrame extends JFrame {

    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel lastInLabel;
    private JLabel lastOutLabel;
    private DefaultTableModel historyModel;
    private JButton btnIn;
    private JButton btnOut;
    private JProgressBar progressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DashboardFrame() {
        super("🔄 Sync Monitor - PostgreSQL ↔ MariaDB");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(820, 580));
        setLocationRelativeTo(null);
        setBackground(new Color(245, 236, 240));

        initUI();
        refreshHistory();
    }

    private void initUI() {
        JPanel root = darkPanel(new BorderLayout(0, 0));
        setContentPane(root);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildCenter(),  BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = darkPanel(new FlowLayout(FlowLayout.LEFT, 20, 14));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 210, 220)));

        JLabel title = label("🔄  Sync Monitor", Font.BOLD, 16, new Color(55, 65, 81));
        JLabel master = pill("MASTER · PostgreSQL/Pagila", new Color(254, 174, 207));
        JLabel arrow  = label("↔", Font.PLAIN, 14, new Color(107, 114, 128));
        JLabel slave  = pill("SLAVE · MariaDB", new Color(236, 72, 153));

        p.add(title); p.add(Box.createHorizontalStrut(8));
        p.add(master); p.add(arrow); p.add(slave);
        return p;
    }

    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                           buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(400);
        split.setBackground(new Color(245, 236, 240));
        split.setBorder(null);
        split.setDividerSize(4);
        return split;
    }

    private JPanel buildLeftPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(16, 16, 8, 8));

        // Botones
        JPanel btnPanel = darkPanel(new GridLayout(1, 2, 10, 0));
        btnIn  = syncButton("⬇  SYNC-IN",  "Master → Slave (tablas IN)",  new Color(254, 174, 207));
        btnOut = syncButton("⬆  SYNC-OUT", "Slave → Master (tablas OUT)", new Color(236, 72, 153));
        btnIn.addActionListener(e  -> runSync("IN"));
        btnOut.addActionListener(e -> runSync("OUT"));
        btnPanel.add(btnIn);
        btnPanel.add(btnOut);

        // Tarjetas de última sync
        JPanel cards = darkPanel(new GridLayout(1, 2, 10, 0));
        JPanel cardIn  = buildStatCard("Última SYNC-IN",  lastInLabel  = label("—", Font.PLAIN, 13, new Color(107, 114, 128)));
        JPanel cardOut = buildStatCard("Última SYNC-OUT", lastOutLabel = label("—", Font.PLAIN, 13, new Color(107, 114, 128)));
        cards.add(cardIn); cards.add(cardOut);

        // Log en vivo
        logArea = new JTextArea();
        logArea.setBackground(new Color(250, 245, 248));   
        logArea.setForeground(new Color(75, 85, 99));      
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 210, 220)));
        scroll.getViewport().setBackground(logArea.getBackground());

        JLabel logTitle = label("Log en vivo", Font.BOLD, 12, new Color(107, 114, 128));
        logTitle.setBorder(new EmptyBorder(8, 0, 4, 0));

        JPanel top = darkPanel(new BorderLayout(0, 10));
        top.add(btnPanel, BorderLayout.NORTH);
        top.add(cards,    BorderLayout.CENTER);

        p.add(top,      BorderLayout.NORTH);
        p.add(logTitle, BorderLayout.CENTER);
        p.add(scroll,   BorderLayout.SOUTH);
        ((BorderLayout)p.getLayout()).setVgap(0);

        p.setPreferredSize(new Dimension(390, 0));
        scroll.setPreferredSize(new Dimension(0, 280));

        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = darkPanel(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(16, 8, 8, 16));

        JLabel title = label("Historial de sincronizaciones", Font.BOLD, 13, new Color(55, 65, 81));
        title.setBorder(new EmptyBorder(0, 0, 8, 0));

        String[] cols = {"Tipo", "Inicio", "Fin", "Estado", "Errores"};
        historyModel  = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(historyModel);
        table.setBackground(new Color(252, 244, 248));
        table.setForeground(new Color(55, 65, 81));
        table.setGridColor(new Color(230, 210, 220));
        table.setRowHeight(26);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setSelectionBackground(new Color(230, 210, 220));
        table.setSelectionForeground(new Color(55, 65, 81));
        table.getTableHeader().setBackground(new Color(20, 22, 32));
        table.getTableHeader().setForeground(new Color(107, 114, 128));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        table.getColumnModel().getColumn(4).setMaxWidth(60);

        // Renderer de color por estado
        table.setDefaultRenderer(Object.class, (tbl, val, sel, foc, row, col) -> {
            JLabel cell = new JLabel(val != null ? val.toString() : "");
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));
            cell.setBackground(sel ? new Color(230, 210, 220) : (row % 2 == 0 ? new Color(252, 244, 248) : new Color(248, 240, 246)));

            if (col == 0) {
                cell.setForeground("SYNC-IN".equals(val) ? new Color(254, 174, 207) : new Color(236, 72, 153));
                cell.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            } else if (col == 3) {
                String s = val != null ? val.toString() : "";
                cell.setForeground(switch (s) {
                    case "OK"      -> new Color(254, 174, 207);
                    case "PARCIAL" -> new Color(251, 191, 36);
                    default        -> new Color(239, 68, 68);
                });
                cell.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            } else {
                cell.setForeground(new Color(55, 65, 81));
            }
            return cell;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 210, 220)));
        scroll.getViewport().setBackground(new Color(252, 244, 248));

        JButton refreshBtn = new JButton("↻ Actualizar");
        refreshBtn.setBackground(new Color(230, 210, 220));
        refreshBtn.setForeground(new Color(55, 65, 81));
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refreshHistory());

        JPanel hdr = darkPanel(new BorderLayout());
        hdr.add(title, BorderLayout.WEST);
        hdr.add(refreshBtn, BorderLayout.EAST);

        p.add(hdr,    BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = darkPanel(new BorderLayout(10, 0));
        p.setBorder(new EmptyBorder(6, 16, 6, 16));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 210, 220)),
            new EmptyBorder(6, 16, 6, 16)
        ));

        statusLabel = label("Listo.", Font.PLAIN, 12, new Color(107, 114, 128));
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setPreferredSize(new Dimension(160, 14));
        progressBar.setBackground(new Color(230, 210, 220));
        progressBar.setForeground(new Color(254, 174, 207));
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);

        p.add(statusLabel, BorderLayout.WEST);
        p.add(progressBar, BorderLayout.EAST);
        return p;
    }

 
    private void runSync(String type) {
        btnIn.setEnabled(false);
        btnOut.setEnabled(false);
        logArea.setText("");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setForeground(new Color(251, 191, 36));
        statusLabel.setText("⟳ " + (type.equals("IN") ? "SYNC-IN" : "SYNC-OUT") + " en curso...");

        executor.submit(() -> {
            SyncResult result = type.equals("IN")
                    ? SyncIn.run(this::appendLog)
                    : SyncOut.run(this::appendLog);

            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                btnIn.setEnabled(true);
                btnOut.setEnabled(true);

                Color statusColor = switch (result.status) {
                    case SyncResult.STATUS_OK      -> new Color(254, 174, 207);
                    case SyncResult.STATUS_PARTIAL -> new Color(251, 191, 36);
                    default                        -> new Color(239, 68, 68);
                };
                statusLabel.setForeground(statusColor);
                statusLabel.setText(result.type + " finalizado — " + result.status +
                        " (" + result.finishedAt + ")");

                refreshHistory();
            });
        });
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

     private void refreshHistory() {
        List<SyncResult> history = SyncHistory.load();
        historyModel.setRowCount(0);

        String lastIn = null, lastOut = null;

        for (SyncResult r : history) {
            historyModel.addRow(new Object[]{
                r.type, r.startedAt, r.finishedAt, r.status, r.totalErrors
            });
            if (lastIn == null  && "SYNC-IN".equals(r.type))  lastIn  = r.finishedAt + " — " + r.status;
            if (lastOut == null && "SYNC-OUT".equals(r.type)) lastOut = r.finishedAt + " — " + r.status;
        }

        lastInLabel.setText(lastIn   != null ? lastIn  : "Sin registros");
        lastOutLabel.setText(lastOut != null ? lastOut : "Sin registros");
        lastInLabel.setForeground(colorForStatus(lastIn));
        lastOutLabel.setForeground(colorForStatus(lastOut));
    }

    private Color colorForStatus(String s) {
        if (s == null)           return new Color(107, 114, 128);
        if (s.contains("OK"))    return new Color(254, 174, 207);
        if (s.contains("PARCI")) return new Color(251, 191, 36);
        return new Color(239, 68, 68);
    }


    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(new Color(245, 236, 240));
        return p;
    }

    private JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font(Font.SANS_SERIF, style, size));
        return l;
    }

    private JLabel pill(String text, Color color) {
        JLabel l = new JLabel(" " + text + " ");
        l.setForeground(color);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        l.setOpaque(true);
        l.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
        l.setBorder(BorderFactory.createLineBorder(
            new Color(color.getRed(), color.getGreen(), color.getBlue(), 80), 1, true));
        return l;
    }

    private JButton syncButton(String title, String subtitle, Color accent) {
        JButton btn = new JButton("<html><center><b>" + title + "</b><br>" +
                "<span style='font-size:9px;color:#888'>" + subtitle + "</span></center></html>");
        btn.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
        btn.setForeground(accent);
        btn.setBorder(BorderFactory.createLineBorder(
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80)));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(0, 64));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        return btn;
    }

    private JPanel buildStatCard(String title, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(new Color(252, 244, 248));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 210, 220)),
            new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel t = label(title, Font.BOLD, 10, new Color(107, 114, 128));
        p.add(t, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true));
    }
}