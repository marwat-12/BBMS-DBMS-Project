import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class BBMSApp extends JFrame {

    private final DonorDAO donorDAO = new DonorDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final AdminDAO adminDAO = new AdminDAO();
    private final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private final EssentialTestDAO screeningDAO = new EssentialTestDAO();
    private final BloodTestingDAO testingDAO = new BloodTestingDAO();
    private final BloodRecordDAO recordDAO = new BloodRecordDAO();
    private final BloodInventoryDAO inventoryDAO = new BloodInventoryDAO();

    // Used so the Essential Test tab can jump straight to the Blood Testing tab
    // with the ScreeningID already filled in (Admin's "Send to Testing" workflow)
    private JTabbedPane mainTabs;
    private JTextField testingScreeningIdField;
    private JTextField recordDonationIdField;
    private JTextField inventoryDonationIdField;
    private JButton testingSearchButton;
    private static final int ADMIN_TESTING_TAB_INDEX = 5;
    private static final int ADMIN_RECORD_TAB_INDEX = 6;
    private static final int ADMIN_INVENTORY_TAB_INDEX = 7;
    private String myAdminId; // set for Admin role only, used to attribute new inventory rows

    private static final Color BG = new Color(246, 243, 238);
    private static final Color TEAL = new Color(14, 59, 60);
    private static final Color CRIMSON = new Color(160, 20, 30);

    public BBMSApp(String username, String userId, String role) {
        setTitle("LifeLine BBMS — Logged in as " + username + " (" + role + ")");
        setSize(1050, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        // ---- top bar: who's logged in + Logout ----
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(TEAL);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel welcome = new JLabel("\ud83e\ude78  LifeLine BBMS  \u2014  " + username + " (" + role + ")");
        welcome.setForeground(Color.WHITE);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 14f));
        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(CRIMSON);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btnLogout.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new AuthFrame().setVisible(true));
        });
        topBar.add(welcome, BorderLayout.WEST);
        topBar.add(btnLogout, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        String myDonorId = null;
        String myPatientId = null;
        try {
            if (role.equals("Donor")) myDonorId = donorDAO.getDonorIdByUserId(userId);
            if (role.equals("Patient")) myPatientId = patientDAO.getPatientIdByUserId(userId);
            if (role.equals("Admin")) myAdminId = adminDAO.getAdminIdByUserId(userId);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load your profile: " + ex.getMessage());
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBackground(BG);
        tabs.setForeground(TEAL);
        this.mainTabs = tabs;

        // Role-based tabs: Admin sees everything, Donor sees only the donation
        // side, Patient sees only the request side. This mirrors the ISA
        // relationship in the ERD (Admin/Donor/Patient are all a User).
        if (role.equals("Admin")) {
            tabs.addTab("Donors", withHeader("Donors", "Manage donor profiles, eligibility, and phone/address updates", buildDonorTab(true, null)));
            tabs.addTab("Patients", withHeader("Patients", "Manage patient profiles who request blood", buildPatientTab(true, null)));
            tabs.addTab("Admins", withHeader("Admins", "Manage staff accounts with full system access", buildAdminTab()));
            tabs.addTab("Blood Requests", withHeader("Blood Requests", "Review and approve requests made by patients", buildRequestTab(true, null)));
            tabs.addTab("Essential Test", withHeader("Essential Test", "Screen donors and complete pending donation requests", buildScreeningTab(true, null)));
            tabs.addTab("Blood Testing", withHeader("Blood Testing", "Lab panels for eligible donors — auto-queued after screening", buildTestingTab(true, null)));
            tabs.addTab("Blood Record", withHeader("Blood Record", "Finalized donations — auto-queued after a passed test", buildRecordTab(true, null)));
            tabs.addTab("Blood Inventory", withHeader("Blood Inventory", "Current stock — auto-queued after a completed record", buildInventoryTab()));
        } else if (role.equals("Donor")) {
            tabs.addTab("My Donor Profile", withHeader("My Donor Profile", "Your details, and your donation eligibility", buildDonorTab(false, myDonorId)));
            tabs.addTab("Essential Test", withHeader("Essential Test", "Request to donate — an Admin will screen you", buildScreeningTab(false, myDonorId)));
            tabs.addTab("Blood Testing", withHeader("Blood Testing", "Lab results from your donation", buildTestingTab(false, myDonorId)));
            tabs.addTab("Blood Record", withHeader("Blood Record", "Your finalized donation history", buildRecordTab(false, myDonorId)));
        } else if (role.equals("Patient")) {
            tabs.addTab("My Patient Profile", withHeader("My Patient Profile", "Your details", buildPatientTab(false, myPatientId)));
            tabs.addTab("Blood Requests", withHeader("Blood Requests", "Request blood and track its status", buildRequestTab(false, myPatientId)));
        }

        add(tabs, BorderLayout.CENTER);
    }

    // Wraps a tab's content with a colored title/subtitle banner, matching the
    // login/register screens' style, so every tab looks designed rather than a bare form.
    private JPanel withHeader(String title, String subtitle, JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 18));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(CRIMSON);
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 12));
        s.setForeground(new Color(100, 95, 90));
        header.add(t);
        header.add(Box.createVerticalStrut(2));
        header.add(s);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(225, 220, 210));

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(BG);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(sep, BorderLayout.SOUTH);

        wrapper.add(topSection, BorderLayout.NORTH);
        JPanel bodyWrap = new JPanel(new BorderLayout());
        bodyWrap.setBackground(BG);
        bodyWrap.add(content, BorderLayout.CENTER);
        wrapper.add(bodyWrap, BorderLayout.CENTER);
        return wrapper;
    }

    // ================= DONORS TAB =================
    private JPanel buildDonorTab(boolean isAdmin, String ownDonorId) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fUsername = new JTextField(),
                   fEmail = new JTextField(), fName = new JTextField(), fCnic = new JTextField(),
                   fAge = new JTextField(), fPhone = new JTextField(), fAddress = new JTextField(),
                   fDob = new JTextField("YYYY-MM-DD"), fPassword = new JTextField();
        JComboBox<String> fGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        JComboBox<String> fGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});

        // DonorID and UserID are auto-generated on Save — Admin never has to invent or look one up
        form.add(new JLabel("Username"));  form.add(fUsername);
        form.add(new JLabel("Password"));  form.add(fPassword);
        form.add(new JLabel("Email"));     form.add(fEmail);
        form.add(new JLabel("Name"));      form.add(fName);
        form.add(new JLabel("CNIC"));      form.add(fCnic);
        form.add(new JLabel("Gender"));    form.add(fGender);
        form.add(new JLabel("Age"));       form.add(fAge);
        form.add(new JLabel("Phone"));     form.add(fPhone);
        form.add(new JLabel("Address"));   form.add(fAddress);
        form.add(new JLabel("DOB"));       form.add(fDob);
        form.add(new JLabel("Bloodgroup"));form.add(fGroup);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"DonorID","Name","CNIC","Bloodgroup","Phone"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchGroup = new JTextField(10);
        JTextField fDeleteId = new JTextField(6);
        JTextField fUpdateId = new JTextField(isAdmin ? "" : ownDonorId, 6);
        if (!isAdmin) fUpdateId.setEditable(false); // a Donor can only act on their own ID
        JTextField fUpdatePhone = new JTextField(8);
        JLabel eligLabel = new JLabel(" ");

        JButton btnAdd = redButton("Add Donor");
        JButton btnSearch = new JButton(isAdmin ? "Search" : "Refresh");
        JButton btnDelete = new JButton("Delete by ID");
        JButton btnUpdate = new JButton("Update Phone");
        JButton btnElig = new JButton("Check Eligibility");

        // ---- self-service profile edit, Donor only ----
        JTextField fMyAddress = new JTextField(14);
        JTextField fMyPhone = new JTextField(10);
        JComboBox<String> fMyGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        JButton btnSaveProfile = redButton("Save My Profile");

        btnAdd.addActionListener(e -> {
            try {
                String newId = donorDAO.registerDonor(fUsername.getText(), fPassword.getText(), fEmail.getText(),
                        fName.getText(), fCnic.getText(), (String) fGender.getSelectedItem(),
                        Integer.parseInt(fAge.getText()), fPhone.getText(), fAddress.getText(),
                        fDob.getText(), (String) fGroup.getSelectedItem());
                // Admin created this account directly, so queue it into Essential Test right away.
                // (A donor who self-registers via the login screen does NOT get this — they must
                // click "Request to Donate" themselves.)
                screeningDAO.requestDonation(newId);
                JOptionPane.showMessageDialog(this, "Donor added successfully. Assigned DonorID: " + newId +
                        "\nAutomatically queued into Essential Test.");
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Donor Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = isAdmin
                        ? donorDAO.searchDonorsByTerm(fSearchGroup.getText())
                        : donorDAO.getDonorById(ownDonorId); // Donor: only their own row, never the whole table
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Click a row to load its ID into the action fields below (no more guessing IDs);
        // double-click opens that donor's full Slip/History straight away.
        if (isAdmin) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                    fDeleteId.setText(id);
                    fUpdateId.setText(id);
                }
            });
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                        showDonorSlip((String) model.getValueAt(table.getSelectedRow(), 0));
                    }
                }
            });
        }

        btnDelete.addActionListener(e -> {
            try {
                donorDAO.deleteDonor(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Donor deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnUpdate.addActionListener(e -> {
            try {
                donorDAO.updateDonorPhone(fUpdateId.getText(), fUpdatePhone.getText());
                JOptionPane.showMessageDialog(this, "Phone updated.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnElig.addActionListener(e -> {
            try {
                String status = donorDAO.getEligibility(fUpdateId.getText());
                eligLabel.setText("Eligibility: " + status);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Lookup Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSaveProfile.addActionListener(e -> {
            try {
                donorDAO.updateDonorProfile(ownDonorId, fMyPhone.getText(), fMyAddress.getText(), (String) fMyGroup.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Profile updated.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnSlip = new JButton("Generate Slip");
        btnSlip.setBackground(new Color(14, 59, 60));
        btnSlip.setForeground(Color.WHITE);
        btnSlip.addActionListener(e -> {
            String id = isAdmin ? fUpdateId.getText() : ownDonorId;
            showDonorSlip(id);
        });

        form.setVisible(isAdmin); // Donor role doesn't fill this form themselves — Admin creates the account

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(BG);
        if (isAdmin) actions.add(btnAdd);
        if (isAdmin) { actions.add(new JLabel("  Search (name/ID/CNIC/group):")); actions.add(fSearchGroup); }
        actions.add(btnSearch);
        if (isAdmin) {
            actions.add(new JLabel("  Delete ID:")); actions.add(fDeleteId); actions.add(btnDelete);
            actions.add(new JLabel("  ID:"));            actions.add(fUpdateId);
            actions.add(new JLabel("New phone:"));       actions.add(fUpdatePhone); actions.add(btnUpdate);
            actions.add(btnElig); actions.add(eligLabel);
            actions.add(btnSlip);
        } else {
            actions.add(new JLabel("  Phone:"));   actions.add(fMyPhone);
            actions.add(new JLabel("Address:"));   actions.add(fMyAddress);
            actions.add(new JLabel("Bloodgroup:"));actions.add(fMyGroup);
            actions.add(btnSaveProfile);
            actions.add(btnElig); actions.add(eligLabel);
        }

        panel.add(form, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        if (isAdmin) {
            JLabel hint = new JLabel("  Tip: click a row to load its ID below \u00b7 double-click a row to open its Slip/History");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            hint.setForeground(new Color(90, 90, 90));
            centerWrap.add(hint, BorderLayout.NORTH);
        }
        centerWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        btnSearch.doClick();
        return panel;
    }

    // ================= PATIENTS TAB =================
    private JPanel buildPatientTab(boolean isAdmin, String ownPatientId) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fUsername = new JTextField(),
                   fEmail = new JTextField(), fName = new JTextField(), fCnic = new JTextField(),
                   fPhone = new JTextField(), fAddress = new JTextField(), fPassword = new JTextField();
        JComboBox<String> fGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        JComboBox<String> fGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});

        // PatientID and UserID are auto-generated on Save — Admin never has to invent or look one up
        form.add(new JLabel("Username"));  form.add(fUsername);
        form.add(new JLabel("Password"));  form.add(fPassword);
        form.add(new JLabel("Email"));     form.add(fEmail);
        form.add(new JLabel("Name"));      form.add(fName);
        form.add(new JLabel("CNIC"));      form.add(fCnic);
        form.add(new JLabel("Gender"));    form.add(fGender);
        form.add(new JLabel("Bloodgroup"));form.add(fGroup);
        form.add(new JLabel("Phone"));     form.add(fPhone);
        form.add(new JLabel("Address"));   form.add(fAddress);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"PatientID","Name","CNIC","Bloodgroup","Phone"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchGroup = new JTextField(10);
        JTextField fDeleteId = new JTextField(6);
        JTextField fUpdateId = new JTextField(isAdmin ? "" : ownPatientId, 6);
        if (!isAdmin) fUpdateId.setEditable(false); // a Patient can only act on their own ID
        JTextField fUpdatePhone = new JTextField(8);

        JButton btnAdd = redButton("Add Patient");
        JButton btnSearch = new JButton(isAdmin ? "Search" : "Refresh");
        JButton btnDelete = new JButton("Delete by ID");
        JButton btnUpdate = new JButton("Update Phone");

        JTextField fMyAddress = new JTextField(14);
        JTextField fMyPhone = new JTextField(10);
        JComboBox<String> fMyGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        JButton btnSaveProfile = redButton("Save My Profile");

        btnAdd.addActionListener(e -> {
            try {
                String newId = patientDAO.registerPatient(fUsername.getText(), fPassword.getText(), fEmail.getText(),
                        fName.getText(), fCnic.getText(), (String) fGender.getSelectedItem(),
                        (String) fGroup.getSelectedItem(), fPhone.getText(), fAddress.getText());
                JOptionPane.showMessageDialog(this, "Patient added successfully. Assigned PatientID: " + newId);
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Patient Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = isAdmin
                        ? patientDAO.searchPatientsByTerm(fSearchGroup.getText())
                        : patientDAO.getPatientById(ownPatientId); // Patient: only their own row
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        if (isAdmin) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                    fDeleteId.setText(id);
                    fUpdateId.setText(id);
                }
            });
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                        showPatientSlip((String) model.getValueAt(table.getSelectedRow(), 0));
                    }
                }
            });
        }

        btnDelete.addActionListener(e -> {
            try {
                patientDAO.deletePatient(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Patient deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnUpdate.addActionListener(e -> {
            try {
                patientDAO.updatePatientPhone(fUpdateId.getText(), fUpdatePhone.getText());
                JOptionPane.showMessageDialog(this, "Phone updated.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSaveProfile.addActionListener(e -> {
            try {
                patientDAO.updatePatientProfile(ownPatientId, fMyPhone.getText(), fMyAddress.getText(), (String) fMyGroup.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Profile updated.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnSlip = new JButton("Generate Slip");
        btnSlip.setBackground(new Color(14, 59, 60));
        btnSlip.setForeground(Color.WHITE);
        btnSlip.addActionListener(e -> {
            String id = isAdmin ? fUpdateId.getText() : ownPatientId;
            showPatientSlip(id);
        });

        form.setVisible(isAdmin); // Patient role doesn't fill this form themselves — Admin creates the account

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(BG);
        if (isAdmin) actions.add(btnAdd);
        if (isAdmin) { actions.add(new JLabel("  Search (name/ID/CNIC/group):")); actions.add(fSearchGroup); }
        actions.add(btnSearch);
        if (isAdmin) {
            actions.add(new JLabel("  Delete ID:")); actions.add(fDeleteId); actions.add(btnDelete);
            actions.add(new JLabel("  ID:"));            actions.add(fUpdateId);
            actions.add(new JLabel("New phone:"));       actions.add(fUpdatePhone); actions.add(btnUpdate);
            actions.add(btnSlip);
        } else {
            actions.add(new JLabel("  Phone:"));   actions.add(fMyPhone);
            actions.add(new JLabel("Address:"));   actions.add(fMyAddress);
            actions.add(new JLabel("Bloodgroup:"));actions.add(fMyGroup);
            actions.add(btnSaveProfile);
        }

        panel.add(form, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        if (isAdmin) {
            JLabel hint = new JLabel("  Tip: click a row to load its ID below \u00b7 double-click a row to open its Slip/History");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            hint.setForeground(new Color(90, 90, 90));
            centerWrap.add(hint, BorderLayout.NORTH);
        }
        centerWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        btnSearch.doClick();
        return panel;
    }

    // ================= ADMINS TAB =================
    private JPanel buildAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fUsername = new JTextField(), fPassword = new JTextField(),
                   fEmail = new JTextField(), fName = new JTextField(), fPhone = new JTextField();
        form.add(new JLabel("Username")); form.add(fUsername);
        form.add(new JLabel("Password")); form.add(fPassword);
        form.add(new JLabel("Email"));    form.add(fEmail);
        form.add(new JLabel("Name"));     form.add(fName);
        form.add(new JLabel("Phone"));    form.add(fPhone);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"AdminID","Name","Phone"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchTerm = new JTextField(10);
        JTextField fDeleteId = new JTextField(6);
        JTextField fUpdateId = new JTextField(6);
        JTextField fUpdatePhone = new JTextField(8);

        JButton btnAdd = redButton("Add Admin");
        JButton btnSearch = new JButton("Search");
        JButton btnDelete = new JButton("Delete by ID");
        JButton btnUpdate = new JButton("Update Phone");

        btnAdd.addActionListener(e -> {
            try {
                String newId = adminDAO.registerAdmin(fUsername.getText(), fPassword.getText(),
                        fEmail.getText(), fName.getText(), fPhone.getText());
                JOptionPane.showMessageDialog(this, "Admin added successfully. Assigned AdminID: " + newId);
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Admin Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = adminDAO.searchAdminsByTerm(fSearchTerm.getText());
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                fDeleteId.setText(id);
                fUpdateId.setText(id);
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                adminDAO.deleteAdmin(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Admin deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnUpdate.addActionListener(e -> {
            try {
                adminDAO.updateAdminPhone(fUpdateId.getText(), fUpdatePhone.getText());
                JOptionPane.showMessageDialog(this, "Phone updated.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBackground(BG);
        actions.add(btnAdd);
        actions.add(new JLabel("  Search (name/ID/phone):")); actions.add(fSearchTerm); actions.add(btnSearch);
        actions.add(new JLabel("  Delete ID:")); actions.add(fDeleteId); actions.add(btnDelete);
        actions.add(new JLabel("  ID:"));        actions.add(fUpdateId);
        actions.add(new JLabel("New phone:"));   actions.add(fUpdatePhone); actions.add(btnUpdate);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        btnSearch.doClick();
        return panel;
    }

    // ================= BLOOD REQUESTS TAB =================
    private JPanel buildRequestTab(boolean isAdmin, String ownPatientId) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fPatientId = new JTextField(isAdmin ? "" : ownPatientId);
        if (!isAdmin) fPatientId.setEditable(false); // a Patient can only request for themselves
        JTextField fQty = new JTextField();
        JComboBox<String> fGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        // RequestID is auto-generated on Save
        form.add(new JLabel("PatientID"));  form.add(fPatientId);
        form.add(new JLabel("Required Group")); form.add(fGroup);
        form.add(new JLabel("Quantity"));   form.add(fQty);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"RequestID","Patient","Group","Qty","Status"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchStatus = new JTextField(10);
        JTextField fApproveId = new JTextField(6);
        JTextField fApproveAdmin = new JTextField(isAdmin && myAdminId != null ? myAdminId : "", 6);
        JTextField fFulfillId = new JTextField(6);
        JTextField fDeleteId = new JTextField(6);

        JButton btnAdd = redButton("New Request");
        JButton btnSearch = new JButton(isAdmin ? "Search" : "Refresh My Requests");
        JButton btnApprove = new JButton("Approve");
        JButton btnFulfill = redButton("Fulfil (Allocate Stock)");
        JButton btnDelete = new JButton("Delete by ID");

        btnAdd.addActionListener(e -> {
            try {
                String newId = requestDAO.createRequest(fPatientId.getText(),
                        (String) fGroup.getSelectedItem(), Integer.parseInt(fQty.getText()));
                JOptionPane.showMessageDialog(this, "Request created. Reference: " + newId);
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Request Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = isAdmin
                        ? requestDAO.searchRequestsByTerm(fSearchStatus.getText())
                        : requestDAO.searchRequestsByPatient(ownPatientId); // Patient: only their own requests
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        if (isAdmin) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                    fApproveId.setText(id);
                    fFulfillId.setText(id);
                    fDeleteId.setText(id);
                }
            });
        }

        btnApprove.addActionListener(e -> {
            try {
                requestDAO.approveRequest(fApproveId.getText(), fApproveAdmin.getText());
                JOptionPane.showMessageDialog(this, "Request approved.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Approve Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnFulfill.addActionListener(e -> {
            try {
                requestDAO.fulfillRequest(fFulfillId.getText());
                JOptionPane.showMessageDialog(this, "Stock allocated from Inventory. Request marked Fulfilled.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Fulfil Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                requestDAO.deleteRequest(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Request deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.setVisible(!isAdmin); // only a Patient fills out a new request (the "Makes" relationship)

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(BG);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setBackground(BG);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setBackground(BG);
        if (!isAdmin) row1.add(btnAdd); // Patient: create a request
        if (isAdmin) { row1.add(new JLabel("  Search (RequestID/PatientID/name):")); row1.add(fSearchStatus); }
        row1.add(btnSearch);
        if (isAdmin) { // Admin: approve, allocate stock, and manage requests
            row2.add(new JLabel("Approve ID:"));     row2.add(fApproveId);
            row2.add(new JLabel("Admin ID:"));       row2.add(fApproveAdmin); row2.add(btnApprove);
            row2.add(new JLabel("   Fulfil ID:"));   row2.add(fFulfillId);    row2.add(btnFulfill);
            row2.add(new JLabel("   Delete ID:"));   row2.add(fDeleteId);     row2.add(btnDelete);
        }
        actions.add(row1);
        actions.add(row2);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        btnSearch.doClick();
        return panel;
    }

    // ================= ESSENTIAL TEST (SCREENING) TAB =================
    private JPanel buildScreeningTab(boolean isAdmin, String ownDonorId) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fScreeningId = new JTextField(), fDonorId = new JTextField(), fHemo = new JTextField(),
                   fPulse = new JTextField(), fWeight = new JTextField(), fBp = new JTextField(), fQty = new JTextField();
        form.add(new JLabel("ScreeningID")); form.add(fScreeningId);
        form.add(new JLabel("DonorID"));     form.add(fDonorId);
        form.add(new JLabel("Hemoglobin"));  form.add(fHemo);
        form.add(new JLabel("Pulse"));       form.add(fPulse);
        form.add(new JLabel("Weight"));      form.add(fWeight);
        form.add(new JLabel("Blood Pressure")); form.add(fBp);
        form.add(new JLabel("Quantity (units)")); form.add(fQty);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"ScreeningID","DonorID","Hemoglobin","Pulse","Weight","Eligibility","Quantity"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchElig = new JTextField(8);
        JTextField fDeleteId = new JTextField(6);

        JButton btnAdd = redButton("Screen Donor");
        JButton btnSearch = new JButton(isAdmin ? "Search" : "Refresh My History");
        JButton btnDelete = new JButton("Delete by ID");
        JButton btnRequestDonate = redButton("Request to Donate");

        // ---- Admin: complete a Donor's pending donation request with real vitals ----
        JTextField fCompleteId = new JTextField(6);
        JTextField fCompleteHemo = new JTextField(5);
        JTextField fCompletePulse = new JTextField(5);
        JTextField fCompleteWeight = new JTextField(5);
        JTextField fCompleteBp = new JTextField(6);
        JTextField fCompleteQty = new JTextField(4);
        JButton btnComplete = redButton("Complete Screening");
        JTextField fSendId = new JTextField(6);
        JButton btnSendToTesting = new JButton("Send to Blood Testing \u2192");
        JButton btnPendingOnly = new JButton("Show Pending Only");
        JButton btnCompletedOnly = new JButton("Show Completed Only");

        btnAdd.addActionListener(e -> {
            try {
                screeningDAO.addScreening(fScreeningId.getText(), fDonorId.getText(),
                        Double.parseDouble(fHemo.getText()), Integer.parseInt(fPulse.getText()),
                        Double.parseDouble(fWeight.getText()), fBp.getText(),
                        fQty.getText().isBlank() ? 0 : Integer.parseInt(fQty.getText()));
                JOptionPane.showMessageDialog(this, "Screening saved.");
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Screening Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRequestDonate.addActionListener(e -> {
            try {
                String newId = screeningDAO.requestDonation(ownDonorId);
                JOptionPane.showMessageDialog(this, "Donation request sent! Reference: " + newId +
                        "\nAn Admin will screen you and update this soon.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Request Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnComplete.addActionListener(e -> {
            try {
                double hb = Double.parseDouble(fCompleteHemo.getText());
                double wt = Double.parseDouble(fCompleteWeight.getText());
                int qty = fCompleteQty.getText().isBlank() ? 0 : Integer.parseInt(fCompleteQty.getText());
                screeningDAO.completeScreening(fCompleteId.getText(),
                        hb, Integer.parseInt(fCompletePulse.getText()), wt, fCompleteBp.getText(), qty);
                if (hb >= 12.5 && wt >= 50) {
                    // Eligible — automatically move this donor into the Blood Testing queue
                    testingDAO.createPendingTest(fCompleteId.getText());
                    JOptionPane.showMessageDialog(this, "Screening completed — Eligible (" + qty + " unit(s) drawn). Automatically added to Blood Testing.");
                } else {
                    JOptionPane.showMessageDialog(this, "Screening completed — Ineligible.");
                }
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Complete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSendToTesting.addActionListener(e -> {
            if (testingScreeningIdField != null) {
                testingScreeningIdField.setText(fSendId.getText());
            }
            if (mainTabs != null) mainTabs.setSelectedIndex(ADMIN_TESTING_TAB_INDEX);
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = isAdmin
                        ? screeningDAO.searchScreeningsByTerm(fSearchElig.getText())
                        : screeningDAO.searchScreeningsByDonor(ownDonorId); // Donor: only their own history
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPendingOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : screeningDAO.searchPending()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCompletedOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : screeningDAO.searchCompleted()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                screeningDAO.deleteScreening(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Screening deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        if (isAdmin) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                    fDeleteId.setText(id);
                    fCompleteId.setText(id);
                    fSendId.setText(id);
                }
            });
        }

        form.setVisible(isAdmin); // only Admin performs a fresh, complete screening

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(BG);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setBackground(BG);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setBackground(BG);
        if (isAdmin) {
            row1.add(btnAdd);
            row1.add(new JLabel("  Delete ID:")); row1.add(fDeleteId); row1.add(btnDelete);
            row1.add(new JLabel("  Search (ScreeningID/DonorID):")); row1.add(fSearchElig);
            row1.add(btnSearch);
            row1.add(btnPendingOnly); row1.add(btnCompletedOnly);
            row2.add(new JLabel("Complete pending — ScreeningID:")); row2.add(fCompleteId);
            row2.add(new JLabel("Hb:")); row2.add(fCompleteHemo);
            row2.add(new JLabel("Pulse:")); row2.add(fCompletePulse);
            row2.add(new JLabel("Weight:")); row2.add(fCompleteWeight);
            row2.add(new JLabel("BP:")); row2.add(fCompleteBp);
            row2.add(new JLabel("Qty (if eligible):")); row2.add(fCompleteQty);
            row2.add(btnComplete);
            row2.add(new JLabel("   ScreeningID:")); row2.add(fSendId); row2.add(btnSendToTesting);
        } else {
            row1.add(btnRequestDonate);
            row1.add(btnSearch);
        }
        actions.add(row1);
        actions.add(row2);

        panel.add(form, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        if (isAdmin) {
            JLabel hint = new JLabel("  Tip: click a row to load its ScreeningID into Delete / Complete / Send fields below");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            hint.setForeground(new Color(90, 90, 90));
            centerWrap.add(hint, BorderLayout.NORTH);
        }
        centerWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        btnSearch.doClick();
        return panel;
    }

    // ================= BLOOD TESTING TAB =================
    private JPanel buildTestingTab(boolean isAdmin, String ownDonorId) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fTestId = new JTextField(), fScreeningId = new JTextField();
        if (isAdmin) this.testingScreeningIdField = fScreeningId; // lets Essential Test tab prefill this
        JComboBox<String> fHiv = new JComboBox<>(new String[]{"Negative","Positive"});
        JComboBox<String> fHepB = new JComboBox<>(new String[]{"Negative","Positive"});
        JComboBox<String> fHepC = new JComboBox<>(new String[]{"Negative","Positive"});
        JComboBox<String> fSyph = new JComboBox<>(new String[]{"Negative","Positive"});
        form.add(new JLabel("TestID"));      form.add(fTestId);
        form.add(new JLabel("ScreeningID")); form.add(fScreeningId);
        form.add(new JLabel("HIV"));         form.add(fHiv);
        form.add(new JLabel("Hepatitis B")); form.add(fHepB);
        form.add(new JLabel("Hepatitis C")); form.add(fHepC);
        form.add(new JLabel("Syphilis"));    form.add(fSyph);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"TestID","ScreeningID","HIV","HepB","HepC","Syphilis","Result"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchResult = new JTextField(10);
        JTextField fDeleteId = new JTextField(6);

        JButton btnAdd = redButton("Log Test");
        JButton btnSearch = new JButton(isAdmin ? "Search" : "Refresh My History");
        JButton btnDelete = new JButton("Delete by ID");

        // ---- Admin: complete a Pending test row with real lab results ----
        JTextField fCompleteId = new JTextField(6);
        JComboBox<String> fCHiv = new JComboBox<>(new String[]{"Negative","Positive"});
        JComboBox<String> fCHepB = new JComboBox<>(new String[]{"Negative","Positive"});
        JComboBox<String> fCHepC = new JComboBox<>(new String[]{"Negative","Positive"});
        JComboBox<String> fCSyph = new JComboBox<>(new String[]{"Negative","Positive"});
        JButton btnComplete = redButton("Complete Test");
        JTextField fSendRecordId = new JTextField(6);
        JButton btnSendToRecord = new JButton("Send to Blood Record \u2192");
        JButton btnPendingOnly = new JButton("Show Pending Only");
        JButton btnCompletedOnly = new JButton("Show Completed Only");

        btnAdd.addActionListener(e -> {
            try {
                testingDAO.addTest(fTestId.getText(), fScreeningId.getText(),
                        (String) fHiv.getSelectedItem(), (String) fHepB.getSelectedItem(),
                        (String) fHepC.getSelectedItem(), (String) fSyph.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Test panel saved.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Test Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = isAdmin
                        ? testingDAO.searchTestsByTerm(fSearchResult.getText())
                        : testingDAO.searchTestsByDonor(ownDonorId); // Donor: only their own test panels
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnComplete.addActionListener(e -> {
            try {
                String hiv = (String) fCHiv.getSelectedItem(), hepB = (String) fCHepB.getSelectedItem(),
                       hepC = (String) fCHepC.getSelectedItem(), syph = (String) fCSyph.getSelectedItem();
                testingDAO.completeTest(fCompleteId.getText(), hiv, hepB, hepC, syph);
                boolean pass = hiv.equals("Negative") && hepB.equals("Negative") && hepC.equals("Negative") && syph.equals("Negative");
                if (pass) {
                    String donationId = recordDAO.createPendingRecord(fCompleteId.getText());
                    inventoryDAO.createPendingInventory(donationId, myAdminId);
                    JOptionPane.showMessageDialog(this, "Test completed — Pass. Automatically added to Blood Record AND Blood Inventory.");
                } else {
                    JOptionPane.showMessageDialog(this, "Test completed — Fail.");
                }
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Complete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSendToRecord.addActionListener(e -> {
            if (recordDonationIdField != null) recordDonationIdField.setText(fSendRecordId.getText());
            if (mainTabs != null) mainTabs.setSelectedIndex(ADMIN_RECORD_TAB_INDEX);
        });

        btnPendingOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : testingDAO.searchPending()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCompletedOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : testingDAO.searchCompleted()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                testingDAO.deleteTest(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Test deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        if (isAdmin) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                    fDeleteId.setText(id);
                    fCompleteId.setText(id);
                    fSendRecordId.setText(id);
                }
            });
        }

        form.setVisible(isAdmin); // only Admin logs a fresh, complete test panel

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(BG);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setBackground(BG);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setBackground(BG);
        if (isAdmin) {
            row1.add(btnAdd);
            row1.add(new JLabel("  Delete ID:")); row1.add(fDeleteId); row1.add(btnDelete);
            row1.add(new JLabel("  Search (TestID/ScreeningID):")); row1.add(fSearchResult);
            row1.add(btnSearch);
            row1.add(btnPendingOnly); row1.add(btnCompletedOnly);
            row2.add(new JLabel("Complete pending — TestID:")); row2.add(fCompleteId);
            row2.add(new JLabel("HIV:")); row2.add(fCHiv);
            row2.add(new JLabel("HepB:")); row2.add(fCHepB);
            row2.add(new JLabel("HepC:")); row2.add(fCHepC);
            row2.add(new JLabel("Syph:")); row2.add(fCSyph);
            row2.add(btnComplete);
            row2.add(new JLabel("   TestID:")); row2.add(fSendRecordId); row2.add(btnSendToRecord);
        } else {
            row1.add(btnSearch);
        }
        actions.add(row1);
        actions.add(row2);

        panel.add(form, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        if (isAdmin) {
            JLabel hint = new JLabel("  Tip: click a row to load its TestID below \u00b7 Pending rows appear automatically once a screening is Eligible");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            hint.setForeground(new Color(90, 90, 90));
            centerWrap.add(hint, BorderLayout.NORTH);
        }
        centerWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        btnSearch.doClick();
        return panel;
    }

    // ================= BLOOD RECORD TAB =================
    private JPanel buildRecordTab(boolean isAdmin, String ownDonorId) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fDonationId = new JTextField(), fTestId = new JTextField(), fQty = new JTextField();
        JComboBox<String> fGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        form.add(new JLabel("DonationID")); form.add(fDonationId);
        form.add(new JLabel("TestID"));     form.add(fTestId);
        form.add(new JLabel("Bloodgroup")); form.add(fGroup);
        form.add(new JLabel("Quantity"));   form.add(fQty);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"DonationID","TestID","Date","Bloodgroup","Quantity"}, 0);
        JTable table = styledTable(model);

        JTextField fDeleteId = new JTextField(6);
        JButton btnAdd = redButton("Create Record");
        JButton btnSearch = new JButton(isAdmin ? "Refresh List" : "Refresh My History");
        JButton btnDelete = new JButton("Delete by ID");

        // ---- Admin: complete a Pending record with real donation details ----
        JTextField fCompleteId = new JTextField(6);
        if (isAdmin) this.recordDonationIdField = fCompleteId; // lets Blood Testing tab prefill this
        JComboBox<String> fCompleteGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        JTextField fCompleteQty = new JTextField(4);
        fCompleteQty.setEditable(false); // quantity was already entered once at screening — shown here, not retyped
        JButton btnComplete = redButton("Complete Record");
        JTextField fSendInvId = new JTextField(6);
        JButton btnSendToInventory = new JButton("Send to Inventory \u2192");
        JButton btnPendingOnly = new JButton("Show Pending Only");
        JButton btnCompletedOnly = new JButton("Show Completed Only");

        if (isAdmin) {
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                    fDeleteId.setText(id);
                    fCompleteId.setText(id);
                    fSendInvId.setText(id);
                    Object grp = model.getValueAt(table.getSelectedRow(), 3);
                    Object qty = model.getValueAt(table.getSelectedRow(), 4);
                    if (grp != null) fCompleteGroup.setSelectedItem(grp.toString());
                    fCompleteQty.setText(qty == null ? "0" : qty.toString());
                }
            });
        }

        btnAdd.addActionListener(e -> {
            try {
                recordDAO.addRecord(fDonationId.getText(), fTestId.getText(),
                        (String) fGroup.getSelectedItem(), Integer.parseInt(fQty.getText()));
                JOptionPane.showMessageDialog(this, "Record created.");
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Record Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = isAdmin
                        ? recordDAO.searchRecords()
                        : recordDAO.searchRecordsByDonor(ownDonorId); // Donor: only their own records
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnComplete.addActionListener(e -> {
            try {
                recordDAO.completeRecord(fCompleteId.getText(), (String) fCompleteGroup.getSelectedItem(),
                        Integer.parseInt(fCompleteQty.getText()));
                if (!inventoryDAO.hasInventoryForDonation(fCompleteId.getText())) {
                    inventoryDAO.createPendingInventory(fCompleteId.getText(), myAdminId);
                    JOptionPane.showMessageDialog(this, "Record completed. Added to Blood Inventory.");
                } else {
                    JOptionPane.showMessageDialog(this, "Record completed.");
                }
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Complete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSendToInventory.addActionListener(e -> {
            if (inventoryDonationIdField != null) inventoryDonationIdField.setText(fSendInvId.getText());
            if (mainTabs != null) mainTabs.setSelectedIndex(ADMIN_INVENTORY_TAB_INDEX);
        });

        btnPendingOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : recordDAO.searchPending()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCompletedOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : recordDAO.searchCompleted()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                recordDAO.deleteRecord(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Record deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        form.setVisible(isAdmin); // only Admin generates a fresh, complete blood record

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(BG);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setBackground(BG);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setBackground(BG);
        if (isAdmin) {
            row1.add(btnAdd);
            row1.add(new JLabel("  Delete ID:")); row1.add(fDeleteId); row1.add(btnDelete);
            row1.add(btnSearch);
            row1.add(btnPendingOnly); row1.add(btnCompletedOnly);
            row2.add(new JLabel("Complete pending — DonationID:")); row2.add(fCompleteId);
            row2.add(new JLabel("Bloodgroup:")); row2.add(fCompleteGroup);
            row2.add(new JLabel("Quantity (auto):")); row2.add(fCompleteQty);
            row2.add(btnComplete);
            row2.add(new JLabel("   DonationID:")); row2.add(fSendInvId); row2.add(btnSendToInventory);
        } else {
            row1.add(btnSearch);
        }
        actions.add(row1);
        actions.add(row2);

        panel.add(form, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        if (isAdmin) {
            JLabel hint = new JLabel("  Tip: click a row to load its DonationID below \u00b7 Pending rows appear automatically once a test Passes");
            hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
            hint.setForeground(new Color(90, 90, 90));
            centerWrap.add(hint, BorderLayout.NORTH);
        }
        centerWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        btnSearch.doClick();
        return panel;
    }

    // ================= BLOOD INVENTORY TAB =================
    private JPanel buildInventoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(BG);

        JPanel form = new JPanel(new GridLayout(0, 4, 6, 6));
        form.setBackground(BG);
        JTextField fInvId = new JTextField(), fDonationId = new JTextField(), fAdminId = new JTextField(),
                   fCollDate = new JTextField("YYYY-MM-DD"), fExpDate = new JTextField("YYYY-MM-DD"), fQty = new JTextField();
        JComboBox<String> fStatus = new JComboBox<>(new String[]{"Available","Reserved","Expired"});
        form.add(new JLabel("InventoryID"));    form.add(fInvId);
        form.add(new JLabel("DonationID"));     form.add(fDonationId);
        form.add(new JLabel("AdminID"));        form.add(fAdminId);
        form.add(new JLabel("Status"));         form.add(fStatus);
        form.add(new JLabel("Collection Date"));form.add(fCollDate);
        form.add(new JLabel("Expiry Date"));    form.add(fExpDate);
        form.add(new JLabel("Quantity"));       form.add(fQty);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"InventoryID","Bloodgroup","Quantity","Expiry","Status"}, 0);
        JTable table = styledTable(model);

        JTextField fSearchStatus = new JTextField(10);
        JTextField fUpdateId = new JTextField(6);
        JComboBox<String> fNewStatus = new JComboBox<>(new String[]{"Available","Reserved","Expired"});
        JTextField fDeleteId = new JTextField(6);

        JButton btnAdd = redButton("Add Stock");
        JButton btnSearch = new JButton("Search");
        JButton btnUpdate = new JButton("Update Status");
        JButton btnDelete = new JButton("Delete by ID");
        JButton btnPendingOnly = new JButton("Show Pending Only");
        JButton btnCompletedOnly = new JButton("Show Completed Only");

        // ---- Admin: complete a Pending inventory row with real stock details ----
        JTextField fCompleteId = new JTextField(6);
        this.inventoryDonationIdField = fCompleteId; // lets Blood Record tab prefill this
        JTextField fCompleteColl = new JTextField("YYYY-MM-DD", 8);
        JTextField fCompleteExp = new JTextField("YYYY-MM-DD", 8);
        JTextField fCompleteQty = new JTextField(4);
        fCompleteQty.setEditable(false); // quantity already flowed in from Blood Record — shown here, not retyped
        JButton btnComplete = redButton("Complete Stock");

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                String id = (String) model.getValueAt(table.getSelectedRow(), 0);
                fUpdateId.setText(id);
                fDeleteId.setText(id);
                fCompleteId.setText(id);
                Object qty = model.getValueAt(table.getSelectedRow(), 2);
                fCompleteQty.setText(qty == null ? "0" : qty.toString());
            }
        });

        btnAdd.addActionListener(e -> {
            try {
                inventoryDAO.addInventory(fInvId.getText(), fDonationId.getText(), fAdminId.getText(),
                        (String) fStatus.getSelectedItem(), fCollDate.getText(), fExpDate.getText(),
                        Integer.parseInt(fQty.getText()));
                JOptionPane.showMessageDialog(this, "Stock added.");
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Add Stock Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSearch.addActionListener(e -> {
            try {
                model.setRowCount(0);
                List<String[]> rows = inventoryDAO.searchInventoryByTerm(fSearchStatus.getText());
                for (String[] r : rows) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnComplete.addActionListener(e -> {
            try {
                inventoryDAO.completeInventory(fCompleteId.getText(), fCompleteColl.getText(), fCompleteExp.getText());
                JOptionPane.showMessageDialog(this, "Stock completed and marked Available.");
                btnSearch.doClick();
            } catch (SQLException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Complete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnUpdate.addActionListener(e -> {
            try {
                inventoryDAO.updateStatus(fUpdateId.getText(), (String) fNewStatus.getSelectedItem());
                JOptionPane.showMessageDialog(this, "Status updated.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            try {
                inventoryDAO.deleteInventory(fDeleteId.getText());
                JOptionPane.showMessageDialog(this, "Inventory row deleted.");
                btnSearch.doClick();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPendingOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : inventoryDAO.searchPending()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCompletedOnly.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] r : inventoryDAO.searchCompleted()) model.addRow(r);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Search Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(BG);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setBackground(BG);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setBackground(BG);
        row1.add(btnAdd);
        row1.add(new JLabel("  Search (InventoryID/DonationID/group):")); row1.add(fSearchStatus); row1.add(btnSearch);
        row1.add(new JLabel("  ID:")); row1.add(fUpdateId); row1.add(fNewStatus); row1.add(btnUpdate);
        row1.add(new JLabel("  Delete ID:")); row1.add(fDeleteId); row1.add(btnDelete);
        row1.add(btnPendingOnly); row1.add(btnCompletedOnly);
        row2.add(new JLabel("Complete pending — InventoryID:")); row2.add(fCompleteId);
        row2.add(new JLabel("Collected:")); row2.add(fCompleteColl);
        row2.add(new JLabel("Expiry:")); row2.add(fCompleteExp);
        row2.add(new JLabel("Qty (auto):")); row2.add(fCompleteQty);
        row2.add(btnComplete);
        actions.add(row1);
        actions.add(row2);

        panel.add(form, BorderLayout.NORTH);
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG);
        JLabel hint = new JLabel("  Tip: click a row to load its ID below \u00b7 Pending rows appear automatically once a record is completed");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        hint.setForeground(new Color(90, 90, 90));
        centerWrap.add(hint, BorderLayout.NORTH);
        centerWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(centerWrap, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        btnSearch.doClick();
        return panel;
    }

    // ================= SLIP / RECEIPT GENERATION =================
    private void showDonorSlip(String donorId) {
        if (donorId == null || donorId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter or select a DonorID first (use the ID field above).");
            return;
        }
        StringBuilder sb = new StringBuilder();
        try {
            List<String[]> profile = donorDAO.getDonorById(donorId);
            sb.append("========== LifeLine BBMS — Donor Slip ==========\n\n");
            if (profile.isEmpty()) {
                sb.append("No donor found with ID ").append(donorId).append("\n");
            } else {
                String[] p = profile.get(0);
                sb.append("DonorID     : ").append(p[0]).append('\n');
                sb.append("Name        : ").append(p[1]).append('\n');
                sb.append("CNIC        : ").append(p[2]).append('\n');
                sb.append("Bloodgroup  : ").append(p[3]).append('\n');
                sb.append("Phone       : ").append(p[4]).append("\n\n");

                sb.append("---- Screening History ----\n");
                for (String[] r : screeningDAO.searchScreeningsByDonor(donorId))
                    sb.append(String.format("%-8s Hb:%-6s Pulse:%-5s Wt:%-6s %s%n", r[0], r[2], r[3], r[4], r[5]));

                sb.append("\n---- Blood Testing History ----\n");
                for (String[] r : testingDAO.searchTestsByDonor(donorId))
                    sb.append(String.format("%-8s HIV:%-9s HepB:%-9s HepC:%-9s Syph:%-9s Result:%s%n",
                            r[0], r[2], r[3], r[4], r[5], r[6]));

                sb.append("\n---- Blood Donation Records ----\n");
                for (String[] r : recordDAO.searchRecordsByDonor(donorId))
                    sb.append(String.format("%-8s %-12s %-4s Qty:%s%n", r[0], r[2], r[3], r[4]));
            }
        } catch (SQLException ex) {
            sb.append("Error building slip: ").append(ex.getMessage());
        }
        showSlipDialog("Donor Slip — " + donorId, sb.toString());
    }

    private void showPatientSlip(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter or select a PatientID first (use the ID field above).");
            return;
        }
        StringBuilder sb = new StringBuilder();
        try {
            List<String[]> profile = patientDAO.getPatientById(patientId);
            sb.append("========== LifeLine BBMS — Patient Slip ==========\n\n");
            if (profile.isEmpty()) {
                sb.append("No patient found with ID ").append(patientId).append("\n");
            } else {
                String[] p = profile.get(0);
                sb.append("PatientID   : ").append(p[0]).append('\n');
                sb.append("Name        : ").append(p[1]).append('\n');
                sb.append("CNIC        : ").append(p[2]).append('\n');
                sb.append("Bloodgroup  : ").append(p[3]).append('\n');
                sb.append("Phone       : ").append(p[4]).append("\n\n");

                sb.append("---- Blood Request History ----\n");
                for (String[] r : requestDAO.searchRequestsByPatient(patientId))
                    sb.append(String.format("%-8s Group:%-4s Qty:%-4s %s%n", r[0], r[2], r[3], r[4]));
            }
        } catch (SQLException ex) {
            sb.append("Error building slip: ").append(ex.getMessage());
        }
        showSlipDialog("Patient Slip — " + patientId, sb.toString());
    }

    private void showSlipDialog(String title, String content) {
        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(this);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);

        JButton btnCopy = new JButton("Copy to Clipboard");
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(content), null);
            JOptionPane.showMessageDialog(dialog, "Copied.");
        });
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(btnCopy); foot.add(btnClose);
        dialog.add(foot, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ================= LOOK & FEEL HELPERS =================
    // A crimson "call to action" button matching the brand color used on the login/register screens.
    private JButton redButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(160, 20, 30));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        return b;
    }

    // A table with a bold header, taller rows, and alternating row colors so long lists are easier to read.
    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 235, 235));
                }
                return c;
            }
        };
        table.setRowHeight(24);
        table.setGridColor(new Color(225, 225, 225));
        table.setSelectionBackground(new Color(160, 20, 30));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(new Color(14, 59, 60));
        table.getTableHeader().setForeground(Color.WHITE);
        return table;
    }
}
