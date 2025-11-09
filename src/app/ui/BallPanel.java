package app.ui;

import app.model.Student;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class BallPanel extends JPanel {
    private static final int RADIUS = 14;
    private final Map<Integer, Ball> balls = new ConcurrentHashMap<>(); // idStudent -> Ball
    private final Random rng = new Random(123);

    public BallPanel() {
        setPreferredSize(new Dimension(320, 0));
        setBackground(new Color(18, 18, 18));
        setBorder(BorderFactory.createTitledBorder("Students Concurrency Demo"));
    }

    /** Sinkronkan daftar bola dengan daftar students terbaru */
    public void setStudents(List<Student> list) {
        // hapus bola yang tidak ada lagi student-nya
        balls.keySet().removeIf(id -> list.stream().noneMatch(s -> s.getId() == id));

        // tambahkan bola untuk student baru
        for (Student s : list) {
            balls.computeIfAbsent(s.getId(), id -> {
                // warna stabil berdasarkan id
                Color color = new Color(100 + (id * 37) % 155, 100 + (id * 73) % 155, 100 + (id * 19) % 155);
                Ball b = new Ball(color);
                b.start(this);
                return b;
            });
        }
        repaint();
    }

    /** Tambah bola saat student baru berhasil tersimpan */
    public void addStudentBall(Student s) {
        balls.computeIfAbsent(s.getId(), id -> {
            Color color = new Color(100 + (id * 37) % 155, 100 + (id * 73) % 155, 100 + (id * 19) % 155);
            Ball b = new Ball(color);
            b.start(this);
            return b;
        });
        repaint();
    }

    /** Hapus bola saat student dihapus */
    public void removeStudentBall(int studentId) {
        Ball b = balls.remove(studentId);
        if (b != null) b.stop();
        repaint();
    }

    /** Hentikan semua thread bola (dipanggil saat window ditutup) */
    public void stopAll() {
        balls.values().forEach(Ball::stop);
        balls.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // gambar semua bola (snapshot aman; fields diakses via volatile)
        Graphics2D g2 = (Graphics2D) g.create();
        for (Ball b : balls.values()) {
            g2.setColor(b.color);
            int r = RADIUS;
            int x = (int) Math.round(b.x) - r;
            int y = (int) Math.round(b.y) - r;
            g2.fillOval(x, y, r * 2, r * 2);
            // outline
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawOval(x, y, r * 2, r * 2);
        }
        g2.dispose();
    }

    /** Kelas bola + game loop per-bola (1 thread per bola) */
    private static class Ball implements Runnable {
        private final Color color;
        private volatile boolean running = true;
        private Thread th;
        // posisi/kecepatan (volatile agar pembacaan paint aman tanpa lock)
        volatile double x, y, vx, vy;

        Ball(Color color) {
            this.color = color;
            // inisialisasi random ringan; posisi awal akan diset saat panel sudah punya ukuran
            this.x = 40 + Math.random() * 160;
            this.y = 40 + Math.random() * 120;
            this.vx = 1 + Math.random() * 3;   // px per tick
            this.vy = 1 + Math.random() * 3;
        }

        void start(BallPanel panel) {
            th = new Thread(() -> runLoop(panel), "Ball-Thread");
            th.setDaemon(true);
            th.start();
        }

        void stop() {
            running = false;
            if (th != null) th.interrupt();
        }

        @Override
        public void run() { /* unused */ }

        private void runLoop(BallPanel panel) {
            // simple game loop ~60 FPS
            final int radius = RADIUS;
            while (running) {
                try {
                    int w = Math.max(panel.getWidth(), 200);
                    int h = Math.max(panel.getHeight(), 200);

                    // init center jika keluar panel / ukuran 0
                    if (x <= 0 || y <= 0 || x >= w || y >= h) {
                        x = Math.max(radius, Math.min(w - radius, x <= 0 ? w / 3.0 : x));
                        y = Math.max(radius, Math.min(h - radius, y <= 0 ? h / 3.0 : y));
                    }

                    // update posisi
                    x += vx;
                    y += vy;

                    // bounce dinding
                    if (x < radius)      { x = radius;      vx = Math.abs(vx); }
                    if (x > w - radius)  { x = w - radius;  vx = -Math.abs(vx); }
                    if (y < radius)      { y = radius;      vy = Math.abs(vy); }
                    if (y > h - radius)  { y = h - radius;  vy = -Math.abs(vy); }

                    // minta repaint di EDT
                    SwingUtilities.invokeLater(panel::repaint);

                    // sleep ~16ms
                    Thread.sleep(16);
                } catch (InterruptedException ie) {
                    // allow stop
                }
            }
        }
    }
}
