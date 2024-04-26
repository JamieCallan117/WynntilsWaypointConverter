import java.awt.datatransfer.StringSelection;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Artemis Waypoint Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLocationRelativeTo(null);

        JLabel base64Label = new JLabel("Enter Base64:");
        JTextField base64TextField = new JTextField(20);
        JButton decodeButton = new JButton("Convert to Artemis format");
        JTextArea jsonTextArea = new JTextArea();
        jsonTextArea.setEditable(false);
        JButton copyButton = new JButton("Copy JSON");

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        panel.add(base64Label);
        panel.add(base64TextField);
        panel.add(decodeButton);
        panel.add(copyButton);

        JPanel jsonPanel = new JPanel(new BorderLayout());
        jsonPanel.add(new JScrollPane(jsonTextArea), BorderLayout.CENTER);
        jsonPanel.add(copyButton, BorderLayout.SOUTH);

        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(jsonPanel, BorderLayout.CENTER);

        decodeButton.addActionListener(e -> {
            String base64Input = base64TextField.getText();
            try {
                List<WaypointProfile> decoded = WaypointProfile.decode(base64Input);

                JsonArray array = new JsonArray();
                decoded.stream().map(WaypointProfile::toArtemisObject).forEach(array::add);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String formattedJson = gson.toJson(array);

                jsonTextArea.setText(formattedJson);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, "Error decoding waypoints: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        copyButton.addActionListener(e -> {
            String jsonText = jsonTextArea.getText();
            StringSelection selection = new StringSelection(jsonText);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(frame, "JSON copied to clipboard!");
        });

        frame.setVisible(true);
    }
}
