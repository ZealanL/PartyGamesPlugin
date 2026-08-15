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
import zealan.pgp.util.EntityUtil;
import zealan.pgp.util.PlayerUtil;

import java.util.HashMap;
import java.util.HashSet;

public class GamePunchTheBats extends Game {
    public GamePunchTheBats(InitParams params) {
        super(params);
    }

    public static final Vec3i CENTER_SPAWN_POS = new Vec3i(1902, 73, -1872);
    private static final Vector BAT_SPAWN_CENTER = new Vector(1902.5, 82.0, -1871.5);
    private static final double BAT_SPAWN_RADIUS = 3.0;

    private static final int NUM_BATS_NORMAL = 30;
    private static final int NUM_BATS_BONUS = 3;

    private static final int DOUBLE_JUMP_COOLDOWN_TICKS = 40;

    // //////

    private final HashMap<Gamer, Integer> doubleJumpCooldowns = new HashMap<>();

    private final HashSet<Bat> normalBats = new HashSet<>();
    private final HashSet<Bat> bonusBats = new HashSet<>();

    private void spawnBat(boolean isBonus) {
        Vector spawnPos = BAT_SPAWN_CENTER.clone().add(
                new Vector(
                    rand.nextDouble() * BAT_SPAWN_RADIUS - (BAT_SPAWN_RADIUS / 2),
                    rand.nextDouble() * BAT_SPAWN_RADIUS - (BAT_SPAWN_RADIUS / 2),
                    rand.nextDouble() * BAT_SPAWN_RADIUS - (BAT_SPAWN_RADIUS / 2)
                )
        );

        Bat bat = (Bat)world.spawnEntity(
                new Location(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()),
                EntityType.BAT
        );

        String customName = Display.format(isBonus ? "&6Bonus Bat!" : "&aPunch Me!");
        bat.setCustomNameVisible(true);
        bat.setCustomName(customName);

        if (isBonus) {
            bonusBats.add(bat);
        } else {
            normalBats.add(bat);
        }
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

        normalBats.removeIf(Entity::isDead);
        bonusBats.removeIf(Entity::isDead);

        if (normalBats.size() < NUM_BATS_NORMAL)
            spawnBat(false);
        if (bonusBats.size() < NUM_BATS_BONUS)
            spawnBat(true);

        for (var bat : normalBats)
            bat.setAwake(true);
        for (var bat : bonusBats)
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

            boolean isBonus = bonusBats.contains(bat);
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
