package ru.ifmo.ctddev.itegulov.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * @author Daniyar Itegulov
 */
public class UIFileCopyFrame extends JFrame {
    JProgressBar progressBar;
    JLabel timeSpentLabel;
    JLabel timeLeftLabel;
    JLabel averageSpeedLabel;
    JLabel currentSpeedLabel;

    public UIFileCopyFrame() throws HeadlessException {
        super("File copy");

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel infoPanel = new JPanel(new GridLayout(0, 2));
        JPanel progressPanel = new JPanel(new BorderLayout());

        mainPanel.add(infoPanel);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        infoPanel.add(new ResizeableTextLabel("Time  elapsed: "));
        infoPanel.add(timeSpentLabel = new ResizeableTextLabel("00:00:01"));
        infoPanel.add(new ResizeableTextLabel("Time remained: "));
        infoPanel.add(timeLeftLabel = new ResizeableTextLabel("00:00:15"));
        infoPanel.add(new ResizeableTextLabel("Average speed: "));
        infoPanel.add(averageSpeedLabel = new ResizeableTextLabel("534 mB/s"));
        infoPanel.add(new ResizeableTextLabel("Current speed: "));
        infoPanel.add(currentSpeedLabel = new ResizeableTextLabel("940 mB/s"));

        progressPanel.add(progressBar = new JProgressBar());
        JButton cancelButton;
        progressPanel.add(cancelButton = new JButton("Cancel"), BorderLayout.EAST);

        this.getContentPane().add(mainPanel);

        cancelButton.addActionListener(e -> this.processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
    }
}
