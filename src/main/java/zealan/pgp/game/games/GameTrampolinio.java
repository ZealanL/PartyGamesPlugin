package zealan.pgp.game.games;

// If available in standard Bukkit/Bungee API or replace string directly

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftBat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BoundingBox;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;

import java.util.ArrayList;

public class GameTrampolinio extends Game {
    public GameTrampolinio(InitParams params) {
        super(params);
    }

    public static final Vec3i CENTER_BLOCK_POS = new Vec3i(2581, 38, -1872);
    private static final int BOUNCE_FLOOR_Y = 37;
    private static final double BOUNCE_VEL_Y = 1.25;
    private static final double BOUNCE_VEL_XZ = 0.325; // TODO: Guess
    private static final double COIN_SPAWN_RADIUS = 19.0; // TODO: Guess
    private static final double ARMOR_STAND_OFFSET_Y = -2.0;
    private static final double BOOST_ADD_VEL_Y = 3.0;  // TODO: Guess
    private static final int END_GAME_POINTS = 40;

    private enum CoinType {
        GREEN(42.0, 1, 10, Material.WOOL, (byte) 5, "&a+1"),
        YELLOW(45.0, 3, 6, Material.WOOL, (byte) 4, "&e+3"),
        RED(52.0, 10, 3, Material.WOOL, (byte) 14, "&c+10"),
        BOOSTER(45.0, 0, 1, Material.WEB, (byte) 0, "&lBOOSTER"),
        ;

        final double spawnY;
        final int points;
        final int count;
        final Material fallingBlockType;
        final byte fallingBlockData;
        final String nameText;

        CoinType(double spawnY, int points, int count, Material fallingBlockType, byte fallingBlockData, String nameText) {
            this.spawnY = spawnY;
            this.points = points;
            this.count = count;
            this.fallingBlockType = fallingBlockType;
            this.fallingBlockData = fallingBlockData;
            this.nameText = nameText;
        }
    }

    private record Coin(CoinType type, FallingBlock fallingBlock, Bat bat, ArmorStand armorStand) {
        void removeFromWorld() {
            if (fallingBlock != null && fallingBlock.isValid()) fallingBlock.remove();
            if (bat != null && bat.isValid()) bat.remove();
            if (armorStand != null && armorStand.isValid()) armorStand.remove();
        }

        boolean isRemoved() {
            return fallingBlock == null || !fallingBlock.isValid();
        }
    }
    private final ArrayList<Coin> coins = new ArrayList<>();

    private void spawnCoin(CoinType type) {
        var spawnPosRad = Math.sqrt(rand.nextDouble()) * COIN_SPAWN_RADIUS;
        var spawnPosAngle = rand.nextFloat() * Math.PI * 2;
        var spawnOffsetXZ = new Vector(Math.cos(spawnPosAngle), 0, Math.sin(spawnPosAngle)).multiply(spawnPosRad);

        Location centerLoc = new Location(
                world,
                CENTER_BLOCK_POS.x + 0.5,
                type.spawnY,
                CENTER_BLOCK_POS.z + 0.5
        );
        Location spawnPos = centerLoc.clone().add(spawnOffsetXZ);

        Bat bat = (Bat) world.spawnEntity(spawnPos, EntityType.BAT);
        EntityUtil.makeInvisible(bat);
        EntityUtil.setSilent(bat, true);
        EntityUtil.setNoAI(bat, true);

        FallingBlock fbe = world.spawnFallingBlock(spawnPos, type.fallingBlockType, type.fallingBlockData);
        fbe.setDropItem(false);
        if (bat != null)
            bat.setPassenger(fbe);

        Location asLoc = spawnPos.clone().add(0, ARMOR_STAND_OFFSET_Y, 0);
        ArmorStand armorStand = (ArmorStand) world.spawnEntity(asLoc, EntityType.ARMOR_STAND);
        armorStand.setArms(false);
        armorStand.setVisible(false);
        armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', type.nameText));
        armorStand.setCustomNameVisible(true);
        armorStand.setGravity(false);

        coins.add(new Coin(type, fbe, bat, armorStand));
    }

    @Override
    protected void innerOnStart() {
        for (var type : CoinType.values()) {
            for (int i = 0; i < type.count; i++) {
                spawnCoin(type);
            }
        }
    }

    @Override
    protected void innerOnEnd() {

    }

    private void spawnParticleRing(Effect particleType, Location centerPos, int countPer) {
        final int NUM = 20;
        final double RADIUS = 1.0;
        for (int i = 0; i < NUM; i++) {
            double angle = Math.PI * 2 * ((double) i / (double) NUM);
            Vector offset = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(RADIUS);
            Location pos = centerPos.clone().add(offset);
            world.spigot().playEffect(pos, particleType, 0, 0, 0.0f, 0.0f, 0.0f, 0.0f, countPer, 64);
        }
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        coins.removeIf(Coin::isRemoved);

        // Prevent falling blocks from despawning
        for (var coin : coins)
            EntityUtil.keepFallingBlockAlive(coin.fallingBlock);

        for (var gamer : getPlayingGamers()) {
            Player player = gamer.player;
            if (player == null || !player.isOnline()) continue;

            BoundingBox playerHitbox = EntityUtil.getEntityHitbox(player);
            Vector playerVel = player.getVelocity();

            // Bounce update
            boolean shouldBounce = false;
            Gamer bounceVictim = null;

            // Tries to replicate the goofy thing Hypixel does which allows "ALS jumps"
            boolean buggyOnGround;
            if (getTicksElapsed() == 0) {
                buggyOnGround = player.getLocation().getY() < (BOUNCE_FLOOR_Y + 1.0 + 0.5);
            } else {
                buggyOnGround =
                        player.isOnGround() &&
                                (player.getLocation().getBlockY() == (BOUNCE_FLOOR_Y + 1)) &&
                                (player.getVelocity().getY() < (BOUNCE_VEL_Y * 0.75));
            }

            if (buggyOnGround) {
                shouldBounce = true;
            } else {
                for (var ogamer : getGamers()) {
                    if (gamer == ogamer) continue;
                    Player oPlayer = ogamer.player;
                    if (oPlayer == null || !oPlayer.isOnline()) continue;

                    if (player.getLocation().getY() > oPlayer.getLocation().getY()) {
                        if (playerHitbox.intersects(EntityUtil.getEntityHitbox(oPlayer))) {
                            shouldBounce = true;
                            bounceVictim = ogamer;
                            break;
                        }
                    }
                }
            }

            if (shouldBounce) {
                Vector forwardDirH = player.getLocation().getDirection().setY(0).normalize();
                Vector newVel = playerVel.clone().add(forwardDirH.multiply(BOUNCE_VEL_XZ));
                if (getTicksElapsed() == 0) {
                    if (newVel.getY() < 0) {
                        newVel.setY(0);
                    }
                    newVel.add(new Vector(0, BOUNCE_VEL_Y, 0));
                } else {
                    newVel.setY(BOUNCE_VEL_Y);
                }

                player.setVelocity(newVel);

                spawnParticleRing(
                        (bounceVictim != null) ? Effect.VILLAGER_THUNDERCLOUD : Effect.CLOUD, // TODO: Check ANGRY_VILLAGER sound/particle mapping
                        player.getLocation(), 2
                );
                if (bounceVictim != null) {
                    // NOTE: Sound made quieter cause its kind of loud
                    world.playSound(player.getLocation(), Sound.ANVIL_LAND, 0.6f, 1.5f);
                } else {
                    world.playSound(player.getLocation(), Sound.SLIME_ATTACK, 1.0f, 1.0f); // TODO: Check BIG_FALL equivalent
                }

                if (playerVel.getY() > 0.1 && bounceVictim == null) {
                    // They hit an "ALS jump"
                    world.playSound(player.getLocation(), Sound.ENDERDRAGON_HIT, 1.0f, 2.0f);
                }
            }

            // Coin collision
            CoinType typeToRespawn = null;
            for (var coin : coins) {
                if (coin.isRemoved())
                    continue;

                var coinHitbox = EntityUtil.getEntityHitbox(coin.bat());

                // TODO: What the heck is the actual condition? I've seen so many conflicting cases in replays...
                boolean isColliding =
                        playerHitbox.contains(coin.fallingBlock().getLocation().toVector())
                                || (coin.bat() != null && coin.bat().isValid() && playerHitbox.intersects(coinHitbox));
                if (isColliding) {
                    if (coin.type() == CoinType.BOOSTER) {
                        Vector newVel = player.getVelocity().add(new Vector(0, BOOST_ADD_VEL_Y, 0));
                        player.setVelocity(newVel);
                        Display.sendPopupText(player, "&o&lWOOSH!");
                        player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1.0f, 2.0f);
                    } else {
                        Display.sendPopupText(player, coin.type().nameText);
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 2.0f);
                        gamer.points += coin.type().points;
                        if (gamer.points >= END_GAME_POINTS)
                            end(true);
                    }

                    typeToRespawn = coin.type();
                    coin.removeFromWorld();
                    break;
                }
            }
            if (typeToRespawn != null)
                spawnCoin(typeToRespawn);
        }
    }
}