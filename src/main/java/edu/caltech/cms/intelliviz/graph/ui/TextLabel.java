package edu.caltech.cms.intelliviz.graph.ui;

import java.awt.*;

public class TextLabel {

    private String label;
    private static Font labelFont = new Font("SanSerif", Font.PLAIN, 10);

    public TextLabel(String label) {
        this.label = label;
    }

    public void draw(Graphics2D g, double x, double y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setFont(labelFont);
        FontMetrics fm = g.getFontMetrics();
        double fontWidth = fm.stringWidth(label);
        double fontHeight = fm.getAscent();
        int newX = (int)(x - fontWidth / 2);
        int newY = (int)(y + fontHeight / 3);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(newX - 1, newY - (int)fontHeight + 2, (int)fontWidth, (int)fontHeight + 2);
        g2d.setColor(Color.BLACK);
        g2d.drawString(this.label, newX, newY - 1);
    }

    public String toString() {
        return label;
    }
}
