package ru.ifmo.ctddev.itegulov.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * GUI for {@link UIFileCopy} with progress bar, average speed, current speed, elapsed time and time remaining.
 *
 * @author Daniyar Itegulov
 */
public class UIFileCopyFrame extends JFrame {
    JProgressBar progressBar;
    JLabel timeSpentLabel;
    JLabel timeLeftLabel;
    JLabel averageSpeedLabel;
    JLabel currentSpeedLabel;
    JButton cancelButton;

    public UIFileCopyFrame() throws HeadlessException {
        super("File copy");

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel infoPanel = new JPanel(new GridLayout(0, 2));
        JPanel progressPanel = new JPanel(new BorderLayout());

        mainPanel.add(infoPanel);
        mainPanel.add(progressPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        infoPanel.add(new ResizeableTextLabel("Time  elapsed: "));
        infoPanel.add(timeSpentLabel = new ResizeableTextLabel("00:00:00"));
        infoPanel.add(new ResizeableTextLabel("Time remained: "));
        infoPanel.add(timeLeftLabel = new ResizeableTextLabel("00:00:00"));
        infoPanel.add(new ResizeableTextLabel("Average speed: "));
        infoPanel.add(averageSpeedLabel = new ResizeableTextLabel("0 B/s"));
        infoPanel.add(new ResizeableTextLabel("Current speed: "));
        infoPanel.add(currentSpeedLabel = new ResizeableTextLabel("0 B/s"));

        progressPanel.add(progressBar = new JProgressBar());
        progressPanel.add(cancelButton = new JButton("Cancel"), BorderLayout.EAST);

        this.getContentPane().add(mainPanel);
    }
}
