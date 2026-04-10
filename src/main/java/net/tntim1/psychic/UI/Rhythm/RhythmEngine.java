package net.tntim1.psychic.UI.Rhythm;

import java.util.ArrayList;
import java.util.List;

public class RhythmEngine {
    private final List<RhythmNote> notes = new ArrayList<>();
    private long startTime;
    private int earnedPoints = 0;
    private int totalPossiblePoints = 0;

    public void start(List<RhythmNote> spellNotes) {
        this.notes.addAll(spellNotes);
        this.startTime = System.currentTimeMillis();
    }

    public void checkInput(int lane) {
        long currentTime = System.currentTimeMillis() - startTime;
        for (RhythmNote note : notes) {
            if (note.lane == lane && !note.hit && !note.missed) {
                long diff = Math.abs(currentTime - note.targetTime);
                if (diff < 250) { // 250ms "hit" window
                    note.hit = true;
                    earnedPoints += (250 - diff);
                    totalPossiblePoints += 250;
                    return;
                }
            }
        }
        totalPossiblePoints += 50; // Penalty for clicking air
    }

    public float getAccuracy() {
        if (totalPossiblePoints == 0) return 100f;
        return ((float) earnedPoints / totalPossiblePoints) * 100f;
    }

    public List<RhythmNote> getNotes() { return notes; }
    public long getElapsedTime() { return System.currentTimeMillis() - startTime; }
}