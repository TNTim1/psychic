package net.tntim1.psychic.player_data;

public class ClientManaStore {
    public static float mana = 100f;
    public static float maxMana = 100f;

    public static void set(float m, float max) { mana = m; maxMana = max; }
    public static boolean isFull() { return mana >= maxMana; }
}