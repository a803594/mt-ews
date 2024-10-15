package ru.mos.mostech.ews.ui;

import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.Settings;
import ru.mos.mostech.ews.util.IOUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


@Slf4j
public class SimpleUi {
    public static void start() throws IOException {
        // Проверяем, поддерживается ли системный трей
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported!");
            return;
        }

        Image image; // Убедитесь, что у вас есть иконка
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("logo.png")) {
            image = Toolkit.getDefaultToolkit().createImage(IOUtil.readFully(Objects.requireNonNull(is, "logo.png is required")));
        }

        // Создаем иконку системного трей
        final PopupMenu popup = new PopupMenu();
        final SystemTray tray = SystemTray.getSystemTray();

        // Создаем основной JFrame
        final JFrame frame = new JFrame("MT-EWS");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(true);
        frame.setIconImage(image);

        // Создаем вкладки
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Tahoma", Font.PLAIN, 24));

        // Вкладка 1: Информация
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel logo = new JLabel(new ImageIcon(image, "logo")); // Иконка статуса
        JLabel title = new JLabel("MT-EWS");
        title.setFont(new Font("Tahoma", Font.BOLD, 25));

        logoPanel.add(logo);
        logoPanel.add(title);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("Статус: ");
        statusLabel.setFont(new Font("Tahoma", Font.BOLD, 20));
        JLabel statusValue = new JLabel("Работает");
        statusValue.setFont(new Font("Tahoma", Font.BOLD, 20));
        statusValue.setForeground(Color.GREEN); // Зелёный цвет для статуса

        statusPanel.add(statusLabel);
        statusPanel.add(statusValue);

        infoPanel.add(logoPanel, BorderLayout.NORTH);
        infoPanel.add(statusPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Информация", infoPanel);

        // Вкладка 2: Конфигурация
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea(Settings.printAll());
        textArea.setFont(new Font("Tahoma", Font.PLAIN, 24));
        configPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        tabbedPane.addTab("Конфигурация", configPanel);

        // Вкладка 3: Журнал работы
        JPanel logPanel = new JPanel();
        logPanel.setLayout(new BorderLayout());

        JCheckBox logCheckbox = new JCheckBox("Включить расширенные логи");
        logCheckbox.setFont(new Font("Tahoma", Font.BOLD, 20));
        JButton openLogButton = new JButton("Открыть папку с логами");
        openLogButton.setFont(new Font("Tahoma", Font.BOLD, 20));

        openLogButton.addActionListener(e -> {
            try {
                File logFolder = new File("logs"); // Папка с логами
                Desktop.getDesktop().open(logFolder);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        logPanel.add(logCheckbox, BorderLayout.NORTH);
        logPanel.add(openLogButton, BorderLayout.SOUTH);
        tabbedPane.addTab("Журнал работы", logPanel);

        frame.add(tabbedPane);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);


        // Создаем иконку для системного трея
        TrayIcon trayIcon = new TrayIcon(image, "MT-EWS Адаптер", popup);
        trayIcon.setImageAutoSize(true);

        // Добавляем пункт меню в трей
        MenuItem exitItem = new MenuItem("Выход");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            System.exit(0);
        });

        popup.add(exitItem);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }

        // Действия при двойном щелчке
        trayIcon.addActionListener(e -> {
            frame.setVisible(true);
            frame.toFront();
        });
    }
}