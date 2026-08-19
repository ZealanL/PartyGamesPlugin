package zealan.pgp.game.games;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.PlayerUtil;

import java.util.HashMap;
import java.util.HashSet;

public class GamePunchTheBats extends Game {
    public GamePunchTheBats(InitParams params) {
        super(params);
    }

    public static final Vec3i CENTER_SPAWN_POS = new Vec3i(1902, 73, -1872);

    // In reality its 5 per player, but having only 5 bats when soloing is very boring
    private static final int TOTAL_BATS = 40;
    private static final Vector BAT_SPAWN_CENTER = new Vector(1902.5, 81.0, -1871.5);
    private static final Vector BAT_SPAWN_HALF_AREA = new Vector(4.0, 1.0, 4.0);
    private static final double BONUS_BAT_CHANCE = 0.1;
    private static final int BAT_RESPAWN_INTERVAL = 20 * 10;

    private static final int DOUBLE_JUMP_COOLDOWN_TICKS = 40;

    // //////

    private final HashMap<Gamer, Integer> doubleJumpCooldowns = new HashMap<>();

    private final HashSet<Bat> bats = new HashSet<>();

    private void spawnBat() {
        boolean isBonus = rand.nextDouble() < BONUS_BAT_CHANCE;

        var spawnOffsetScale = new Vector(
                rand.nextDouble() * 2 - 1,
                rand.nextDouble() * 2 - 1,
                rand.nextDouble() * 2 - 1
        );

        var spawnPos = BAT_SPAWN_CENTER.clone().add(BAT_SPAWN_HALF_AREA.clone().multiply(spawnOffsetScale));

        Bat bat = (Bat)world.spawnEntity(
                new Location(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()),
                EntityType.BAT
        );

        String customName = Display.format(isBonus ? "&6Bonus Bat!" : "&aPunch Me!");
        bat.setCustomNameVisible(true);
        bat.setCustomName(customName);

        bats.add(bat);
    }

    @Override
    protected void innerOnStart() {
        for (var gamer : getGamers())
            doubleJumpCooldowns.put(gamer, 0);
    }

    @Override
    public void spawnGamer(Gamer gamer) {
        super.spawnGamer(gamer);
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        for (var gamer : getGamers()) {
            var doubleJumpCooldown = doubleJumpCooldowns.get(gamer);
            if (doubleJumpCooldown == null)
                continue;

            if (doubleJumpCooldown > 0)
                doubleJumpCooldowns.replace(gamer, doubleJumpCooldown - 1);

            double doubleJumpChargeFrac = (DOUBLE_JUMP_COOLDOWN_TICKS - doubleJumpCooldown) / (double)DOUBLE_JUMP_COOLDOWN_TICKS;
            gamer.player.setExp((float)doubleJumpChargeFrac);
        }

        // Update double jump networking
        for (var gamer : getGamers()) {
            var doubleJumpCooldown = doubleJumpCooldowns.getOrDefault(gamer, 0);
            boolean canDoubleJump = doubleJumpCooldown == 0;
            gamer.player.setFlySpeed(5e-5f);
            gamer.player.setAllowFlight(canDoubleJump);
            PlayerUtil.sendAbilities(gamer.player);
        }

        bats.removeIf(Entity::isDead);

        if (getTicksElapsed() % BAT_RESPAWN_INTERVAL == 0) {
            while (bats.size() < TOTAL_BATS) {
                spawnBat();
            }
        }

        for (var bat : bats)
            bat.setAwake(true);
    }

    @Override
    protected void innerOnEnd() {

    }

    @Override
    public boolean canAttackEntity(Player player, Entity entity, EntityDamageEvent event) {
        if (entity instanceof Bat bat) {
            var gamer = getGamer(player);
            if (gamer == null)
                return false;

            event.setDamage(999);

            boolean isBonus = bat.getCustomName().contains("Bonus");
            if (isBonus) {
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7f, 1.0f);
                gamer.points += 4;
            } else {
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.7f, 1.0f);
                gamer.points += 1;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean canStartFlying(Player player) {

        var gamer = getGamer(player);
        if (gamer != null) {
            var cooldown = doubleJumpCooldowns.getOrDefault(gamer, 0);
            if (cooldown <= 0) {
                var vel = player.getVelocity();
                player.setVelocity(vel.setY(1.25));
                doubleJumpCooldowns.put(gamer, DOUBLE_JUMP_COOLDOWN_TICKS);
            }
        }

        return false;
    }
}
