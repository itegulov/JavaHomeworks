package ru.ifmo.ctddev.itegulov.ui;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Helper class, which stores generated fonts and generates new one, when it's necessary.
 * It's just factory of fonts.
 *
 * @author Daniyar Itegulov
 * @see Font
 */
public final class FontHelper {
    private static Map<String, List<Font>> fonts = new HashMap<>();

    /**
     * Returns {@link Font} which represents the same font as {@code inFont}, but with size {@code size}.
     * Doesn't create new {@code Font} if can.
     *
     * @return required font
     * @param size size of new font
     * @param inFont old font
     * @see Font#deriveFont(float)
     */
    public static Font getFontWithSize(int size, Font inFont) {
        if (fonts.containsKey(inFont.getName())) {
            for (Font font : fonts.get(inFont.getName())) {
                if ((font.getSize() == size) && (font.getStyle() == inFont.getStyle())) {
                    return font;
                }
            }
        } else {
            fonts.put(inFont.getName(), new ArrayList<>());
        }
        Font newFont = inFont.deriveFont((float) size);
        fonts.get(inFont.getName()).add(newFont);
        return newFont;
    }
}