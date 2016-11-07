package com.sungardas.enhancedsnapshots.dto;


public class Cluster {
    private int minNodeNumber;
    private int maxNodeNumber;

    public int getMinNodeNumber() {
        return minNodeNumber;
    }

    public void setMinNodeNumber(int minNodeNumber) {
        this.minNodeNumber = minNodeNumber;
    }

    public int getMaxNodeNumber() {
        return maxNodeNumber;
    }

    public void setMaxNodeNumber(int maxNodeNumber) {
        this.maxNodeNumber = maxNodeNumber;
    }
}
