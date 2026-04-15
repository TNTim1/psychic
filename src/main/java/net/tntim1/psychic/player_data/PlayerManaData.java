package net.tntim1.psychic.player_data;


public class PlayerManaData {
    private float mana;
    private float maxMana;

    private static final float DEFAULT_MAX = 100f;
    private static final float REGEN_PER_SECOND = 2f; // 2 mana/sec

    public PlayerManaData() {
        this.maxMana = DEFAULT_MAX;
        this.mana = DEFAULT_MAX;
    }

    public float getMana()    { return mana; }
    public float getMaxMana() { return maxMana; }
    public boolean isFull()   { return mana >= maxMana; }

    public void setMaxMana(float max) { this.maxMana = max; this.mana = Math.min(mana, max); }
    public void setMana(float mana) { this.mana = Math.max(0, Math.min(maxMana, mana)); }

    public boolean spend(float amount) {
        if (mana < amount) return false;
        mana = Math.max(0, mana - amount);
        return true;
    }

    /** Called every tick server-side. Returns true if value changed. */
    private float regenPerSecond = 2f; // now instance-level, not static

    public float getRegenPerSecond() { return regenPerSecond; }
    public void setRegenPerSecond(float rate) { this.regenPerSecond = Math.max(0, rate); }

    public boolean tick() {
        if (mana < maxMana) {
            mana = Math.min(maxMana, mana + regenPerSecond / 20f);
            return true;
        }
        return false;
    }
}