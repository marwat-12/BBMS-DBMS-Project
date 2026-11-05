import java.sql.*;

public class UserDAO {
    // Returns {UserID, Role} if the username/password match a row in USERS, otherwise null
    public String[] login(String username, String password) throws SQLException {
        String sql = "SELECT UserID, Role FROM USERS WHERE Username = ? AND Password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString("UserID"), rs.getString("Role")};
                }
                return null;
            }
        }
    }
}
