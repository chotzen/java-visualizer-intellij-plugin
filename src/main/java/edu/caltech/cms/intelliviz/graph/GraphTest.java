package edu.caltech.cms.intelliviz.graph;

import javax.swing.*;

public class GraphTest extends JFrame {

    public GraphTest() {
        super();
        this.setSize(600, 400);
        this.add(new GraphCanvas());
        this.setVisible(true);
    }


    public static void main(String[] args) {
        new GraphTest();
    }
}

