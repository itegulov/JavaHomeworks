package ru.ifmo.ctddev.itegulov.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;


/**
 * Text area, where text will dynamically adjust it's font size to fill all the area available.
 *
 * @author Daniyar Itegulov
 * @see JLabel
 */
public class ResizeableTextLabel extends JLabel {
    private Dimension preferredSize;
    private Dimension minimumSize;

    protected class ResizableAdapter extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            resize();
        }
    }

    /**
     * Creates a resizable text label with text, icon and  predefined
     * horizontal text alignment and resizable  font setting.
     *
     * @param text                text to be displayed in  the label
     * @param icon                image to be displayed in the label
     * @param horizontalAlignment horizontal alignment of the text in the label
     */
    public ResizeableTextLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        addComponentListener(new ResizableAdapter());
    }

    /**
     * Creates a resizable text label with text.
     *
     * @param text text to be displayed in  the label
     */
    public ResizeableTextLabel(String text) {
        this(text, null, JLabel.LEADING);
    }

    /**
     * Does exactly the same as {@link JLabel#setText(String)}, but also
     * resize font to adjust space available.
     *
     * @param text text to be displayed
     */
    @Override
    public void setText(String text) {
        if (text != null && text.length() < 1) {
            text = " ";
        }

        super.setText(text);

        if (getFont() != null) {
            resize();
        }
    }

    /**
     * Adjusts the size of the font to the size of the label.
     */
    protected void resize() {
        int newSize = getHeight() * 14 / 20;
        Font font = FontHelper.getFontWithSize(newSize, getFont());
        int length = getFontMetrics(font).stringWidth(getText());

        if (length <= getWidth() * 0.9 || length == 0) {
            super.setFont(font);
        } else {
            super.setFont(FontHelper.getFontWithSize(
                    (int) (newSize * getWidth() * 0.9 / length), font));
        }
    }

    /**
     * Does exactly the same as {@link JLabel#addNotify()}, but also
     * resize font to adjust space available.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        resize();
    }

    /**
     * Does exactly the same as {@link JLabel#getPreferredSize()}, but also
     * adjusts dimension to fit space available.
     *
     * @return dimension, representing preferred size of the label
     */
    @Override
    public Dimension getPreferredSize() {
        if (preferredSize != null) {
            return preferredSize;
        } else  {

            Font font = getFont();

            int height = font.getSize() * 20 / 13;
            int width = getFontMetrics(font).stringWidth(getText()) * 10 / 8;

            if (width == 0) {
                width = 1;
            }

            return new Dimension(width, height);
        }
    }

    /**
     * This method was overriden to implement font resizing.
     *
     * @see javax.swing.JComponent#setPreferredSize(Dimension)
     */
    @Override
    public void setPreferredSize(Dimension newPreferredSize) {
        preferredSize = newPreferredSize;
        super.setPreferredSize(newPreferredSize);
    }

    /**
     * Does exactly the same as {@link JLabel#getMinimumSize()}, but also
     * adjusts dimension to fit space available.
     *
     * @see java.awt.Component#getMinimumSize()
     */
    @Override
    public Dimension getMinimumSize() {
        if (minimumSize != null) {
            return minimumSize;
        } else {
            return getPreferredSize();
        }
    }

    /**
     * This method was overriden to implement font resizing.
     *
     * @see javax.swing.JComponent#setMinimumSize(Dimension)
     */
    @Override
    public void setMinimumSize(Dimension newMinimumSize) {
        minimumSize = newMinimumSize;
        super.setMinimumSize(newMinimumSize);
    }
}