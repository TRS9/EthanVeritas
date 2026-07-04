package com.timstefan.ethan_veritas.handler;

import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * X-ray style ore highlighting for Thoth's Universal Detect: an invisible, AI-less,
 * glowing marker entity is placed inside each sensed Magic Ore block - the glowing
 * outline renders through walls exactly like a glowing living entity, which is the
 * only through-wall highlight available without a custom client shader.
 * <p>
 * The marker is a size-1 slime rather than a shulker: its half-block outline sits
 * inset within the ore, so it never z-fights ("flickers") against the block faces
 * when looked at directly.
 * <p>
 * Markers are discarded when their lifetime ends OR the moment their ore block no
 * longer exists (mined, exploded), so no ghost markers linger over empty holes.
 * Any marker that survives into a fresh session (crash, chunk unload race) is
 * discarded the moment it joins a level again.
 * <p>
 * Listeners are registered programmatically from the mod constructor (init())
 * instead of via annotation scanning, so their registration is never in doubt.
 */
public class OreRevealHandler {
    private static final String ORE_MARKER = "ethan_veritas_ore_marker";
    private static final List<Marker> ACTIVE = new ArrayList<>();

    private record Marker(Entity entity, BlockPos pos, long expiry) {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(OreRevealHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(OreRevealHandler::onEntityJoin);
    }

    public static void spawnMarker(ServerLevel level, BlockPos pos, long lifetimeTicks) {
        Slime marker = EntityType.SLIME.create(level);
        if (marker == null) return;
        marker.setPos(pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D);
        marker.setNoAi(true);
        marker.setSilent(true);
        marker.setInvulnerable(true);
        marker.setInvisible(true);
        marker.setGlowingTag(true);
        marker.setPersistenceRequired();
        marker.getPersistentData().putBoolean(ORE_MARKER, true);
        // Track BEFORE spawning: the join listener below fires during addFreshEntity
        // and treats any flagged-but-untracked marker as a stale leftover to delete.
        ACTIVE.add(new Marker(marker, pos, level.getGameTime() + lifetimeTicks));
        level.addFreshEntity(marker);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Marker> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            Entity entity = marker.entity();
            if (entity.isRemoved()) {
                iterator.remove();
            } else if (entity.level().getGameTime() >= marker.expiry() || !isStillMagicOre(entity, marker.pos())) {
                entity.discard();
                iterator.remove();
            }
        }
    }

    /** The highlight dies with the ore: once mined, no ghost marker remains. */
    private static boolean isStillMagicOre(Entity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        return state.is((Block) TensuraBlocks.MAGIC_ORE.get()) || state.is((Block) TensuraBlocks.DEEPSLATE_MAGIC_ORE.get());
    }

    /** Markers never outlive a session: anything re-joining a level with the flag is stale. */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean(ORE_MARKER)
                && ACTIVE.stream().noneMatch(marker -> marker.entity() == entity)) {
            entity.discard();
        }
    }
}
