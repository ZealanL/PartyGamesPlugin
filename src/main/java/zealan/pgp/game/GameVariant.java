package zealan.pgp.game;

import org.bukkit.Material;

public record GameVariant(String id, String name, String desc, Material iconItem) {
    public static GameVariant NONE = new GameVariant("normal", "Normal", "(No Variant)", Material.BARRIER);
}
