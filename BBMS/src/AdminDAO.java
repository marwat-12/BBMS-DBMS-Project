import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    // Finds the logged-in Admin's own AdminID (used to attribute new inventory rows)
    public String getAdminIdByUserId(String userId) throws SQLException {
        String sql = "SELECT AdminID FROM ADMINS WHERE UserID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // Auto-generates AdminID and UserID — Admin never has to type or guess an ID
    public String registerAdmin(String username, String password, String email,
                                 String name, String phone) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            String userId = nextId(con, "USERS", "UserID", "U");
            String adminId = nextId(con, "ADMINS", "AdminID", "A");
            try (PreparedStatement ps1 = con.prepareStatement("INSERT INTO USERS VALUES (?,?,?,?,?)")) {
                ps1.setString(1, userId); ps1.setString(2, username); ps1.setString(3, email);
                ps1.setString(4, password); ps1.setString(5, "Admin");
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement("INSERT INTO ADMINS VALUES (?,?,?,?)")) {
                ps2.setString(1, adminId); ps2.setString(2, userId); ps2.setString(3, name); ps2.setString(4, phone);
                ps2.executeUpdate();
            }
            con.commit();
            return adminId;
        }
    }

    // Collision-proof ID generator: looks at the highest existing numeric suffix
    // and adds 1, instead of counting rows (which breaks the moment any row is
    // deleted, since the count can then fall behind IDs that still exist).
    private String nextId(Connection con, String table, String idColumn, String prefix) throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(" + idColumn + ", " + (prefix.length() + 1) + "))), 0) FROM " + table;
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int max = rs.getInt(1);
            return prefix + String.format("%02d", max + 1);
        }
    }

    // Search by AdminID, Name, or Phone (partial match) — used by the general search box
    public List<String[]> searchAdminsByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT AdminID, Name, Phone FROM ADMINS" +
                     (filter ? " WHERE UPPER(AdminID) LIKE ? OR UPPER(Name) LIKE ? OR Phone LIKE ?" : "") +
                     " ORDER BY AdminID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) {
                String like = "%" + term.toUpperCase() + "%";
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, "%" + term + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
            }
        }
        return rows;
    }

    public void addAdmin(String adminId, String userId, String username, String email,
                          String name, String phone) throws SQLException {
        String userSql = "INSERT INTO USERS VALUES (?,?,?,?,?)";
        String adminSql = "INSERT INTO ADMINS VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(userSql)) {
                ps1.setString(1, userId); ps1.setString(2, username); ps1.setString(3, email);
                ps1.setString(4, "admin123"); ps1.setString(5, "Admin");
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement(adminSql)) {
                ps2.setString(1, adminId); ps2.setString(2, userId); ps2.setString(3, name); ps2.setString(4, phone);
                ps2.executeUpdate();
            }
            con.commit();
        }
    }

    public List<String[]> searchAdmins() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT AdminID, Name, Phone FROM ADMINS ORDER BY AdminID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
        }
        return rows;
    }

    public void updateAdminPhone(String adminId, String newPhone) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE ADMINS SET Phone = ? WHERE AdminID = ?")) {
            ps.setString(1, newPhone); ps.setString(2, adminId);
            if (ps.executeUpdate() == 0) throw new SQLException("No admin found with ID " + adminId);
        }
    }

    public void deleteAdmin(String adminId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ADMINS WHERE AdminID = ?")) {
            ps.setString(1, adminId);
            if (ps.executeUpdate() == 0) throw new SQLException("No admin found with ID " + adminId);
        }
    }
}