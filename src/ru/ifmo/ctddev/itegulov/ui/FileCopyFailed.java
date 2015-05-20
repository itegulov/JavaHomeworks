package ru.ifmo.ctddev.itegulov.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FileCopyFailed extends JDialog {
    private JPanel contentPane;
    private JButton buttonRetry;
    private JButton buttonMiss;
    private JLabel textLabel;
    private JButton buttonAbort;
    private FileCopyResult result;

    public enum FileCopyResult {
        RETRY, MISS, ABORT
    }

    public FileCopyFailed(Frame owner, File file) {
        super(owner, "File copy failed", true);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonRetry);

        textLabel.setText("File " + file + " couldn't be copied");

        buttonRetry.addActionListener(e -> {
            result = FileCopyResult.RETRY;
            dispose();
        });

        buttonMiss.addActionListener(e -> {
            result = FileCopyResult.MISS;
            dispose();
        });

        buttonAbort.addActionListener(e -> {
            result = FileCopyResult.ABORT;
            dispose();
        });
    }

    public FileCopyResult getResult() {
        return result;
    }
}
