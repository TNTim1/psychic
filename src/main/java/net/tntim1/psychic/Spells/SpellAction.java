package net.tntim1.psychic.Spells;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface SpellAction {
    void execute(ServerPlayer player);
}