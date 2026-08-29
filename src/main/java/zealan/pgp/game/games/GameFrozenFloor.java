package zealan.pgp.game.games;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import zealan.pgp.api.display.Display;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;

import java.util.HashMap;

public class GameFrozenFloor extends Game {
    public static final GameVariant VARIANT_NO_COOLDOWN = new GameVariant(
            "no_cooldown", "No Cooldown", "Removes cooldown on snowball throwing",
            Material.WATCH
    );

    private static final int THROW_COOLDOWN_TICKS = 5;
    private static final int VOID_Y = 0;

    private final HashMap<Gamer, Integer> lastThrowTicks = new HashMap<>();

    public GameFrozenFloor(InitParams params) {
        super(params);
    }

    @Override
    protected void innerOnStart() {

    }

    @Override
    protected void innerOnTick() {
        for (var gamer : getPlayingGamers()) {
            for (int i = 0; i < 9; i++)
                gamer.player.getInventory().setItem(i, new ItemStack(Material.SNOW_BALL, 64));

            if (gamer.player.getLocation().getY() <= VOID_Y) {
                Display.sendMsg(world, "{}&f fell into the void.", gamer.player);
                gamer.player.damage(999);
            }
        }

        if (getPlayingGamers().size() <= 1 && getGamers().size() > 1) {
            this.end(true);
        }
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    public boolean canAttackEntity(Player player, Entity entity, EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Snowball;
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        var gamer = getPlayingGamer(player);
        if (gamer == null)
            return false;

        if (item != null && item.getType() == Material.SNOW_BALL) {
            int lastThrowTick = lastThrowTicks.getOrDefault(gamer, -999);
            int timeSince = gamer.player.getTicksLived() - lastThrowTick;

            if (timeSince >= THROW_COOLDOWN_TICKS || variant == VARIANT_NO_COOLDOWN) {
                lastThrowTicks.put(gamer, gamer.player.getTicksLived());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canInteractBlock(Player player, Block block) {
        return true;
    }
}
