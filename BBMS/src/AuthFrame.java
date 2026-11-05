import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class AuthFrame extends JFrame {
    private final UserDAO userDAO = new UserDAO();
    private final DonorDAO donorDAO = new DonorDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    // ---- EDIT THESE TWO PATHS to wherever you saved each background image ----
    private static final String LOGIN_BG_PATH    = "D:\\DBMS PROJECT\\BBMS\\login_bg.jpg";
    private static final String REGISTER_BG_PATH = "D:\\DBMS PROJECT\\BBMS\\register_bg.jpg";
    // ---------------------------------------------------------------------

    private static final int W = 1200, H = 682;

    private final JLabel background = new JLabel();
    private final JPanel formHolder = new JPanel();

    public AuthFrame() {
        setTitle("LifeLine BBMS");
        setSize(W, H);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JLayeredPane layers = new JLayeredPane();
        layers.setPreferredSize(new Dimension(W, H));
        setContentPane(layers);

        background.setBounds(0, 0, W, H);
        layers.add(background, JLayeredPane.DEFAULT_LAYER);

        formHolder.setOpaque(false);
        formHolder.setBounds(307, 14, 528, 543); // the blank white area of the image
        layers.add(formHolder, JLayeredPane.PALETTE_LAYER);

        showLoginView();
    }

    private void setBackgroundImage(String path) {
        ImageIcon raw = new ImageIcon(path);
        Image scaled = raw.getImage().getScaledInstance(W, H, Image.SCALE_SMOOTH);
        background.setIcon(new ImageIcon(scaled));
    }

    // ================= LOGIN VIEW =================
    private void showLoginView() {
        setBackgroundImage(LOGIN_BG_PATH);
        formHolder.removeAll();
        formHolder.setLayout(new GridBagLayout());
        formHolder.add(buildLoginBox());
        formHolder.revalidate();
        formHolder.repaint();
    }

    private JPanel buildLoginBox() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Sign in to LifeLine BBMS");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(160, 20, 30));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField fUsername = new JTextField();
        JPasswordField fPassword = new JPasswordField();
        widen(fUsername); widen(fPassword);

        JLabel status = new JLabel(" ");
        status.setForeground(Color.RED);
        status.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnLogin = redButton("Login");
        JButton toRegister = linkButton("New here? Register");

        form.add(title);
        form.add(Box.createVerticalStrut(30));
        form.add(labeled("Username")); form.add(Box.createVerticalStrut(5)); form.add(fUsername);
        form.add(Box.createVerticalStrut(15));
        form.add(labeled("Password")); form.add(Box.createVerticalStrut(5)); form.add(fPassword);
        form.add(Box.createVerticalStrut(20));
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(10));
        form.add(status);
        form.add(Box.createVerticalStrut(25));
        form.add(toRegister);

        btnLogin.addActionListener(e -> {
            String username = fUsername.getText();
            String password = new String(fPassword.getPassword());
            try {
                String[] result = userDAO.login(username, password);
                if (result == null) {
                    status.setText("Invalid username or password.");
                } else {
                    String role = result[1];
                    String userId = result[0];
                    dispose();
                    SwingUtilities.invokeLater(() -> new BBMSApp(username, userId, role).setVisible(true));
                }
            } catch (SQLException ex) {
                status.setText("DB error: " + ex.getMessage());
            }
        });

        toRegister.addActionListener(e -> showRegisterView());
        return form;
    }

    // ================= REGISTER VIEW =================
    private void showRegisterView() {
        setBackgroundImage(REGISTER_BG_PATH);
        formHolder.removeAll();
        formHolder.setLayout(new BorderLayout());
        formHolder.add(buildRegisterBox(), BorderLayout.CENTER);
        formHolder.revalidate();
        formHolder.repaint();
    }

    private JComponent buildRegisterBox() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Create your account");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(160, 20, 30));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<String> fRole = new JComboBox<>(new String[]{"Donor", "Patient"});
        JTextField fName = new JTextField();
        JTextField fCnic = new JTextField();
        JComboBox<String> fGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        JComboBox<String> fGroup = new JComboBox<>(new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        JTextField fPhone = new JTextField();
        JTextField fAddress = new JTextField();
        JTextField fAge = new JTextField();
        JTextField fDob = new JTextField("YYYY-MM-DD");
        JTextField fUsername = new JTextField();
        JTextField fEmail = new JTextField();
        JPasswordField fPassword = new JPasswordField();

        for (JComponent c : new JComponent[]{fRole, fName, fCnic, fGender, fGroup, fPhone,
                fAddress, fAge, fDob, fUsername, fEmail, fPassword}) widen(c);

        JPanel donorExtra = new JPanel();
        donorExtra.setOpaque(false);
        donorExtra.setLayout(new BoxLayout(donorExtra, BoxLayout.Y_AXIS));
        donorExtra.add(labeled("Age")); donorExtra.add(Box.createVerticalStrut(3)); donorExtra.add(fAge);
        donorExtra.add(Box.createVerticalStrut(8));
        donorExtra.add(labeled("Date of Birth")); donorExtra.add(Box.createVerticalStrut(3)); donorExtra.add(fDob);
        donorExtra.add(Box.createVerticalStrut(8));
        donorExtra.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel status = new JLabel(" ");
        status.setForeground(Color.RED);
        status.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnRegister = redButton("Register");
        JButton toLogin = linkButton("Already have an account? Sign in");

        form.add(title);
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("I am a")); form.add(Box.createVerticalStrut(3)); form.add(fRole);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Full Name")); form.add(Box.createVerticalStrut(3)); form.add(fName);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("CNIC")); form.add(Box.createVerticalStrut(3)); form.add(fCnic);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Gender")); form.add(Box.createVerticalStrut(3)); form.add(fGender);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Blood Group")); form.add(Box.createVerticalStrut(3)); form.add(fGroup);
        form.add(Box.createVerticalStrut(8));
        form.add(donorExtra); // only meaningful for Donor, hidden for Patient
        form.add(labeled("Phone")); form.add(Box.createVerticalStrut(3)); form.add(fPhone);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Address")); form.add(Box.createVerticalStrut(3)); form.add(fAddress);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Username")); form.add(Box.createVerticalStrut(3)); form.add(fUsername);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Email")); form.add(Box.createVerticalStrut(3)); form.add(fEmail);
        form.add(Box.createVerticalStrut(8));
        form.add(labeled("Password")); form.add(Box.createVerticalStrut(3)); form.add(fPassword);
        form.add(Box.createVerticalStrut(15));
        form.add(btnRegister);
        form.add(Box.createVerticalStrut(8));
        form.add(status);
        form.add(Box.createVerticalStrut(15));
        form.add(toLogin);

        Runnable toggleDonorFields = () -> {
            donorExtra.setVisible("Donor".equals(fRole.getSelectedItem()));
            form.revalidate();
        };
        fRole.addActionListener(e -> toggleDonorFields.run());
        toggleDonorFields.run();

        btnRegister.addActionListener(e -> {
            try {
                String role = (String) fRole.getSelectedItem();
                if ("Donor".equals(role)) {
                    donorDAO.registerDonor(fUsername.getText(), new String(fPassword.getPassword()), fEmail.getText(),
                            fName.getText(), fCnic.getText(), (String) fGender.getSelectedItem(),
                            Integer.parseInt(fAge.getText()), fPhone.getText(), fAddress.getText(),
                            fDob.getText(), (String) fGroup.getSelectedItem());
                } else {
                    patientDAO.registerPatient(fUsername.getText(), new String(fPassword.getPassword()), fEmail.getText(),
                            fName.getText(), fCnic.getText(), (String) fGender.getSelectedItem(),
                            (String) fGroup.getSelectedItem(), fPhone.getText(), fAddress.getText());
                }
                JOptionPane.showMessageDialog(this, "Account created! Please sign in.");
                showLoginView();
            } catch (SQLException | NumberFormatException ex) {
                status.setText("Error: " + ex.getMessage());
            }
        });

        toLogin.addActionListener(e -> showLoginView());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        return scroll;
    }

    // ================= small helpers =================
    private JLabel labeled(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }

    private void widen(JComponent c) {
        c.setMaximumSize(new Dimension(260, 30));
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private JButton redButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBackground(new Color(160, 20, 30));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setMaximumSize(new Dimension(260, 34));
        return b;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(new Color(14, 59, 60));
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return b;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AuthFrame().setVisible(true));
    }
}
