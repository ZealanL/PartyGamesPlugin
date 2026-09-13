package zealan.pgp.world;

import net.minecraft.server.v1_8_R3.ChunkSection;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import zealan.pgp.math.BlockRange2d;

import static zealan.pgp.Globals.PLOG;

public class FastWorldCopy {
    public static void copyRange(World srcWorld, World dstWorld, BlockRange2d range) {
        net.minecraft.server.v1_8_R3.World src = ((CraftWorld) srcWorld).getHandle();
        net.minecraft.server.v1_8_R3.World dst = ((CraftWorld) dstWorld).getHandle();

        int minCx = range.minX >> 4;
        int maxCx = range.maxX >> 4;
        int minCz = range.minZ >> 4;
        int maxCz = range.maxZ >> 4;

        PLOG.info(" > Loading chunks...");
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                var bukkitSrcChunk = srcWorld.getChunkAt(cx, cz);
                if (((CraftChunk)bukkitSrcChunk).getHandle().isEmpty())
                    continue;

                var bukkitDstChunk = dstWorld.getChunkAt(cx, cz);
                if (!bukkitSrcChunk.isLoaded()) bukkitSrcChunk.load();
                if (!bukkitSrcChunk.isLoaded()) bukkitDstChunk.load();
            }
        }

        PLOG.info(" > Copying chunks...");
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                var srcChunk = src.getChunkAt(cx, cz);
                if (srcChunk.isEmpty())
                    continue;
                var dstChunk = dst.getChunkAt(cx, cz);
                copySections(srcChunk, dstChunk);
            }
        }
    }

    private static void copySections(net.minecraft.server.v1_8_R3.Chunk src, net.minecraft.server.v1_8_R3.Chunk dst) {
        ChunkSection[] srcSections = src.getSections();
        ChunkSection[] dstSections = dst.getSections();

        for (int i = 0; i < srcSections.length; i++) {
            ChunkSection srcSection = srcSections[i];
            if (srcSection == null) {
                dstSections[i] = null;
                continue;
            }

            var copy = new ChunkSection(srcSection.getYPosition(), true);
            System.arraycopy(
                    srcSection.getIdArray(), 0,
                    copy.getIdArray(), 0,
                    srcSection.getIdArray().length
            );
            System.arraycopy(
                    srcSection.getSkyLightArray().a(), 0,
                    copy.getSkyLightArray().a(), 0,
                    srcSection.getSkyLightArray().a().length
            );
            System.arraycopy(
                    srcSection.getEmittedLightArray().a(), 0,
                    copy.getEmittedLightArray().a(), 0,
                    srcSection.getEmittedLightArray().a().length
            );

            copy.recalcBlockCounts();
            dstSections[i] = copy;
        }
        dst.initLighting();
        dst.f(true);
    }
}
