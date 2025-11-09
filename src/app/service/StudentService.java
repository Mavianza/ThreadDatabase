package app.service;

import app.dao.StudentDAO;
import app.model.Student;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StudentService implements AutoCloseable {
    private final StudentDAO dao = new StudentDAO();
    // Single-thread: operasi DB diserialkan, UI tetap responsif
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public void loadAsync(Consumer<List<Student>> onDone, Consumer<Throwable> onErr) {
        io.submit(() -> {
            try {
                var data = dao.getAll();
                SwingUtilities.invokeLater(() -> onDone.accept(data));
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> onErr.accept(t));
            }
        });
    }

    public void addAsync(String name, int age, Runnable onOk, Consumer<Throwable> onErr) {
        io.submit(() -> {
            try { dao.insert(new Student(0, name, age)); SwingUtilities.invokeLater(onOk); }
            catch (Throwable t) { SwingUtilities.invokeLater(() -> onErr.accept(t)); }
        });
    }

    public void updateAsync(int id, String name, int age, Runnable onOk, Consumer<Throwable> onErr) {
        io.submit(() -> {
            try { dao.update(new Student(id, name, age)); SwingUtilities.invokeLater(onOk); }
            catch (Throwable t) { SwingUtilities.invokeLater(() -> onErr.accept(t)); }
        });
    }

    public void deleteAsync(int id, Runnable onOk, Consumer<Throwable> onErr) {
        io.submit(() -> {
            try { dao.delete(id); SwingUtilities.invokeLater(onOk); }
            catch (Throwable t) { SwingUtilities.invokeLater(() -> onErr.accept(t)); }
        });
    }

    @Override public void close() { io.shutdownNow(); }
}
