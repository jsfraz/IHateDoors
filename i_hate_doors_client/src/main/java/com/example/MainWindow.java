package com.example;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.eclipse.paho.client.mqttv3.MqttException;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;

public class MainWindow extends JFrame {
    private BufferedImage icon; // icon

    private JPanel panel1;
    private JLabel ipLabel;
    private JTextField ipField;
    private JButton ipOkButton;
    private JButton findButton;
    private JButton testButton;
    // panel2
    private JPanel panel2;
    private JRadioButton muteMicRadio;
    private JButton muteMicBindButton;
    private JRadioButton muteSoundRadio;
    private JButton muteSoundBindButton;
    // panel3
    private JPanel panel3;
    private JCheckBox toggleBindCheckBox;
    private JButton toggleBindButton;
    private JCheckBox playToggleSoundCheckBox;
    private JCheckBox unpredictableModeCheckBox;
    // panel4
    private JPanel panel4;
    private JButton okButton;
    private JButton onOffButton;
    private JButton exitButton;

    private String oldIp;
    private JDialog searchDialog;
    private DiscoverThread discoverThread;

    private boolean bindingMuteMic = false;
    private boolean bindingMuteSound = false;
    private boolean bindingToggle = false;

    private MqttThread mqttThread;
    private GlobalKeyListener globalKeyListener;

    private final String commandMqttTopic = "sensor/commands";
    private final String valuesMqttTopic = "sensor/values";

    // regex pattern:
    // https://mkyong.com/regular-expressions/how-to-validate-ip-address-with-regular-expression/
    public final Pattern ipv4Pattern = Pattern
            .compile("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$");

    // constructor
    public MainWindow() {
        /*
         * Swing layouts:
         * https://docs.oracle.com/javase/tutorial/uiswing/layout/visual.html
         */

        // basic window setup
        super("I hate doors");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setResizable(false);
        setLayout(new GridBagLayout());

        // components

        // TODO align to center
        // panel1 (sensor IP)
        GridBagConstraints tableConstraints1 = new GridBagConstraints();
        tableConstraints1.fill = GridBagConstraints.HORIZONTAL;
        tableConstraints1.gridx = 0;
        tableConstraints1.gridy = 0;
        tableConstraints1.gridwidth = 2;
        // initializing and adding components
        panel1 = new JPanel(new FlowLayout());
        add(panel1, tableConstraints1);
        ipLabel = new JLabel("Sensor IP:");
        panel1.add(ipLabel);
        ipField = new JTextField(10);
        ipField.setHorizontalAlignment(SwingConstants.CENTER);
        ipField.setText(SettingsSingleton.GetInstance().getIp());
        ipField.setCaretPosition(ipField.getText().length());
        ipField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent arg0) {
                // nothing
            }

            @Override
            public void keyReleased(KeyEvent arg0) {
                if (SettingsSingleton.GetInstance().getIp().equals(ipField.getText())) {
                    ipOkButton.setEnabled(false);
                } else {
                    if (ipOkButton.isEnabled() == false) {
                        ipOkButton.setEnabled(true);
                    }
                }

                Matcher matcher = ipv4Pattern.matcher(ipField.getText());
                if (matcher.matches())
                    testButton.setEnabled(true);
                else
                    testButton.setEnabled(false);
            }

            @Override
            public void keyTyped(KeyEvent arg0) {
                // nothing as well
            }
        });
        panel1.add(ipField);
        ipOkButton = new JButton("OK");
        ipOkButton.setEnabled(false);
        ipOkButton.addActionListener(e -> handleIpOkButton(e));
        panel1.add(ipOkButton);
        findButton = new JButton("Find");
        findButton.addActionListener(e -> handleFindButton(e));
        panel1.add(findButton);
        testButton = new JButton("Test");
        testButton.addActionListener(e -> handleTestButton(e));
        Matcher matcher = ipv4Pattern.matcher(SettingsSingleton.GetInstance().getIp());
        if (matcher.matches() == false)
            testButton.setEnabled(false);
        panel1.add(testButton);

        // panel2 (action)
        tableConstraints1.gridx = 0;
        tableConstraints1.gridy = 1;
        tableConstraints1.gridwidth = 1;
        // initializing and adding components
        panel2 = new JPanel(new GridBagLayout());
        panel2.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Mute option"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        add(panel2, tableConstraints1);
        // panel grid
        GridBagConstraints tableConstraints2 = new GridBagConstraints();
        tableConstraints2.fill = GridBagConstraints.HORIZONTAL;
        muteMicRadio = new JRadioButton("Microphone");
        muteMicRadio.addActionListener(e -> handleMuteRadio(e));
        tableConstraints2.gridx = 0;
        tableConstraints2.gridy = 0;
        panel2.add(muteMicRadio, tableConstraints2);
        muteMicBindButton = new JButton("###");
        muteMicBindButton.addActionListener(e -> handleMuteMicBindButton(e));
        tableConstraints2.gridx = 1;
        tableConstraints2.gridy = 0;
        panel2.add(muteMicBindButton, tableConstraints2);
        muteSoundRadio = new JRadioButton("Sound");
        muteSoundRadio.addActionListener(e -> handleMuteRadio(e));
        tableConstraints2.gridx = 0;
        tableConstraints2.gridy = 1;
        panel2.add(muteSoundRadio, tableConstraints2);
        muteSoundBindButton = new JButton("###");
        muteSoundBindButton.addActionListener(e -> handleMuteSoundBindButton(e));
        tableConstraints2.gridx = 1;
        tableConstraints2.gridy = 1;
        panel2.add(muteSoundBindButton, tableConstraints2);

        // panel3 (settings)
        tableConstraints1.gridx = 1;
        tableConstraints1.gridy = 1;
        tableConstraints1.gridwidth = 2;
        // initializing and adding components
        panel3 = new JPanel(new GridBagLayout());
        panel3.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Settings"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        add(panel3, tableConstraints1);
        GridBagConstraints tableConstraints3 = new GridBagConstraints();
        tableConstraints3.fill = GridBagConstraints.HORIZONTAL;
        toggleBindCheckBox = new JCheckBox("Toggle key");
        toggleBindCheckBox.setSelected(SettingsSingleton.GetInstance().getToggleButton());
        toggleBindCheckBox.addActionListener(e -> handleToggleKeyCheckBox(e));
        tableConstraints3.gridx = 0;
        tableConstraints3.gridy = 0;
        panel3.add(toggleBindCheckBox, tableConstraints3);
        toggleBindButton = new JButton("###");
        toggleBindButton.addActionListener(e -> handleToggleBindButton(e));
        tableConstraints3.gridx = 1;
        tableConstraints3.gridy = 0;
        panel3.add(toggleBindButton, tableConstraints3);
        playToggleSoundCheckBox = new JCheckBox("Play toggle sound");
        playToggleSoundCheckBox.setSelected(SettingsSingleton.GetInstance().getPlayToggleSound());
        playToggleSoundCheckBox.addActionListener(e -> handlePlayToggleSoundCheckBox(e));
        tableConstraints3.gridx = 0;
        tableConstraints3.gridy = 1;
        tableConstraints3.gridwidth = 2;
        panel3.add(playToggleSoundCheckBox, tableConstraints3);
        if (!SettingsSingleton.GetInstance().getToggleButton()) {
            toggleBindButton.setEnabled(false);
            playToggleSoundCheckBox.setEnabled(false);
        }
        unpredictableModeCheckBox = new JCheckBox("Unpredictable mode");
        unpredictableModeCheckBox.setSelected(SettingsSingleton.GetInstance().getUnpredictableMode());
        unpredictableModeCheckBox.addActionListener(e -> handleUnpredictableModeCheckBox(e));
        tableConstraints3.gridx = 0;
        tableConstraints3.gridy = 2;
        tableConstraints3.gridwidth = 2;
        panel3.add(unpredictableModeCheckBox, tableConstraints3);

        // TODO align to center
        // panel4 (ok and exit buttons)
        tableConstraints1.gridx = 0;
        tableConstraints1.gridy = 3;
        // initializing and adding components
        panel4 = new JPanel(new FlowLayout());
        add(panel4, tableConstraints1);
        okButton = new JButton("OK");
        okButton.addActionListener(e -> handleOkButton(e));
        panel4.add(okButton);
        onOffButton = new JButton("   ");
        onOffButton.setEnabled(false);
        onOffButton.addActionListener(e -> handleOnOffButton(e));
        panel4.add(onOffButton);
        exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> handleExitButton(e));
        panel4.add(exitButton);

        // tray icon: https://github.com/evandromurilo/system_tray_example
        try {
            icon = ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("door.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                super.windowClosing(windowEvent);
                minimizeToTray();
            }
        });

        // program icon
        if (icon != null)
            setIconImage(icon);

        setMuteRadioValues();
        setButtonTexts();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        requestFocus();

        if (matcher.matches()) {
            mqttThread = new MqttThread(SettingsSingleton.GetInstance().getIp(), valuesMqttTopic, this);
            mqttThread.start();
        } else {
            setOnOffButtonStatus(Status.off);
        }

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }

        globalKeyListener = new GlobalKeyListener(this);
        GlobalScreen.addNativeKeyListener(globalKeyListener);
        globalKeyListener.setEnabled(false);
    }

    // minimalizes app on tray
    private void minimizeToTray() {
        System.out.println("Adding icon to tray...");
        if (!SystemTray.isSupported()) {
            System.out.println("Tray icon not supported.");
        }

        PopupMenu popup = new PopupMenu();
        // tray icon was distorted:
        // https://stackoverflow.com/questions/12287137/system-tray-icon-looks-distorted
        final TrayIcon trayIcon = new TrayIcon(
                icon.getScaledInstance(new TrayIcon(icon).getSize().width, -1, Image.SCALE_SMOOTH));
        final SystemTray tray = SystemTray.getSystemTray();

        MenuItem openItem = new MenuItem("Settings");
        MenuItem closeItem = new MenuItem("Exit");

        popup.add(openItem);
        popup.add(closeItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
            setVisible(false);
            System.out.println("Tray icon added.");
        } catch (AWTException e) {
            System.out.println("Unable to add tray icon.");
            e.printStackTrace();
        }

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (actionEvent.getActionCommand() != null && actionEvent.getActionCommand().equals("Exit")) {
                    System.out.println("Exiting...");
                    System.exit(0);
                }
                System.out.println("Opening from tray...");
                globalKeyListener.setEnabled(false);
                setVisible(true);
                tray.remove(trayIcon);
            }
        };

        popup.addActionListener(listener);
        trayIcon.addActionListener(listener);
        globalKeyListener.setEnabled(true);
    }

    // ipOk button
    private void handleIpOkButton(ActionEvent event) {
        Matcher matcher = ipv4Pattern.matcher(ipField.getText());
        if (matcher.matches()) {
            SettingsSingleton.GetInstance().setIp(ipField.getText());
            try {
                SettingsSingleton.GetInstance().saveSettings();
                if (mqttThread != null) {
                    if (mqttThread.isAlive())
                        mqttThread.stopRunning();
                }
                JOptionPane.showMessageDialog(null, "IP adress successfully set.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                ipOkButton.setEnabled(false);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "IP adress is not valid.", "Error", JOptionPane.ERROR_MESSAGE);
            ipField.setText(SettingsSingleton.GetInstance().getIp());
            ipOkButton.setEnabled(false);
        }
    }

    // find button
    private void handleFindButton(ActionEvent event) {
        discoverThread = new DiscoverThread(this);
        oldIp = SettingsSingleton.GetInstance().getIp();

        searchDialog = new JDialog(this, "Searching");
        searchDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                super.windowClosing(windowEvent);
                searchEndMessage();
            }

            @Override
            public void windowOpened(WindowEvent windowEvent) {
                discoverThread.start();
            }
        });
        searchDialog.setModal(true);
        searchDialog.setResizable(false);
        searchDialog.add(getSearchDialgoPanel());
        searchDialog.setLocationRelativeTo(this);
        searchDialog.pack();
        searchDialog.setVisible(true);
    }

    // panel for search dialog
    private JPanel getSearchDialgoPanel() {
        JPanel dialogPanel = new JPanel();
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(dialogPanel);
        JLabel loadingLabel = new JLabel(" Searching... ", JLabel.CENTER);
        loadingLabel.setSize(50, 50);
        dialogPanel.add(loadingLabel);
        return dialogPanel;
    }

    public void searchEndMessage() {
        discoverThread.stopRunning();
        searchDialog.dispose();
        DiscoverData data = discoverThread.getDiscoverData();
        if (data != null) {
            String message = "";
            String title = "";
            int messageType = 0;
            if (oldIp.equals(data.ip)) {
                message = "IP address wasn't changed.";
                title = "Warning";
                messageType = JOptionPane.WARNING_MESSAGE;
            } else {
                System.out.println("Adding " + data.hostname + " (" + data.ip + ")...");
                SettingsSingleton.GetInstance().setIp(data.ip);
                try {
                    SettingsSingleton.GetInstance().saveSettings();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mqttThread != null) {
                    if (mqttThread.isAlive())
                        mqttThread.stopRunning();
                }
                message = "Found!";
                title = "Info";
                messageType = JOptionPane.INFORMATION_MESSAGE;
                ipField.setText(data.ip);
                ipField.setCaretPosition(ipField.getText().length());
            }
            sendBroadcastEndMessage(data.ip);
            JOptionPane.showMessageDialog(null, message, title, messageType);
            testButton.setEnabled(true);
        }
    }

    // send MQTT message for end of discovery broadcasting
    private void sendBroadcastEndMessage(String ip) {
        try {
            new Mqtt(ip, commandMqttTopic)
                    .publish(Tools.objectToJson(new Message(MessageType.stopBroadcast)));
        } catch (MqttException e) {
            JOptionPane.showMessageDialog(null,
                    "Device was found but connecting to MQTT broker failed. Verify that service is running or check your configuration.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // test button
    private void handleTestButton(ActionEvent event) {
        testButton.setEnabled(false);
        boolean success = new Mqtt(ipField.getText(), "sensor/commands").testConnection();
        String title;
        String message;
        int icon;
        if (success) {
            title = "Info";
            message = "Success!";
            icon = JOptionPane.INFORMATION_MESSAGE;
        } else {
            title = "Error";
            message = "Unable to connect to MQTT broker. Verify that service is running or check your configuration.";
            icon = JOptionPane.ERROR_MESSAGE;
        }
        JOptionPane.showMessageDialog(null, message, title, icon);
        testButton.setEnabled(true);
    }

    // setting mute radio button values
    private void setMuteRadioValues() {
        MuteOption option = SettingsSingleton.GetInstance().getMuteOption();
        if (option == MuteOption.microphone) {
            muteMicRadio.setSelected(true);
        } else {
            muteMicRadio.setSelected(false);
        }
        if (option == MuteOption.sound) {
            muteSoundRadio.setSelected(true);
        } else {
            muteSoundRadio.setSelected(false);
        }
    }

    // mute button text values
    private void setButtonTexts() {
        muteMicBindButton.setText(KeyEvent.getKeyText(SettingsSingleton.GetInstance().getMuteMicKey()));
        muteSoundBindButton.setText(KeyEvent.getKeyText(SettingsSingleton.GetInstance().getMuteSoundKey()));
        toggleBindButton.setText(KeyEvent.getKeyText(SettingsSingleton.GetInstance().getToggleKey()));
        pack();
    }

    // mute radio
    private void handleMuteRadio(ActionEvent event) {
        String command = event.getActionCommand();
        if (command == "Microphone")
            SettingsSingleton.GetInstance().setMuteOption(MuteOption.microphone);
        if (command == "Sound")
            SettingsSingleton.GetInstance().setMuteOption(MuteOption.sound);
        try {
            SettingsSingleton.GetInstance().saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setMuteRadioValues();
    }

    // mute mic bind button
    private void handleMuteMicBindButton(ActionEvent event) {
        if (bindingMuteMic == false) {
            bindingMuteMic = true;
            muteMicBindButton.setText("   ");
            pack();
            Color originalColor = muteMicBindButton.getBackground();
            muteMicBindButton.setBackground(Color.LIGHT_GRAY);
            muteMicBindButton.addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(KeyEvent arg0) {
                    // nothing
                }

                @Override
                public void keyReleased(KeyEvent arg0) {
                    int keyCode = arg0.getKeyCode();
                    if (keyCode != KeyEvent.VK_ESCAPE) {
                        if (keyCode != SettingsSingleton.GetInstance().getMuteSoundKey()
                                && keyCode != SettingsSingleton.GetInstance().getToggleKey()) {
                            SettingsSingleton.GetInstance().setMuteMicKey(keyCode);
                            try {
                                SettingsSingleton.GetInstance().saveSettings();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else
                            JOptionPane.showMessageDialog(null, "Key is already used!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                    }
                    muteMicBindButton.setBackground(originalColor);
                    setButtonTexts();
                    muteMicBindButton.removeKeyListener(this);
                    FocusListener[] focusListeners = muteMicBindButton.getFocusListeners();
                    if (focusListeners.length == 1)
                        muteMicBindButton.removeFocusListener(focusListeners[0]);
                    bindingMuteMic = false;
                }

                @Override
                public void keyTyped(KeyEvent arg0) {
                    // nothing as well
                }

            });
            muteMicBindButton.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent arg0) {
                    // nothing
                }

                @Override
                public void focusLost(FocusEvent arg0) {
                    bindingMuteMic = false;
                    muteMicBindButton.setBackground(originalColor);
                    setButtonTexts();
                    muteMicBindButton.removeFocusListener(this);
                    KeyListener[] keyListeners = muteMicBindButton.getKeyListeners();
                    if (keyListeners.length == 1)
                        muteMicBindButton.removeKeyListener(keyListeners[0]);
                }
            });
        }
    }

    // mute sound bind button
    private void handleMuteSoundBindButton(ActionEvent event) {
        if (bindingMuteSound == false) {
            bindingMuteSound = true;
            muteSoundBindButton.setText("   ");
            pack();
            Color originalColor = muteSoundBindButton.getBackground();
            muteSoundBindButton.setBackground(Color.LIGHT_GRAY);
            muteSoundBindButton.addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(KeyEvent arg0) {
                    // nothing
                }

                @Override
                public void keyReleased(KeyEvent arg0) {
                    int keyCode = arg0.getKeyCode();
                    if (keyCode != KeyEvent.VK_ESCAPE) {
                        if (keyCode != SettingsSingleton.GetInstance().getMuteMicKey()
                                && keyCode != SettingsSingleton.GetInstance().getToggleKey()) {
                            SettingsSingleton.GetInstance().setMuteSoundKey(keyCode);
                            try {
                                SettingsSingleton.GetInstance().saveSettings();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else
                            JOptionPane.showMessageDialog(null, "Key is already used!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                    }
                    muteSoundBindButton.setBackground(originalColor);
                    setButtonTexts();
                    muteSoundBindButton.removeKeyListener(this);
                    FocusListener[] focusListeners = muteSoundBindButton.getFocusListeners();
                    if (focusListeners.length == 1)
                        muteSoundBindButton.removeFocusListener(focusListeners[0]);
                    bindingMuteSound = false;
                }

                @Override
                public void keyTyped(KeyEvent arg0) {
                    // nothing as well
                }

            });
            muteSoundBindButton.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent arg0) {
                    // nothing
                }

                @Override
                public void focusLost(FocusEvent arg0) {
                    bindingMuteSound = false;
                    muteSoundBindButton.setBackground(originalColor);
                    setButtonTexts();
                    muteSoundBindButton.removeFocusListener(this);
                    KeyListener[] keyListeners = muteSoundBindButton.getKeyListeners();
                    if (keyListeners.length == 1)
                        muteSoundBindButton.removeKeyListener(keyListeners[0]);
                }
            });
        }
    }

    // toggle key radio
    private void handleToggleKeyCheckBox(ActionEvent event) {
        if (toggleBindCheckBox.isSelected()) {
            SettingsSingleton.GetInstance().setToggleButton(true);
            toggleBindButton.setEnabled(true);
        } else {
            SettingsSingleton.GetInstance().setToggleButton(false);
            toggleBindButton.setEnabled(false);
        }
        try {
            SettingsSingleton.GetInstance().saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // toggle bind button
    private void handleToggleBindButton(ActionEvent event) {
        if (bindingToggle == false) {
            bindingToggle = true;
            toggleBindButton.setText("   ");
            pack();
            Color originalColor = toggleBindButton.getBackground();
            toggleBindButton.setBackground(Color.LIGHT_GRAY);
            toggleBindButton.addKeyListener(new KeyListener() {

                @Override
                public void keyPressed(KeyEvent arg0) {
                    // nothing
                }

                @Override
                public void keyReleased(KeyEvent arg0) {
                    int keyCode = arg0.getKeyCode();
                    if (keyCode != KeyEvent.VK_ESCAPE) {
                        if (keyCode != SettingsSingleton.GetInstance().getMuteMicKey()
                                && keyCode != SettingsSingleton.GetInstance().getMuteSoundKey()) {
                            SettingsSingleton.GetInstance().setToggleKey(keyCode);
                            try {
                                SettingsSingleton.GetInstance().saveSettings();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else
                            JOptionPane.showMessageDialog(null, "Key is already used!", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                    }
                    toggleBindButton.setBackground(originalColor);
                    setButtonTexts();
                    toggleBindButton.removeKeyListener(this);
                    FocusListener[] focusListeners = toggleBindButton.getFocusListeners();
                    if (focusListeners.length == 1)
                        toggleBindButton.removeFocusListener(focusListeners[0]);
                    bindingToggle = false;
                }

                @Override
                public void keyTyped(KeyEvent arg0) {
                    // nothing as well
                }

            });
            toggleBindButton.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent arg0) {
                    // nothing
                }

                @Override
                public void focusLost(FocusEvent arg0) {
                    toggleBindButton.setBackground(originalColor);
                    setButtonTexts();
                    toggleBindButton.removeFocusListener(this);
                    KeyListener[] keyListeners = toggleBindButton.getKeyListeners();
                    if (keyListeners.length == 1)
                        toggleBindButton.removeKeyListener(keyListeners[0]);
                    bindingToggle = false;
                }
            });
        }
    }

    private void handlePlayToggleSoundCheckBox(ActionEvent event) {
        if (playToggleSoundCheckBox.isSelected())
            SettingsSingleton.GetInstance().setPlayToggleSound(true);
        else
            SettingsSingleton.GetInstance().setPlayToggleSound(false);
        try {
            SettingsSingleton.GetInstance().saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUnpredictableModeCheckBox(ActionEvent event) {
        if (unpredictableModeCheckBox.isSelected())
            SettingsSingleton.GetInstance().setUnpredictableMode(true);
        else
            SettingsSingleton.GetInstance().setUnpredictableMode(false);
        try {
            SettingsSingleton.GetInstance().saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ok button
    private void handleOkButton(ActionEvent event) {
        minimizeToTray();
    }

    // on/off button (start/stop mqtt thread)
    private void handleOnOffButton(ActionEvent event) {
        onOffButton.setEnabled(false);
        if (mqttThread != null) {
            if (mqttThread.isAlive()) {
                mqttThread.stopRunning();
            } else {
                mqttThread = new MqttThread(SettingsSingleton.GetInstance().getIp(), valuesMqttTopic, this);
                mqttThread.start();
            }
        } else {
            Matcher matcher = ipv4Pattern.matcher(SettingsSingleton.GetInstance().getIp());
            if (matcher.matches()) {
                mqttThread = new MqttThread(SettingsSingleton.GetInstance().getIp(), valuesMqttTopic, this);
                mqttThread.start();
            } else {
                onOffButton.setEnabled(true);
            }
        }
    }

    public boolean isMqttThreadRunning() {
        return mqttThread.isAlive();
    }

    public void startMqttThread() {
        onOffButton.setEnabled(false);
        mqttThread = new MqttThread(SettingsSingleton.GetInstance().getIp(), valuesMqttTopic, this);
        mqttThread.start();
        if (SettingsSingleton.GetInstance().getPlayToggleSound())
            Tools.playSound(Sound.enable);
    }

    public void stopMqttThread() {
        onOffButton.setEnabled(false);
        mqttThread.stopRunning();
        if (SettingsSingleton.GetInstance().getPlayToggleSound())
            Tools.playSound(Sound.disable);
    }

    public boolean isMqttThreadNull() {
        return mqttThread == null;
    }

    public void setOnOffButtonStatus(Status status) {
        if (status == Status.on) {
            onOffButton.setForeground(Color.green);
            onOffButton.setText("ON");
        } else {
            onOffButton.setForeground(Color.red);
            onOffButton.setText("OFF");
        }
        onOffButton.setEnabled(true);
    }

    // exit button
    private void handleExitButton(ActionEvent event) {
        System.out.println("Exiting...");
        System.exit(0);
    }
}