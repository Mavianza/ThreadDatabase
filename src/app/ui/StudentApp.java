package app.ui;

import app.model.Student;
import app.service.StudentService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentApp extends JFrame {
    private final JTextField tfName = new JTextField(12);
    private final JTextField tfAge  = new JTextField(4);

    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Name","Age"}, 0) {
        public boolean isCellEditable(int r, int c){ return c != 0; } // ID read-only
    };
    private final JTable table = new JTable(model);

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnUpd = new JButton("Update");
    private final JButton btnDel = new JButton("Delete");
    private final JButton btnRef = new JButton("Refresh");
    private final JLabel  status = new JLabel("Idle");

    private final StudentService service = new StudentService();

    // indikator busy (threading)
    private final JProgressBar loading = new JProgressBar();
    private final JPanel glass = new JPanel() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0,0,0,120));
            g2.fillRect(0,0,getWidth(),getHeight());
            g2.dispose();
        }
    };
    private final JLabel glassText = new JLabel("Loading…");
    private final javax.swing.Timer dotsAnim;
    private String baseStatus = "Idle";

    // panel animasi bola (thread per student)
    private final BallPanel ballPanel = new BallPanel();

    public StudentApp(){
        super("Students — JDBC + Threads + Swing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 520);
        setLocationRelativeTo(null);

        // TOP panel (form + tombol)
        JPanel top = new JPanel();
        top.add(new JLabel("Name")); top.add(tfName);
        top.add(new JLabel("Age"));  top.add(tfAge);
        top.add(btnAdd); top.add(btnUpd); top.add(btnDel); top.add(btnRef);

        // kiri: table, kanan: ballPanel (split pane)
        JScrollPane tableScroll = new JScrollPane(table);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, ballPanel);
        split.setResizeWeight(0.6);

        // status + progress
        loading.setIndeterminate(true);
        loading.setVisible(false);
        JPanel south = new JPanel(new BorderLayout());
        south.add(status, BorderLayout.WEST);
        south.add(loading, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // glass overlay
        glass.setOpaque(false);
        glass.setLayout(new GridBagLayout());
        glassText.setForeground(Color.WHITE);
        glassText.setFont(glassText.getFont().deriveFont(Font.BOLD, 18f));
        glass.add(glassText, new GridBagConstraints());
        setGlassPane(glass);

        // animasi titik di status
        dotsAnim = new javax.swing.Timer(300, e -> {
            String dots = status.getText().endsWith("…") ? "" :
                          status.getText().endsWith("..") ? "…" :
                          status.getText().endsWith(".") ? ".." : ".";
            status.setText(baseStatus + dots);
        });

        // actions
        btnAdd.addActionListener(e -> {
            glassText.setText("Inserting student…");
            addStudent();
        });
        btnUpd.addActionListener(e -> {
            glassText.setText("Updating student…");
            updateStudent();
        });
        btnDel.addActionListener(e -> {
            glassText.setText("Deleting student…");
            deleteStudent();
        });
        btnRef.addActionListener(e -> {
            glassText.setText("Loading students…");
            loadStudents();
        });

        // initial load
        loadStudents();
    }

    private void setBusy(boolean b){
        btnAdd.setEnabled(!b);
        btnUpd.setEnabled(!b);
        btnDel.setEnabled(!b);
        btnRef.setEnabled(!b);
        tfName.setEnabled(!b);
        tfAge.setEnabled(!b);

        loading.setVisible(b);
        setCursor(b ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        glass.setVisible(b);

        baseStatus = b ? "Working" : "Idle";
        status.setText(baseStatus);
        if (b) dotsAnim.start(); else dotsAnim.stop();
    }

    private void loadStudents(){
        setBusy(true);
        service.loadAsync(this::onStudentsLoaded, t -> err("Load failed: "+t.getMessage()));
    }

    private void onStudentsLoaded(List<Student> list){
        // isi tabel
        model.setRowCount(0);
        list.forEach(s -> model.addRow(new Object[]{ s.getId(), s.getName(), s.getAge() }));
        // sinkronkan bola
        ballPanel.setStudents(list);
        setBusy(false);
    }

    private void addStudent(){
        String n = tfName.getText().trim();
        String a = tfAge.getText().trim();
        if(n.isEmpty() || a.isEmpty()){ err("Fill name & age"); return; }
        int age;
        try { age = Integer.parseInt(a); } catch(Exception ex){ err("Age must be a number"); return; }

        setBusy(true);
        // setelah insert → reload list (agar dapat ID auto-increment) → ballPanel akan tambah bola
        service.addAsync(n, age, this::loadStudents, t -> err("Insert failed: "+t.getMessage()));
        tfName.setText(""); tfAge.setText("");
    }

    private void updateStudent(){
        int r = table.getSelectedRow();
        if(r < 0){ err("Select a row to update"); return; }
        int id = (int) model.getValueAt(r, 0);
        String n = (String) model.getValueAt(r, 1);
        int age;
        try { age = Integer.parseInt(model.getValueAt(r, 2).toString()); }
        catch(Exception ex){ err("Age must be a number"); return; }

        setBusy(true);
        service.updateAsync(id, n, age, this::loadStudents, t -> err("Update failed: "+t.getMessage()));
    }

    private void deleteStudent(){
        int r = table.getSelectedRow();
        if(r < 0){ err("Select a row to delete"); return; }
        int id = (int) model.getValueAt(r, 0);

        setBusy(true);
        // setelah delete → reload; ballPanel akan otomatis remove bola yang hilang
        service.deleteAsync(id, this::loadStudents, t -> err("Delete failed: "+t.getMessage()));
    }

    private void err(String m){ setBusy(false); JOptionPane.showMessageDialog(this, m); }

    @Override
    public void dispose() {
        super.dispose();
        ballPanel.stopAll();   // matikan semua thread bola saat window ditutup
        service.close();       // matikan executor DB
    }
}
