import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    // Self-registration: generates PatientID and UserID automatically
    public String registerPatient(String username, String password, String email,
                                   String name, String cnic, String gender, String bloodgroup,
                                   String phone, String address) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            String userId = nextId(con, "USERS", "UserID", "U");
            String patientId = nextId(con, "PATIENTS", "PatientID", "P");

            try (PreparedStatement ps1 = con.prepareStatement("INSERT INTO USERS VALUES (?,?,?,?,?)")) {
                ps1.setString(1, userId); ps1.setString(2, username); ps1.setString(3, email);
                ps1.setString(4, password); ps1.setString(5, "Patient");
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement("INSERT INTO PATIENTS VALUES (?,?,?,?,?,?,?,?)")) {
                ps2.setString(1, patientId); ps2.setString(2, userId); ps2.setString(3, name);
                ps2.setString(4, cnic); ps2.setString(5, gender); ps2.setString(6, bloodgroup);
                ps2.setString(7, phone); ps2.setString(8, address);
                ps2.executeUpdate();
            }
            con.commit();
            return patientId;
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

    // Finds this logged-in user's own PatientID (used to restrict a Patient to their own data)
    public String getPatientIdByUserId(String userId) throws SQLException {
        String sql = "SELECT PatientID FROM PATIENTS WHERE UserID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // Returns just one patient's own row (not the whole table)
    public List<String[]> getPatientById(String patientId) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT PatientID, Name, CNIC, Bloodgroup, Phone FROM PATIENTS WHERE PatientID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public void addPatient(String patientId, String userId, String username, String email,
                            String name, String cnic, String gender, String bloodgroup,
                            String phone, String address) throws SQLException {
        String userSql = "INSERT INTO USERS VALUES (?,?,?,?,?)";
        String patSql  = "INSERT INTO PATIENTS VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(userSql)) {
                ps1.setString(1, userId); ps1.setString(2, username); ps1.setString(3, email);
                ps1.setString(4, "pass123"); ps1.setString(5, "Patient");
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement(patSql)) {
                ps2.setString(1, patientId); ps2.setString(2, userId); ps2.setString(3, name);
                ps2.setString(4, cnic); ps2.setString(5, gender); ps2.setString(6, bloodgroup);
                ps2.setString(7, phone); ps2.setString(8, address);
                ps2.executeUpdate();
            }
            con.commit();
        }
    }

    // General search box: matches PatientID, Name, CNIC, or Bloodgroup — partial match, case-insensitive
    public List<String[]> searchPatientsByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT PatientID, Name, CNIC, Bloodgroup, Phone FROM PATIENTS" +
                     (filter ? " WHERE UPPER(PatientID) LIKE ? OR UPPER(Name) LIKE ? OR CNIC LIKE ? OR UPPER(Bloodgroup) LIKE ?" : "") +
                     " ORDER BY PatientID";
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

    public List<String[]> searchPatients(String bloodgroup) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = bloodgroup != null && !bloodgroup.isBlank();
        String sql = "SELECT PatientID, Name, CNIC, Bloodgroup, Phone FROM PATIENTS" +
                     (filter ? " WHERE Bloodgroup = ?" : "") + " ORDER BY PatientID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) ps.setString(1, bloodgroup);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    // Lets a Patient update more than just their phone number (self-service profile edit)
    public void updatePatientProfile(String patientId, String phone, String address, String bloodgroup) throws SQLException {
        String sql = "UPDATE PATIENTS SET Phone=?, Address=?, Bloodgroup=? WHERE PatientID=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone); ps.setString(2, address); ps.setString(3, bloodgroup); ps.setString(4, patientId);
            if (ps.executeUpdate() == 0) throw new SQLException("No patient found with ID " + patientId);
        }
    }

    public void updatePatientPhone(String patientId, String newPhone) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE PATIENTS SET Phone = ? WHERE PatientID = ?")) {
            ps.setString(1, newPhone); ps.setString(2, patientId);
            if (ps.executeUpdate() == 0) throw new SQLException("No patient found with ID " + patientId);
        }
    }

    public void deletePatient(String patientId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM PATIENTS WHERE PatientID = ?")) {
            ps.setString(1, patientId);
            if (ps.executeUpdate() == 0) throw new SQLException("No patient found with ID " + patientId);
        }
    }
}