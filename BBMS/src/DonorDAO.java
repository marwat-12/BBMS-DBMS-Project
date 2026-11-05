import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonorDAO {

    // CREATE — inserts a matching USERS row and a DONORS row together
    // Self-registration: generates DonorID and UserID automatically, lets the
    // person set their own username/password (used by the Register screen)
    public String registerDonor(String username, String password, String email,
                                 String name, String cnic, String gender, int age, String phone,
                                 String address, String dob, String bloodgroup) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            String userId = nextId(con, "USERS", "UserID", "U");
            String donorId = nextId(con, "DONORS", "DonorID", "D");

            try (PreparedStatement ps1 = con.prepareStatement("INSERT INTO USERS VALUES (?,?,?,?,?)")) {
                ps1.setString(1, userId); ps1.setString(2, username); ps1.setString(3, email);
                ps1.setString(4, password); ps1.setString(5, "Donor");
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement(
                    "INSERT INTO DONORS VALUES (?,?,?,?,?,?,?,?,TO_DATE(?,'YYYY-MM-DD'),?)")) {
                ps2.setString(1, donorId); ps2.setString(2, userId); ps2.setString(3, name);
                ps2.setString(4, cnic); ps2.setString(5, gender); ps2.setInt(6, age);
                ps2.setString(7, phone); ps2.setString(8, address); ps2.setString(9, dob);
                ps2.setString(10, bloodgroup);
                ps2.executeUpdate();
            }
            con.commit();
            return donorId;
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

    // Finds this logged-in user's own DonorID (used to restrict a Donor to their own data)
    public String getDonorIdByUserId(String userId) throws SQLException {
        String sql = "SELECT DonorID FROM DONORS WHERE UserID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // Returns just one donor's own row (not the whole table)
    public List<String[]> getDonorById(String donorId) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT DonorID, Name, CNIC, Bloodgroup, Phone FROM DONORS WHERE DonorID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public void addDonor(String donorId, String userId, String username, String email,
                          String name, String cnic, String gender, int age, String phone,
                          String address, String dob, String bloodgroup) throws SQLException {
        String userSql  = "INSERT INTO USERS VALUES (?,?,?,?,?)";
        String donorSql = "INSERT INTO DONORS VALUES (?,?,?,?,?,?,?,?,TO_DATE(?,'YYYY-MM-DD'),?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(userSql)) {
                ps1.setString(1, userId);
                ps1.setString(2, username);
                ps1.setString(3, email);
                ps1.setString(4, "pass123");
                ps1.setString(5, "Donor");
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement(donorSql)) {
                ps2.setString(1, donorId);
                ps2.setString(2, userId);
                ps2.setString(3, name);
                ps2.setString(4, cnic);
                ps2.setString(5, gender);
                ps2.setInt(6, age);
                ps2.setString(7, phone);
                ps2.setString(8, address);
                ps2.setString(9, dob);
                ps2.setString(10, bloodgroup);
                ps2.executeUpdate();
            }
            con.commit();
        }
    }

    // READ / SEARCH — by blood group, or blank/null for all donors
    // General search box: matches DonorID, Name, CNIC, or Bloodgroup — partial match, case-insensitive
    public List<String[]> searchDonorsByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT DonorID, Name, CNIC, Bloodgroup, Phone FROM DONORS" +
                     (filter ? " WHERE UPPER(DonorID) LIKE ? OR UPPER(Name) LIKE ? OR CNIC LIKE ? OR UPPER(Bloodgroup) LIKE ?" : "") +
                     " ORDER BY DonorID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) {
                String like = "%" + term.toUpperCase() + "%";
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, "%" + term + "%"); ps.setString(4, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public List<String[]> searchDonors(String bloodgroup) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = bloodgroup != null && !bloodgroup.isBlank();
        String sql = filter
                ? "SELECT DonorID, Name, CNIC, Bloodgroup, Phone FROM DONORS WHERE Bloodgroup = ? ORDER BY DonorID"
                : "SELECT DonorID, Name, CNIC, Bloodgroup, Phone FROM DONORS ORDER BY DonorID";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) ps.setString(1, bloodgroup);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                        rs.getString("DonorID"), rs.getString("Name"), rs.getString("CNIC"),
                        rs.getString("Bloodgroup"), rs.getString("Phone")
                    });
                }
            }
        }
        return rows;
    }

    // UPDATE
    // Lets a Donor update more than just their phone number (self-service profile edit)
    public void updateDonorProfile(String donorId, String phone, String address, String bloodgroup) throws SQLException {
        String sql = "UPDATE DONORS SET Phone=?, Address=?, Bloodgroup=? WHERE DonorID=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone); ps.setString(2, address); ps.setString(3, bloodgroup); ps.setString(4, donorId);
            if (ps.executeUpdate() == 0) throw new SQLException("No donor found with ID " + donorId);
        }
    }

    public void updateDonorPhone(String donorId, String newPhone) throws SQLException {
        String sql = "UPDATE DONORS SET Phone = ? WHERE DonorID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newPhone);
            ps.setString(2, donorId);
            int n = ps.executeUpdate();
            if (n == 0) throw new SQLException("No donor found with ID " + donorId);
        }
    }

    // DELETE
    public void deleteDonor(String donorId) throws SQLException {
        String sql = "DELETE FROM DONORS WHERE DonorID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, donorId);
            int n = ps.executeUpdate();
            if (n == 0) throw new SQLException("No donor found with ID " + donorId);
        }
    }

    // calls the PL/SQL function get_eligibility (from 05_plsql.sql)
    public String getEligibility(String donorId) throws SQLException {
        String sql = "{? = call get_eligibility(?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(2, donorId);
            cs.execute();
            return cs.getString(1);
        }
    }
}