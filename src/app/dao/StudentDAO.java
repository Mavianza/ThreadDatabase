package app.dao;

import app.db.DBUtil;
import app.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public List<Student> getAll() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT id, name, age FROM students ORDER BY id";
        try (Connection c = DBUtil.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("SELECT gagal: " + e.getMessage(), e);
        }
        return list;
    }

    public int insert(Student s) throws SQLException {
        String sql = "INSERT INTO students(name, age) VALUES(?, ?)";
        try (Connection c = DBUtil.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setInt(2, s.getAge());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public int update(Student s) throws SQLException {
        String sql = "UPDATE students SET name=?, age=? WHERE id=?";
        try (Connection c = DBUtil.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setInt(2, s.getAge());
            ps.setInt(3, s.getId());
            return ps.executeUpdate();
        }
    }

    public int delete(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id=?";
        try (Connection c = DBUtil.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }
}
