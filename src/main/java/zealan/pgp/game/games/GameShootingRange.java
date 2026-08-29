package zealan.pgp.game.games;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import zealan.pgp.api.display.Display;
import zealan.pgp.game.Game;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;
import zealan.pgp.util.EntityUtil;

import java.util.*;

public class GameShootingRange extends Game {
    public GameShootingRange(InitParams params) {
        super(params);
    }

    private static final Vec3i[] TARGET_SPAWNS = {
            new Vec3i(1114, 27, 751),
            new Vec3i(1120, 25, 751),
            new Vec3i(1108, 29, 751),
            new Vec3i(1102, 31, 751),
            new Vec3i(1102, 31, 715),
            new Vec3i(1108, 29, 715),
            new Vec3i(1114, 27, 715),
            new Vec3i(1120, 25, 715),
    };
    private static final int CENTER_Z = 733;

    // TODO: Guesses
    private static final double TNT_ZOMBIE_SPAWN_PROB = 1.0 / 30.0;
    private static final int TNT_ZOMBIE_LIMIT = 2;
    private static final double EXPLOSION_KILL_RANGE = 5.0;

    private final HashMap<Gamer, Integer> explosiveArrowCounts = new HashMap<>();
    private final HashMap<Gamer, Integer> queuedPoints = new HashMap<>();
    private final HashMap<LivingEntity, TargetType> targets = new HashMap<>();
    private final HashSet<Arrow> knownArrows = new HashSet<>();

    private enum TargetType {
        // TODO: All spawn weights and min counts are guesses
        ZOMBIE(EntityType.ZOMBIE, 2.0, 1, 16, 4),
        SKELETON(EntityType.SKELETON, 4.0, 2, 10, 3),
        ARMORED_ZOMBIE(EntityType.ZOMBIE, 7.0, 10, 4, 1),
        ARMORED_SKELETON(EntityType.SKELETON, 8.0, 20, 2, 1),
        TNT_ZOMBIE(EntityType.ZOMBIE, 9.0, 0, 0, 0),
        ;

        private static final int TOTAL_SPAWN_WEIGHT;
        static {
            int totalWeight = 0;
            for (TargetType targetType : TargetType.values())
                totalWeight += targetType.spawnWeight;
            TOTAL_SPAWN_WEIGHT = totalWeight;
        }

        final EntityType entityType;
        final double speed;
        final int points;
        final int spawnWeight;
        final int minCount;

        TargetType(EntityType entityType, double speed, int points, int spawnWeight, int minCount) {
            this.entityType = entityType;
            this.speed = speed;
            this.points = points;
            this.spawnWeight = spawnWeight;
            this.minCount = minCount;
        }

        static TargetType pickRandomWeighted(java.util.Random rand, Collection<TargetType> spawnedTypes) {
            if (rand.nextDouble() < TNT_ZOMBIE_SPAWN_PROB) {
                long numTntZombies = spawnedTypes.stream().filter(t -> t == TargetType.TNT_ZOMBIE).count();
                if (numTntZombies < TNT_ZOMBIE_LIMIT)
                    return TargetType.TNT_ZOMBIE;
            }

            for (TargetType targetType : TargetType.values()) {
                long count = spawnedTypes.stream().filter(t -> t == targetType).count();
                if (count < targetType.minCount)
                    return targetType;
            }

            int r = rand.nextInt(TOTAL_SPAWN_WEIGHT);
            for (TargetType targetType : TargetType.values()) {
                if (r <= targetType.spawnWeight) {
                    return targetType;
                } else {
                    r -= targetType.spawnWeight;
                }
            }

            throw new RuntimeException("Unreachable!?");
        }

        LivingEntity spawn(Location loc) {
            // Stupid spawning loop to prevent certain entities
            LivingEntity entity = null;
            do {
                if (entity != null)
                    entity.remove();
                entity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);
            } while (entity instanceof Zombie zombie && (zombie.isVillager() || zombie.isBaby()));

            entity.setMaxHealth(1.0);
            entity.setHealth(1.0);
            entity.setRemoveWhenFarAway(false);
            entity.getEquipment().clear();

            switch (this) {
                case ARMORED_ZOMBIE, ARMORED_SKELETON -> {
                    entity.getEquipment().setHelmet(new ItemStack(Material.GOLD_HELMET));
                    entity.getEquipment().setChestplate(new ItemStack(Material.GOLD_CHESTPLATE));
                    entity.getEquipment().setLeggings(new ItemStack(Material.GOLD_LEGGINGS));
                    entity.getEquipment().setBoots(new ItemStack(Material.GOLD_BOOTS));
                }
                case TNT_ZOMBIE -> entity.getEquipment().setHelmet(new ItemStack(Material.TNT));
                default -> {}
            }
            return entity;
        }
    }

    private static final int MAX_TARGETS = 25;
    private static final int SPAWN_INTERVAL = 7;
    private static final int END_GAME_POINTS = 60;

    private int timeSinceSpawn = 0;

    private void spawnTarget() {
        TargetType type = TargetType.pickRandomWeighted(rand, targets.values());
        Vec3i spawnPos = TARGET_SPAWNS[rand.nextInt(TARGET_SPAWNS.length)];
        Location loc = new Location(world, spawnPos.x + 0.5, spawnPos.y, spawnPos.z + 0.5);

        loc.setYaw(spawnPos.z > CENTER_Z ? -180f : 0f);

        LivingEntity ent = type.spawn(loc);
        EntityUtil.setSilent(ent, true);
        EntityUtil.setNoAI(ent, true);
        targets.put(ent, type);
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getPlayingGamers()) {
            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
            gamer.player.getInventory().setItem(0, bow);
            gamer.player.getInventory().setItem(1, new ItemStack(Material.ARROW));
        }
    }

    @Override
    protected void innerOnEnd() {
        for (LivingEntity ent : targets.keySet()) {
            if (ent != null && ent.isValid())
                ent.remove();
        }
        targets.clear();
        explosiveArrowCounts.clear();
        queuedPoints.clear();
        knownArrows.clear();
    }

    private void killTarget(Gamer gamer, LivingEntity targetEnt) {
        TargetType type = targets.remove(targetEnt);
        if (type == null)
            return;

        if (targetEnt.isValid())
            targetEnt.remove();


        world.spigot().playEffect(
                targetEnt.getLocation(), Effect.EXPLOSION,
                0, 0,
                0.3f, 2.0f, 0.3f,
                0.02f, 20, 64
        );

        if (gamer == null || !gamer.isPlaying())
            return;

        if (type.points != 0)
            queuedPoints.merge(gamer, type.points, Integer::sum);

        if (type == TargetType.TNT_ZOMBIE) {
            explosiveArrowCounts.merge(gamer, 2, Integer::sum);
            Display.sendMsg(gamer.player, "&6+2 &c&lExplosive Arrows");
            gamer.player.playSound(gamer.player.getLocation(), Sound.ORB_PICKUP, 0.7f, 1.0f);
            gamer.player.playSound(gamer.player.getLocation(), Sound.NOTE_BASS, 1.0f, 2.0f); // TODO: closest match to NOTE_SNARE
        }
    }

    @Override
    protected void innerOnTick() {
        targets.keySet().removeIf(ent -> !ent.isValid() || ent.isDead());

        int entitySpawnIters = hasStarted() ? 1 : 2;
        for (int i = 0; i < entitySpawnIters; i++) {
            if (timeSinceSpawn >= SPAWN_INTERVAL) {
                if (targets.size() < MAX_TARGETS)
                    spawnTarget();
                timeSinceSpawn = 0;
            } else {
                timeSinceSpawn++;
            }
        }

        for (var entry : targets.entrySet()) {
            LivingEntity targetEnt = entry.getKey();
            TargetType targetType = entry.getValue();
            Vector curLoc = targetEnt.getLocation().toVector();

            // Both sets starting spawn, and turns them around
            Vec3i targetBlockPos = Vec3i.from(curLoc);
            boolean isOnSpawnPoint = Arrays.asList(TARGET_SPAWNS).contains(targetBlockPos);
            if (isOnSpawnPoint) {
                float yaw = targetBlockPos.z > CENTER_Z ? -180f : 0f;
                EntityUtil.setYaw(targetEnt, yaw);
                EntityUtil.setHeadYaw(targetEnt, yaw);
            }

            double yawRad = Math.toRadians(EntityUtil.getYaw(targetEnt));
            Vector forwardDir = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
            Vector moveDelta = forwardDir.multiply(targetType.speed / 20.0);

            EntityUtil.setPosOnly(targetEnt, curLoc.add(moveDelta));
        }

        for (Arrow arrow : world.getEntitiesByClass(Arrow.class)) {
            if (knownArrows.add(arrow) && arrow.getShooter() instanceof Player shooterPlayer) {
                Gamer shooterGamer = getGamer(shooterPlayer);
                Integer count = shooterGamer != null ? explosiveArrowCounts.get(shooterGamer) : null;
                if (count != null && count > 0) {
                    explosiveArrowCounts.put(shooterGamer, count - 1);
                    shooterPlayer.playSound(shooterPlayer.getLocation(), Sound.FIZZ, 0.7f, 1.5f); // TODO: closest match to TNT_PRIME
                    arrow.setCustomName(" ");
                    arrow.setCustomNameVisible(true);
                }
            }
        }
        knownArrows.removeIf(a -> a == null || !a.isValid());

        for (var entry : queuedPoints.entrySet()) {
            Gamer gamer = entry.getKey();
            int points = entry.getValue();
            gamer.points += points;

            double scale = Math.sqrt(points) / 5;
            boolean isLots = points > 20;
            gamer.player.playSound(
                    gamer.player.getLocation(),
                    isLots ? Sound.LEVEL_UP : Sound.ORB_PICKUP,
                    (float) Math.min(0.5 + scale, 0.9),
                    (float) (0.5 + scale)
            );

            if (isLots) {
                Display.sendPopupText(gamer.player, "&6>> &a&l+" + points + " &6<<");
            } else {
                Display.sendPopupText(gamer.player, "&a+" + points);
            }

            if (gamer.points >= END_GAME_POINTS)
                end(true);
        }
        queuedPoints.clear();
    }

    @Override
    public boolean canAttackEntity(Player player, Entity entity, EntityDamageByEntityEvent event) {
        if (!hasStarted())
            return false;

        if (entity instanceof LivingEntity livingEntity && targets.containsKey(livingEntity)) {
            event.setDamage(999.0);
            killTarget(getGamer(player), livingEntity);
            return true;
        }

        return false;
    }

    @Override
    public void onProjectileHit(Projectile projectile) {
        if (!(projectile instanceof Arrow arrow) || !arrow.isCustomNameVisible())
            return;

        Location hitLoc = arrow.getLocation();
        world.createExplosion(hitLoc.getX(), hitLoc.getY(), hitLoc.getZ(), 0.0f, false, false);
        world.spigot().playEffect(
                hitLoc, Effect.EXPLOSION_LARGE,
                0, 1,
                0.3f, 0.3f, 0.3f,
                0.2f, 1, 64
        );

        Gamer shooterGamer = arrow.getShooter() instanceof Player p ? getGamer(p) : null;
        for (LivingEntity target : new ArrayList<>(targets.keySet())) {
            if (target.getLocation().distanceSquared(hitLoc) <= EXPLOSION_KILL_RANGE * EXPLOSION_KILL_RANGE)
                killTarget(shooterGamer, target);
        }

        projectile.remove();
    }

    @Override
    public boolean canUseItem(Player player, ItemStack item) {
        return item.getType() == Material.BOW;
    }
}