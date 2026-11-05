import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BloodRequestDAO {

    // Restricts results to one patient's own requests
    public List<String[]> searchRequestsByPatient(String patientId) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT br.RequestID, p.Name, br.RequiredGroup, br.Quantity, br.Status " +
                     "FROM BLOOD_REQUESTS br JOIN PATIENTS p ON br.PatientID = p.PatientID " +
                     "WHERE br.PatientID = ? ORDER BY br.RequestID";
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

    // Allocates real stock from Blood Inventory for an Approved request (the "Fulfils"
    // relationship). Deducts quantity from the oldest-expiring matching units first,
    // marks any unit that hits 0 as Reserved, and marks the request Fulfilled.
    public void fulfillRequest(String requestId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            String group; int needed;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT RequiredGroup, Quantity FROM BLOOD_REQUESTS WHERE RequestID = ?")) {
                ps.setString(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("No request found with ID " + requestId);
                    group = rs.getString(1);
                    needed = rs.getInt(2);
                }
            }

            // Oldest-expiring matching, available units first (FEFO)
            String findSql = "SELECT bi.InventoryID, bi.Quantity FROM BLOOD_INVENTORY bi " +
                              "JOIN BLOOD_RECORDS brc ON bi.DonationID = brc.DonationID " +
                              "WHERE bi.Status = 'Available' AND brc.Bloodgroup = ? ORDER BY bi.ExpiryDate ASC";
            int remaining = needed;
            try (PreparedStatement ps = con.prepareStatement(findSql)) {
                ps.setString(1, group);
                try (ResultSet rs = ps.executeQuery()) {
                    while (remaining > 0 && rs.next()) {
                        String invId = rs.getString(1);
                        int haveQty = rs.getInt(2);
                        int take = Math.min(haveQty, remaining);
                        int leftover = haveQty - take;
                        String updateSql = leftover == 0
                                ? "UPDATE BLOOD_INVENTORY SET Quantity = 0, Status = 'Reserved' WHERE InventoryID = ?"
                                : "UPDATE BLOOD_INVENTORY SET Quantity = ? WHERE InventoryID = ?";
                        try (PreparedStatement ups = con.prepareStatement(updateSql)) {
                            if (leftover == 0) {
                                ups.setString(1, invId);
                            } else {
                                ups.setInt(1, leftover);
                                ups.setString(2, invId);
                            }
                            ups.executeUpdate();
                        }
                        remaining -= take;
                    }
                }
            }

            if (remaining > 0) {
                con.rollback();
                throw new SQLException("Not enough " + group + " stock available (short by " + remaining + " unit(s)).");
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE BLOOD_REQUESTS SET Status = 'Fulfilled' WHERE RequestID = ?")) {
                ps.setString(1, requestId);
                ps.executeUpdate();
            }

            con.commit();
        }
    }

    // Auto-generates RequestID — Patient never has to invent one
    public String createRequest(String patientId, String requiredGroup, int qty) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String requestId = nextId(con);
            String sql = "INSERT INTO BLOOD_REQUESTS (RequestID, PatientID, Status, RequestDate, RequiredGroup, Quantity) " +
                         "VALUES (?, ?, 'Pending', SYSDATE, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, requestId);
                ps.setString(2, patientId);
                ps.setString(3, requiredGroup);
                ps.setInt(4, qty);
                ps.executeUpdate();
            }
            return requestId;
        }
    }

    // Collision-proof ID generator: looks at the highest existing numeric suffix
    // and adds 1, instead of counting rows (which breaks the moment any row is
    // deleted, since the count can then fall behind IDs that still exist).
    private String nextId(Connection con) throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(RequestID, 2))), 0) FROM BLOOD_REQUESTS";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int max = rs.getInt(1);
            return "R" + String.format("%02d", max + 1);
        }
    }

    // CREATE
    public void addRequest(String requestId, String patientId, String requiredGroup, int qty) throws SQLException {
        String sql = "INSERT INTO BLOOD_REQUESTS (RequestID, PatientID, Status, RequestDate, RequiredGroup, Quantity) " +
                     "VALUES (?, ?, 'Pending', SYSDATE, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, requestId);
            ps.setString(2, patientId);
            ps.setString(3, requiredGroup);
            ps.setInt(4, qty);
            ps.executeUpdate();
        }
    }

    // READ / SEARCH — by status, or blank/null for all requests
    // General search box: matches RequestID, PatientID, or the patient's Name
    public List<String[]> searchRequestsByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT br.RequestID, p.Name, br.RequiredGroup, br.Quantity, br.Status " +
                     "FROM BLOOD_REQUESTS br JOIN PATIENTS p ON br.PatientID = p.PatientID" +
                     (filter ? " WHERE UPPER(br.RequestID) LIKE ? OR UPPER(br.PatientID) LIKE ? OR UPPER(p.Name) LIKE ?" : "") +
                     " ORDER BY br.RequestID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) {
                String like = "%" + term.toUpperCase() + "%";
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public List<String[]> searchRequests(String status) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = status != null && !status.isBlank();
        String sql = "SELECT br.RequestID, p.Name, br.RequiredGroup, br.Quantity, br.Status " +
                     "FROM BLOOD_REQUESTS br JOIN PATIENTS p ON br.PatientID = p.PatientID " +
                     (filter ? "WHERE br.Status = ? " : "") + "ORDER BY br.RequestID";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5)
                    });
                }
            }
        }
        return rows;
    }

    // calls the PL/SQL procedure approve_request (from 05_plsql.sql)
    public void approveRequest(String requestId, String adminId) throws SQLException {
        String sql = "{call approve_request(?, ?)}";
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, requestId);
            cs.setString(2, adminId);
            cs.execute();
        }
    }

    // DELETE
    public void deleteRequest(String requestId) throws SQLException {
        String sql = "DELETE FROM BLOOD_REQUESTS WHERE RequestID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, requestId);
            int n = ps.executeUpdate();
            if (n == 0) throw new SQLException("No request found with ID " + requestId);
        }
    }
}