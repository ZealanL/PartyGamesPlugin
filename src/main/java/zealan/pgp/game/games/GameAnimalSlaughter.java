package zealan.pgp.game.games;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.Vec3i;

import java.util.Arrays;
import java.util.HashSet;

public class GameAnimalSlaughter extends Game {
    public GameAnimalSlaughter(InitParams params) {
        super(params);
    }

    public static final GameVariant VARIANT_FAST_ANIMALS =
            new GameVariant("fast_animals", "Fast Animals", "Animals move far faster", Material.LEATHER_BOOTS);
    public static final GameVariant VARIANT_ONE_HIT =
            new GameVariant("one_hit", "One Hit", "All animals take one hit to kill", Material.DIAMOND_SWORD);
    public static final GameVariant VARIANT_SWORD_SWAPPING =
            new GameVariant("sword_swapping", "Sword-Swapping", "An old Hypixel bug that let you kill animals faster", Material.ANVIL);

    public static final Vec3i CENTER_BLOCK_POS = new Vec3i(-1027, 26, -1879);
    private static final double ANIMAL_SPAWN_RADIUS = 17.5; // TODO: Guess, though pretty accurate it seems
    private static final int MAX_POSITIVE_ANIMALS = 40; // TODO: Guess
    private static final int MAX_NEGATIVE_ANIMALS = 5;
    private static final double NEGATIVE_ANIMAL_HEALTH = 1.0;

    // NOTE: On Hypixel the max health of the entities is different, and you do 5 damage, but whatever...

    private final HashSet<LivingEntity> animalsPos = new HashSet<>();
    private final HashSet<LivingEntity> animalsNeg = new HashSet<>();

    private enum AnimalType {
        CHICKEN(EntityType.CHICKEN, 1, 1),
        PIG(EntityType.PIG, 3, 2),
        COW(EntityType.COW, 5, 3),
        ;

        final EntityType entityType;
        final int points;
        final int hitsToKill;

        AnimalType(EntityType entityType, int points, int hitsToKill) {
            this.entityType = entityType;
            this.points = points;
            this.hitsToKill = hitsToKill;
        }

        /// ///////

        static AnimalType fromAnimal(LivingEntity animal) {
            EntityType entityType = animal.getType();
            for (AnimalType type : AnimalType.values())
                if (type.entityType.equals(entityType))
                    return type;
            return null;
        }
    }

    @Override
    protected void innerOnStart() {
        for (Gamer gamer : getPlayingGamers()) {
            ItemStack sword = new ItemStack(Material.WOOD_SWORD);
            gamer.player.getInventory().setItem(0, sword);
        }
    }

    @Override
    protected void innerOnEnd() {
        for (LivingEntity animal : animalsPos) {
            if (animal != null && animal.isValid())
                animal.remove();
        }
        for (LivingEntity animal : animalsNeg) {
            if (animal != null && animal.isValid())
                animal.remove();
        }
        animalsPos.clear();
        animalsNeg.clear();
    }

    private void spawnAnimal(boolean isNegative) {
        var pool = isNegative ? animalsNeg : animalsPos;

        // Count up number of animals of each type
        final int NUM_TYPES = AnimalType.values().length;
        var counts = new int[NUM_TYPES];
        for (var existingAnimal : pool) {
            for (int i = 0; i < NUM_TYPES; i++)
                if (existingAnimal.getType().equals(AnimalType.values()[i].entityType))
                    counts[i]++;
        }

        // Compute type spawn probability as inversely proportional to:
        // 1. The points of that animal type
        // 2. The number of that type currently in the game
        // Is this exactly accurate to what hypixel does? Probably not, but it feels good enough to me.
        var probs = new double[NUM_TYPES];
        {
            var scaledCounts = counts.clone();
            for (int i = 0; i < NUM_TYPES; i++)
                scaledCounts[i] *= AnimalType.values()[i].points;

            for (int i = 0; i < NUM_TYPES; i++)
                probs[i] = 1.0 / (1.0 + scaledCounts[i]);
            var probsDiv = Arrays.stream(probs).sum();
            for (int i = 0; i < NUM_TYPES; i++)
                probs[i] /= probsDiv;
        }

        AnimalType chosenType;
        {
            var randFrac = rand.nextDouble();
            chosenType = AnimalType.values()[NUM_TYPES - 1];
            for (int i = 0; i < NUM_TYPES - 1; i++) {
                if (randFrac < probs[i]) {
                    chosenType = AnimalType.values()[i];
                    break;
                } else {
                    randFrac -= probs[i];
                }
            }
        }

        var spawnPosRad = Math.sqrt(rand.nextDouble()) * ANIMAL_SPAWN_RADIUS;
        var spawnPosAngle = rand.nextFloat() * Math.PI * 2;
        var spawnOffset = new Vector(
                Math.cos(spawnPosAngle) * spawnPosRad,
                0,
                Math.sin(spawnPosAngle) * spawnPosRad
        );

        Location spawnLoc = new Location(
                world,
                CENTER_BLOCK_POS.x + 0.5 + spawnOffset.getX(),
                CENTER_BLOCK_POS.y,
                CENTER_BLOCK_POS.z + 0.5 + spawnOffset.getZ()
        );

        LivingEntity animalEntity = (LivingEntity) world.spawnEntity(spawnLoc, chosenType.entityType);

        if (variant == VARIANT_FAST_ANIMALS) {
            animalEntity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SPEED,
                            Integer.MAX_VALUE,
                            3
                    )
            );
        } else {
            animalEntity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SPEED,
                            Integer.MAX_VALUE,
                            1,
                            false,
                            false
                    )
            );
        }

        double maxHealth = isNegative ? NEGATIVE_ANIMAL_HEALTH : (double) chosenType.hitsToKill;
        animalEntity.setMaxHealth(maxHealth);
        animalEntity.setHealth(maxHealth);

        String customName = isNegative ? "§c-50%" : "§a+" + chosenType.points;
        animalEntity.setCustomName(customName);
        animalEntity.setCustomNameVisible(true);

        if (animalEntity instanceof Pig) {
            ((Pig) animalEntity).setSaddle(true);
        }

        pool.add(animalEntity);
    }

    @Override
    protected void innerOnTick() {
        if (!hasStarted())
            return;

        animalsPos.removeIf(animalEntity -> !animalEntity.isValid() || animalEntity.isDead());
        animalsNeg.removeIf(animalEntity -> !animalEntity.isValid() || animalEntity.isDead());

        if (this.getTicksElapsed() % 5 == 0) {
            // Run spawn logic
            if (animalsPos.size() < MAX_POSITIVE_ANIMALS)
                spawnAnimal(false);
            if (animalsNeg.size() < MAX_NEGATIVE_ANIMALS)
                spawnAnimal(true);
        }
    }

    @Override
    public boolean canAttackEntity(Player player, Entity attacked, EntityDamageEvent event) {
        var gamer = getGamer(player);
        if (gamer == null)
            return false;

        if (hasStarted() && attacked instanceof LivingEntity animal) {
            var type = AnimalType.fromAnimal(animal);
            if (type == null)
                return false;

            if (variant != VARIANT_SWORD_SWAPPING && animal.getNoDamageTicks() > 10)
                return false;

            if (variant == VARIANT_ONE_HIT) {
                event.setDamage(999.0);
            } else {
                event.setDamage(1.0);
            }
            player.playSound(player.getLocation(), Sound.HURT_FLESH, 0.8f, 1.25f);

            if (event.getDamage() >= animal.getHealth()) {
                if (animalsPos.contains(animal)) {
                    gamer.points += type.points;
                    float pitch = (float) Math.pow(2.0, ((type.points * 2) - 12) / 12.0);
                    gamer.player.playSound(
                            gamer.player.getLocation(), Sound.ORB_PICKUP,
                            (float) (0.3 + (type.points * 0.1)),
                            pitch
                    );
                } else if (animalsNeg.contains(animal)) {
                    gamer.points /= 2;
                    gamer.player.playSound(gamer.player.getLocation(), Sound.HURT_FLESH, 1.0f, 0.5f);
                }
            }
            return true;
        }

        return false;
    }
}