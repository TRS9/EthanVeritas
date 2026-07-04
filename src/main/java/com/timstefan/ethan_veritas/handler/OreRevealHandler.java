package com.timstefan.ethan_veritas.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * X-ray style ore highlighting for Thoth's Universal Detect: an invisible, AI-less,
 * glowing marker entity is placed inside each sensed Magic Ore block - the glowing
 * outline renders through walls exactly like a glowing living entity, which is the
 * only through-wall highlight available without a custom client shader.
 * <p>
 * Markers are tracked and discarded after their lifetime. Any marker that survives
 * into a fresh session (crash, chunk unload race) is discarded the moment it joins
 * a level again, so no orphaned glowing boxes ever linger in the world.
 */
@EventBusSubscriber(modid = MODID)
public class OreRevealHandler {
    private static final String ORE_MARKER = "ethan_veritas_ore_marker";
    private static final List<Marker> ACTIVE = new ArrayList<>();

    private record Marker(Entity entity, long expiry) {
    }

    public static void spawnMarker(ServerLevel level, BlockPos pos, long lifetimeTicks) {
        Shulker marker = EntityType.SHULKER.create(level);
        if (marker == null) return;
        marker.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        marker.setNoAi(true);
        marker.setSilent(true);
        marker.setInvulnerable(true);
        marker.setInvisible(true);
        marker.setGlowingTag(true);
        marker.getPersistentData().putBoolean(ORE_MARKER, true);
        level.addFreshEntity(marker);
        ACTIVE.add(new Marker(marker, level.getGameTime() + lifetimeTicks));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Marker> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            if (marker.entity().isRemoved()) {
                iterator.remove();
            } else if (marker.entity().level().getGameTime() >= marker.expiry()) {
                marker.entity().discard();
                iterator.remove();
            }
        }
    }

    /** Markers never outlive a session: anything re-joining a level with the flag is stale. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean(ORE_MARKER)
                && ACTIVE.stream().noneMatch(marker -> marker.entity() == entity)) {
            entity.discard();
        }
    }
}
