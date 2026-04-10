package net.tntim1.psychic.UI.Rhythm;


public class RhythmNote {
    public final int lane;      // 0-7
    public final long targetTime; // Time in ms when it should be hit
    public boolean hit = false;
    public boolean missed = false;

    public RhythmNote(int lane, long targetTime) {
        this.lane = lane;
        this.targetTime = targetTime;
    }
}