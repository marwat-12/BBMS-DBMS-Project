import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EssentialTestDAO {

    // Restricts results to one donor's own screening history
    public List<String[]> searchScreeningsByDonor(String donorId) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT ScreeningID, DonorID, Hemoglobin, Pulse, Weights, EligibilityStatus, Quantity " +
                     "FROM ESSENTIAL_TESTS WHERE DonorID = ? ORDER BY ScreeningID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
            }
        }
        return rows;
    }

    // A Donor requests to donate — creates a "Pending" screening row with no
    // measurements yet. Admin later fills in the real numbers via completeScreening().
    public String requestDonation(String donorId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String screeningId = nextId(con, "ESSENTIAL_TESTS");
            String sql = "INSERT INTO ESSENTIAL_TESTS VALUES (?,?,?,?,?,SYSDATE,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, screeningId);
                ps.setString(2, donorId);
                ps.setDouble(3, 0);
                ps.setInt(4, 0);
                ps.setDouble(5, 0);
                ps.setString(6, "Pending");
                ps.setString(7, "Pending");
                ps.setInt(8, 0);
                ps.executeUpdate();
            }
            return screeningId;
        }
    }

    // Admin fills in the actual vitals for a "Pending" screening; eligibility is (re)computed here.
    // Quantity is the units of blood actually drawn — entered ONCE, here, only when the donor
    // turns out Eligible. Every later stage (Testing, Record, Inventory) inherits this same number.
    public void completeScreening(String screeningId, double hemoglobin, int pulse,
                                   double weight, String bloodPressure, int quantity) throws SQLException {
        String eligibility = (hemoglobin >= 12.5 && weight >= 50) ? "Eligible" : "Ineligible";
        int qtyToStore = eligibility.equals("Eligible") ? quantity : 0;
        String sql = "UPDATE ESSENTIAL_TESTS SET Hemoglobin=?, Pulse=?, Weights=?, BloodPressure=?, " +
                     "EligibilityStatus=?, Quantity=?, ScreeningDate=SYSDATE WHERE ScreeningID=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, hemoglobin);
            ps.setInt(2, pulse);
            ps.setDouble(3, weight);
            ps.setString(4, bloodPressure);
            ps.setString(5, eligibility);
            ps.setInt(6, qtyToStore);
            ps.setString(7, screeningId);
            if (ps.executeUpdate() == 0) throw new SQLException("No screening found with ID " + screeningId);
        }
    }

    // Collision-proof ID generator: looks at the highest existing numeric suffix
    // and adds 1, instead of counting rows (which breaks the moment any row is
    // deleted, since the count can then fall behind IDs that still exist).
    private String nextId(Connection con, String table) throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(ScreeningID, 3))), 0) FROM " + table;
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return "SC" + String.format("%02d", rs.getInt(1) + 1);
        }
    }

    public void addScreening(String screeningId, String donorId, double hemoglobin, int pulse,
                              double weight, String bloodPressure, int quantity) throws SQLException {
        String eligibility = (hemoglobin >= 12.5 && weight >= 50) ? "Eligible" : "Ineligible";
        String sql = "INSERT INTO ESSENTIAL_TESTS VALUES (?,?,?,?,?,SYSDATE,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, screeningId); ps.setString(2, donorId); ps.setDouble(3, hemoglobin);
            ps.setInt(4, pulse); ps.setDouble(5, weight); ps.setString(6, bloodPressure);
            ps.setString(7, eligibility);
            ps.setInt(8, eligibility.equals("Eligible") ? quantity : 0);
            ps.executeUpdate();
        }
    }

    // General search box: matches ScreeningID or DonorID
    public List<String[]> searchScreeningsByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT ScreeningID, DonorID, Hemoglobin, Pulse, Weights, EligibilityStatus, Quantity FROM ESSENTIAL_TESTS" +
                     (filter ? " WHERE UPPER(ScreeningID) LIKE ? OR UPPER(DonorID) LIKE ?" : "") + " ORDER BY ScreeningID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) {
                String like = "%" + term.toUpperCase() + "%";
                ps.setString(1, like); ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
            }
        }
        return rows;
    }

    // Just the Donor-submitted requests still waiting on Admin (workflow queue #1)
    public List<String[]> searchPending() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT ScreeningID, DonorID, Hemoglobin, Pulse, Weights, EligibilityStatus, Quantity " +
                     "FROM ESSENTIAL_TESTS WHERE EligibilityStatus = 'Pending' ORDER BY ScreeningID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
        }
        return rows;
    }

    // Screenings Admin has already completed (Eligible or Ineligible)
    public List<String[]> searchCompleted() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT ScreeningID, DonorID, Hemoglobin, Pulse, Weights, EligibilityStatus, Quantity " +
                     "FROM ESSENTIAL_TESTS WHERE EligibilityStatus != 'Pending' ORDER BY ScreeningID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
        }
        return rows;
    }

    public List<String[]> searchScreenings(String eligibility) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = eligibility != null && !eligibility.isBlank();
        String sql = "SELECT ScreeningID, DonorID, Hemoglobin, Pulse, Weights, EligibilityStatus, Quantity FROM ESSENTIAL_TESTS" +
                     (filter ? " WHERE EligibilityStatus = ?" : "") + " ORDER BY ScreeningID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) ps.setString(1, eligibility);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
            }
        }
        return rows;
    }

    // Looks up the quantity recorded at screening time for a given ScreeningID —
    // used by Blood Testing when it creates a Blood Record, so the number never
    // has to be typed again.
    public int getQuantityForScreening(String screeningId) throws SQLException {
        String sql = "SELECT Quantity FROM ESSENTIAL_TESTS WHERE ScreeningID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, screeningId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void deleteScreening(String screeningId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ESSENTIAL_TESTS WHERE ScreeningID = ?")) {
            ps.setString(1, screeningId);
            if (ps.executeUpdate() == 0) throw new SQLException("No screening found with ID " + screeningId);
        }
    }
}