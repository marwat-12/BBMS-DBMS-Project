import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BloodInventoryDAO {

    // Checks whether a donation already has an inventory row (new records get one
    // automatically the instant their test passes; this is a safety check for older data)
    public boolean hasInventoryForDonation(String donationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BLOOD_INVENTORY WHERE DonationID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    // Called automatically right after a Blood Record is completed — creates a
    // placeholder inventory row so it shows up in Inventory immediately
    public String createPendingInventory(String donationId, String adminId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String inventoryId = nextId(con);
            int quantity = 0;
            try (PreparedStatement lps = con.prepareStatement(
                    "SELECT Quantity FROM BLOOD_RECORDS WHERE DonationID = ?")) {
                lps.setString(1, donationId);
                try (ResultSet rs = lps.executeQuery()) {
                    if (rs.next()) quantity = rs.getInt(1);
                }
            }
            String sql = "INSERT INTO BLOOD_INVENTORY VALUES (?,?,?,?,NULL,NULL,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, inventoryId); ps.setString(2, donationId); ps.setString(3, adminId);
                ps.setString(4, "Pending"); ps.setInt(5, quantity);
                ps.executeUpdate();
            }
            return inventoryId;
        }
    }

    // Admin fills in collection/expiry dates for a Pending inventory row and marks it Available.
    // Quantity is NOT set here — it's kept in sync with Blood Record automatically.
    public void completeInventory(String inventoryId, String collectionDate, String expiryDate) throws SQLException {
        String sql = "UPDATE BLOOD_INVENTORY SET Status='Available', CollectionDate=TO_DATE(?,'YYYY-MM-DD'), " +
                     "ExpiryDate=TO_DATE(?,'YYYY-MM-DD') WHERE InventoryID=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, collectionDate); ps.setString(2, expiryDate); ps.setString(3, inventoryId);
            if (ps.executeUpdate() == 0) throw new SQLException("No inventory row found with ID " + inventoryId);
        }
    }

    // Collision-proof ID generator: looks at the highest existing numeric suffix
    // and adds 1, instead of counting rows (which breaks the moment any row is
    // deleted, since the count can then fall behind IDs that still exist).
    private String nextId(Connection con) throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(SUBSTR(InventoryID, 4))), 0) FROM BLOOD_INVENTORY";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int max = rs.getInt(1);
            return "INV" + String.format("%02d", max + 1);
        }
    }

    // Stock still waiting on Admin to confirm collection/expiry dates and quantity
    public List<String[]> searchPending() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT bi.InventoryID, brc.Bloodgroup, bi.Quantity, bi.ExpiryDate, bi.Status " +
                     "FROM BLOOD_INVENTORY bi JOIN BLOOD_RECORDS brc ON bi.DonationID = brc.DonationID " +
                     "WHERE bi.Status = 'Pending' ORDER BY bi.InventoryID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        }
        return rows;
    }

    // Stock Admin has already confirmed
    public List<String[]> searchCompleted() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT bi.InventoryID, brc.Bloodgroup, bi.Quantity, bi.ExpiryDate, bi.Status " +
                     "FROM BLOOD_INVENTORY bi JOIN BLOOD_RECORDS brc ON bi.DonationID = brc.DonationID " +
                     "WHERE bi.Status != 'Pending' ORDER BY bi.InventoryID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
        }
        return rows;
    }

    public void addInventory(String inventoryId, String donationId, String adminId, String status,
                              String collectionDate, String expiryDate, int quantity) throws SQLException {
        String sql = "INSERT INTO BLOOD_INVENTORY VALUES (?,?,?,?,TO_DATE(?,'YYYY-MM-DD'),TO_DATE(?,'YYYY-MM-DD'),?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, inventoryId); ps.setString(2, donationId); ps.setString(3, adminId);
            ps.setString(4, status); ps.setString(5, collectionDate); ps.setString(6, expiryDate); ps.setInt(7, quantity);
            ps.executeUpdate();
        }
    }

    // General search box: matches InventoryID, DonationID, or Bloodgroup
    public List<String[]> searchInventoryByTerm(String term) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = term != null && !term.isBlank();
        String sql = "SELECT bi.InventoryID, brc.Bloodgroup, bi.Quantity, bi.ExpiryDate, bi.Status " +
                     "FROM BLOOD_INVENTORY bi JOIN BLOOD_RECORDS brc ON bi.DonationID = brc.DonationID" +
                     (filter ? " WHERE UPPER(bi.InventoryID) LIKE ? OR UPPER(bi.DonationID) LIKE ? OR UPPER(brc.Bloodgroup) LIKE ?" : "") +
                     " ORDER BY bi.InventoryID";
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

    public List<String[]> searchInventory(String status) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        boolean filter = status != null && !status.isBlank();
        String sql = "SELECT bi.InventoryID, brc.Bloodgroup, bi.Quantity, bi.ExpiryDate, bi.Status " +
                     "FROM BLOOD_INVENTORY bi JOIN BLOOD_RECORDS brc ON bi.DonationID = brc.DonationID" +
                     (filter ? " WHERE bi.Status = ?" : "") + " ORDER BY bi.InventoryID";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (filter) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)});
            }
        }
        return rows;
    }

    public void updateStatus(String inventoryId, String newStatus) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE BLOOD_INVENTORY SET Status = ? WHERE InventoryID = ?")) {
            ps.setString(1, newStatus); ps.setString(2, inventoryId);
            if (ps.executeUpdate() == 0) throw new SQLException("No inventory row found with ID " + inventoryId);
        }
    }

    public void deleteInventory(String inventoryId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM BLOOD_INVENTORY WHERE InventoryID = ?")) {
            ps.setString(1, inventoryId);
            if (ps.executeUpdate() == 0) throw new SQLException("No inventory row found with ID " + inventoryId);
        }
    }
}