import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BloodTestingDAO {

    // Restricts results to one donor's own test panels (joins via their screenings)
    public List<String[]> searchTestsByDonor(String donorId) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT bt.TestID, bt.ScreeningID, bt.HIVTest, bt.HepatitisB, bt.HepatitisC, bt.Syphilis, bt.Result " +
                     "FROM BLOOD_TESTING bt JOIN ESSENTIAL_TESTS et ON bt.ScreeningID = et.ScreeningID " +
                     "WHERE et.DonorID = ? ORDER BY bt.TestID";
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

    public void addTest(String testId, String screeningId, String hiv, String hepB,
                         String hepC, String syphilis) throws SQLException {
        boolean allNegative = hiv.equals("Negative") && hepB.equals("Negative")
                            && hepC.equals("Negative") && syphilis.equals("Negative");
        String result = allNegative ? "Pass" : "Fail";
        String sql = "INSERT INTO BLOOD_TESTING VALUES (?,?,SYSDATE,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, testId); ps.setString(2, screeningId); ps.setString(3, hiv);
            ps.setString(4, hepB); ps.setString(5, hepC); ps.setString(6, syphilis); ps.setString(7, result);
            ps.executeUpdate();
        }
    }

    // Called automatically right after a screening becomes Eligible — creates a
    // placeholder test row so it shows up in Blood Testing immediately (nothing to type)
    public String createPendingTest(String screeningId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String testId = nextId(con);
            String sql = "INSERT INTO BLOOD_TESTING VALUES (?,?,SYSDATE,?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, testId); ps.setString(2, screeningId);
                ps.setString(3, "Pending"); ps.setString(4, "Pending"); ps.setString(5, "Pending"); ps.setString(6, "Pending");
                ps.setString(7, "Pending");
                ps.executeUpdate();
            }
            return testId;
        }
    }

    // Admin fills in the real lab results for a Pending test row; Result is computed here
    public void completeTest(String testId, String hiv, String hepB, String hepC, String syphilis) throws SQLException {
        boolean allNegative = hiv.equals("Negative") && hepB.equals("Negative")
                            && hepC.equals("Negative") && syphilis.equals("Negative");
        String result = allNegative ? "Pass" : "Fail";
        String sql = "UPDATE BLOOD_TESTING SET HIVTest=?, HepatitisB=?, HepatitisC=?, Syphilis=?, Result=?, TestDate=SYSDATE WHERE TestID=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hiv); ps.setString(2, hepB); ps.setString(3, hepC); ps.setString(4, syphilis);
            ps.setString(5, result); ps.setString(6, testId);
            if (ps.executeUpdate() == 0) throw new SQLException("No test found with ID " + testId);
        }
    }

    private String nextId(Connection con) throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(TestID, 2))), 0) FROM BLOOD_TESTING";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return "T" + String.format("%02d", rs.getInt(1) + 1);
        }
    }

    // General search box: matches TestID or ScreeningID
    public List<String[]> searchTestsByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT TestID, ScreeningID, HIVTest, HepatitisB, HepatitisC, Syphilis, Result FROM BLOOD_TESTING" +
                     (filter ? " WHERE UPPER(TestID) LIKE ? OR UPPER(ScreeningID) LIKE ?" : "") + " ORDER BY TestID";
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

    // Test panels still waiting on Admin to fill in the real lab results
    public List<String[]> searchPending() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT TestID, ScreeningID, HIVTest, HepatitisB, HepatitisC, Syphilis, Result " +
                     "FROM BLOOD_TESTING WHERE Result = 'Pending' ORDER BY TestID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
        }
        return rows;
    }

    // Test panels Admin has already completed (Pass or Fail)
    public List<String[]> searchCompleted() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT TestID, ScreeningID, HIVTest, HepatitisB, HepatitisC, Syphilis, Result " +
                     "FROM BLOOD_TESTING WHERE Result != 'Pending' ORDER BY TestID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
        }
        return rows;
    }

    // Eligible screenings that don't have a test panel yet — the "waiting to be tested" queue
    public List<String[]> searchAwaitingTest() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT et.ScreeningID, et.DonorID, et.EligibilityStatus FROM ESSENTIAL_TESTS et " +
                     "WHERE et.EligibilityStatus = 'Eligible' " +
                     "AND et.ScreeningID NOT IN (SELECT ScreeningID FROM BLOOD_TESTING) ORDER BY et.ScreeningID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
        }
        return rows;
    }

    public List<String[]> searchTests(String result) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = result != null && !result.isBlank();
        String sql = "SELECT TestID, ScreeningID, HIVTest, HepatitisB, HepatitisC, Syphilis, Result FROM BLOOD_TESTING" +
                     (filter ? " WHERE Result = ?" : "") + " ORDER BY TestID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) ps.setString(1, result);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)});
            }
        }
        return rows;
    }

    public void deleteTest(String testId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM BLOOD_TESTING WHERE TestID = ?")) {
            ps.setString(1, testId);
            if (ps.executeUpdate() == 0) throw new SQLException("No test found with ID " + testId);
        }
    }
}