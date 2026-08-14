package zealan.pgp.game.games;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import zealan.pgp.bossbar.BossBarContent;
import zealan.pgp.game.Game;
import zealan.pgp.game.GameVariant;
import zealan.pgp.game.Gamer;
import zealan.pgp.math.BlockRange;
import zealan.pgp.math.BoundingBox;
import zealan.pgp.math.Vec3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class GameVolcano extends Game {

    public static final Vec3i CENTER_BLOCK_POS = new Vec3i(-296, 25, -1888);
    private static final int FLOOR_Y = 24;
    private static final BlockRange FLOOR_BOUNDING_RANGE = BlockRange.fromCorners(
            CENTER_BLOCK_POS.x - 20, FLOOR_Y, CENTER_BLOCK_POS.z - 20,
            CENTER_BLOCK_POS.x + 20, FLOOR_Y, CENTER_BLOCK_POS.z + 20
    );
    private static final int DOUBLE_DECAY_BLOCKS_REMAINING_THRESH = 1000;
    private static final double STILL_SPEED_THRESH = 0.08;

    private static class TileStage {
        final byte woolData;
        final int duration;

        TileStage(byte woolData, int duration) {
            this.woolData = woolData;
            this.duration = duration;
        }
    }

    private static class TileState {
        private static final List<TileStage> FLOOR_TILE_STAGES = List.of(
                new TileStage(DyeColor.GRAY.getWoolData(), 0),
                new TileStage(DyeColor.YELLOW.getWoolData(), 25),
                new TileStage(DyeColor.ORANGE.getWoolData(), 25),
                new TileStage(DyeColor.RED.getWoolData(), 30)
        );

        private static final int FLOOR_TILE_DECAY_TICKS;

        static {
            int maxHealth = 0;
            for (TileStage stage : FLOOR_TILE_STAGES) {
                maxHealth += stage.duration;
            }
            FLOOR_TILE_DECAY_TICKS = maxHealth;
        }

        private byte lastData = FLOOR_TILE_STAGES.get(0).woolData;
        private int decayTicks = -1;

        void startDecaying() {
            this.decayTicks = Math.max(this.decayTicks, 0);
        }

        boolean hasStartedDecay() {
            return this.decayTicks >= 0;
        }

        boolean hasFinishedDecaying() {
            return this.decayTicks >= FLOOR_TILE_DECAY_TICKS;
        }

        boolean tick() {
            if (hasStartedDecay() && !hasFinishedDecaying()) {
                this.decayTicks++;
                byte newData = getCurData();
                if (newData != lastData) {
                    lastData = newData;
                    return true;
                }
            }
            return false;
        }

        byte getCurData() {
            int subDecay = this.decayTicks;
            for (TileStage stage : FLOOR_TILE_STAGES) {
                if (subDecay < stage.duration) {
                    return stage.woolData;
                } else {
                    subDecay -= stage.duration;
                }
            }
            return -1; // Indicates AIR
        }
    }

    private final HashMap<Vec3i, TileState> tileStates = new HashMap<>();
    private int tileStatesInitialCount = 0;

    public GameVolcano(InitParams params) {
        super(params);
    }

    @Override
    public void onLoaded() {
        super.onLoaded();
        world.setTime(18000);
    }

    @Override
    protected void innerOnStart() {
        tileStates.clear();
        byte lightGrayData = DyeColor.SILVER.getWoolData();

        for (Vec3i pos : FLOOR_BOUNDING_RANGE) {
            Block block = world.getBlockAt(pos.x, pos.y, pos.z);
            if (block.getType() == Material.WOOL && block.getData() == lightGrayData) {
                tileStates.put(pos, new TileState());
            }
        }
        tileStatesInitialCount = tileStates.size();
    }

    @Override
    protected void innerOnEnd() {
    }

    @Override
    public BossBarContent updateBossBar(Gamer gamer) {
        double remainingFrac = tileStatesInitialCount > 0 ? ((double)tileStates.size() / tileStatesInitialCount) : 0f;
        String countColor = (remainingFrac < 0.5) ? "&c" : "&e";

        return new BossBarContent(
                "Blocks left: " + countColor + tileStates.size(),
                remainingFrac
        );
    }

    @Override
    protected void innerOnTick() {
        for (Gamer gamer : getPlayingGamers()) {
            Player player = gamer.player;
            Block underBlock = player.getLocation().getBlock();

            // Kill when falling into lava
            if (underBlock.getType() == Material.LAVA || underBlock.getType() == Material.STATIONARY_LAVA) {
                player.damage(999.0);
                continue;
            }

            if (player.isOnGround()) {
                HashSet<Vec3i> touchingBlockPositions = new HashSet<>();
                Vector vel = player.getVelocity();
                double horizontalSpeed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
                boolean isStill = horizontalSpeed < STILL_SPEED_THRESH;

                if (isStill) {
                    double minX = player.getLocation().getX() - 0.3;
                    double maxX = player.getLocation().getX() + 0.3;
                    double minZ = player.getLocation().getZ() - 0.3;
                    double maxZ = player.getLocation().getZ() + 0.3;

                    touchingBlockPositions.add(new Vec3i((int) Math.floor(minX), FLOOR_Y, (int) Math.floor(minZ)));
                    touchingBlockPositions.add(new Vec3i((int) Math.floor(maxX), FLOOR_Y, (int) Math.floor(minZ)));
                    touchingBlockPositions.add(new Vec3i((int) Math.floor(minX), FLOOR_Y, (int) Math.floor(maxZ)));
                    touchingBlockPositions.add(new Vec3i((int) Math.floor(maxX), FLOOR_Y, (int) Math.floor(maxZ)));
                } else {
                    touchingBlockPositions.add(new Vec3i(player.getLocation().getBlockX(), FLOOR_Y, player.getLocation().getBlockZ()));
                }

                for (Vec3i tbp : touchingBlockPositions) {
                    TileState tileState = tileStates.get(tbp);
                    if (tileState != null && !tileState.hasStartedDecay()) {
                        tileState.startDecaying();
                    }
                }
            }
        }

        // Make random un-decayed tiles start decaying
        List<Vec3i> undecayeds = new ArrayList<>();
        for (Map.Entry<Vec3i, TileState> entry : tileStates.entrySet()) {
            if (!entry.getValue().hasStartedDecay()) {
                undecayeds.add(entry.getKey());
            }
        }

        if (!undecayeds.isEmpty()) {
            int numToStartDecay = tileStates.size() > DOUBLE_DECAY_BLOCKS_REMAINING_THRESH ? 1 : 2;
            for (int i = 0; i < numToStartDecay; i++) {
                Vec3i targetPos = undecayeds.get(rand.nextInt(undecayeds.size()));
                tileStates.get(targetPos).startDecaying();
            }
        }

        // Update block states
        for (Map.Entry<Vec3i, TileState> entry : tileStates.entrySet()) {
            Vec3i pos = entry.getKey();
            TileState state = entry.getValue();

            if (state.tick()) {
                Block block = world.getBlockAt(pos.x, pos.y, pos.z);
                byte curData = state.getCurData();
                if (curData < 0) {
                    block.setType(Material.AIR);
                } else {
                    block.setType(Material.WOOL);
                    block.setData(curData);
                }
            }
        }

        tileStates.entrySet().removeIf(e -> e.getValue().hasFinishedDecaying());
    }
}