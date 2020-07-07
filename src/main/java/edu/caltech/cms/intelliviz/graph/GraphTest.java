package edu.caltech.cms.intelliviz.graph;

import javax.swing.*;

public class GraphTest extends JFrame {

    public GraphTest() {
        super();
        this.setSize(600, 400);
        GraphCanvas gc = new GraphCanvas();
        this.add(gc);

        this.setVisible(true);
    }


    public static void main(String[] args) {
        new GraphTest();
    }
}

