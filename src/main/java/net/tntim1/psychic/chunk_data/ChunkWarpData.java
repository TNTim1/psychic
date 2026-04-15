package net.tntim1.psychic.chunk_data;

public class ChunkWarpData {
    private int warpStrength;

    public ChunkWarpData() {
        this.warpStrength = 200 + (int)(Math.random() * 301);
    }

    public int getWarpStrength() {
        return warpStrength;
    }

    public void addWarp(int amount) {
        this.warpStrength += amount;
    }
}