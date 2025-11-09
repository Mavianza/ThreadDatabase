package app;

import app.ui.StudentApp;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentApp().setVisible(true));
    }
}
