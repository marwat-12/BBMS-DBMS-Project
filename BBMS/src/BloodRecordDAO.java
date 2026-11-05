import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BloodRecordDAO {

    // Restricts results to one donor's own blood records (joins via testing -> screening)
    public List<String[]> searchRecordsByDonor(String donorId) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT br.DonationID, br.TestID, br.DonationDate, br.Bloodgroup, br.Quantity " +
                     "FROM BLOOD_RECORDS br " +
                     "JOIN BLOOD_TESTING bt ON br.TestID = bt.TestID " +
                     "JOIN ESSENTIAL_TESTS et ON bt.ScreeningID = et.ScreeningID " +
                     "WHERE et.DonorID = ? ORDER BY br.DonationID";
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

    // Called automatically right after a test Passes — creates a placeholder
    // record so it shows up in Blood Record immediately (nothing to type)
    public String createPendingRecord(String testId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String donationId = nextId(con);
            // Look up the donor's registered blood group AND the quantity entered once at
            // screening time — both flow through automatically, nothing typed twice.
            String group = null;
            int quantity = 0;
            String lookupSql = "SELECT d.Bloodgroup, et.Quantity FROM DONORS d " +
                                "JOIN ESSENTIAL_TESTS et ON d.DonorID = et.DonorID " +
                                "JOIN BLOOD_TESTING bt ON et.ScreeningID = bt.ScreeningID WHERE bt.TestID = ?";
            try (PreparedStatement lps = con.prepareStatement(lookupSql)) {
                lps.setString(1, testId);
                try (ResultSet rs = lps.executeQuery()) {
                    if (rs.next()) { group = rs.getString(1); quantity = rs.getInt(2); }
                }
            }
            String sql = "INSERT INTO BLOOD_RECORDS VALUES (?,?,SYSDATE,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, donationId); ps.setString(2, testId);
                ps.setString(3, group); ps.setInt(4, quantity);
                ps.executeUpdate();
            }
            return donationId;
        }
    }

    // Admin fills in the real donation details for a Pending record — this is also
    // the ONLY place Quantity is entered; the linked Blood Inventory row is kept
    // in sync automatically so the number never has to be typed twice.
    public void completeRecord(String donationId, String bloodgroup, int quantity) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE BLOOD_RECORDS SET Bloodgroup=?, Quantity=?, DonationDate=SYSDATE WHERE DonationID=?")) {
                ps.setString(1, bloodgroup); ps.setInt(2, quantity); ps.setString(3, donationId);
                if (ps.executeUpdate() == 0) throw new SQLException("No record found with ID " + donationId);
            }
            // Keep Blood Inventory's quantity identical to the record's — it's the same units
            try (PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE BLOOD_INVENTORY SET Quantity=? WHERE DonationID=?")) {
                ps2.setInt(1, quantity); ps2.setString(2, donationId);
                ps2.executeUpdate(); // 0 rows is fine — inventory row may not exist yet for old data
            }
            con.commit();
        }
    }

    private String nextId(Connection con) throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(DonationID, 3))), 0) FROM BLOOD_RECORDS";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return "DN" + String.format("%02d", rs.getInt(1) + 1);
        }
    }

    // Records still waiting on Admin to confirm real quantity (Quantity = 0 is the placeholder)
    public List<String[]> searchPending() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT DonationID, TestID, DonationDate, Bloodgroup, Quantity FROM BLOOD_RECORDS WHERE Quantity = 0 ORDER BY DonationID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        }
        return rows;
    }

    // Records Admin has already confirmed
    public List<String[]> searchCompleted() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT DonationID, TestID, DonationDate, Bloodgroup, Quantity FROM BLOOD_RECORDS WHERE Quantity > 0 ORDER BY DonationID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        }
        return rows;
    }

    public void addRecord(String donationId, String testId, String bloodgroup, int quantity) throws SQLException {
        String sql = "INSERT INTO BLOOD_RECORDS VALUES (?,?,SYSDATE,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, donationId); ps.setString(2, testId); ps.setString(3, bloodgroup); ps.setInt(4, quantity);
            ps.executeUpdate();
        }
    }

    public List<String[]> searchRecords() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT DonationID, TestID, DonationDate, Bloodgroup, Quantity FROM BLOOD_RECORDS ORDER BY DonationID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        }
        return rows;
    }

    public void deleteRecord(String donationId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM BLOOD_RECORDS WHERE DonationID = ?")) {
            ps.setString(1, donationId);
            if (ps.executeUpdate() == 0) throw new SQLException("No record found with ID " + donationId);
        }
    }
}