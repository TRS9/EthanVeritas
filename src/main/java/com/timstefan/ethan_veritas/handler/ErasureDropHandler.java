package com.timstefan.ethan_veritas.handler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Absolute Erasure removes an existence "leaving no Infons trace at any layer":
 * the kill itself is real (so EP gain, souls and other death-based rewards credit
 * the wielder like any boss kill), but nothing physical remains - no item drops,
 * no experience orbs.
 * <p>
 * The skill stamps the victim's persistent data with the game time just before
 * dealing the killing blow; these handlers consume the stamp. The short validity
 * window means a lingering stamp (e.g. if some cheat-death ability cancelled the
 * kill) cannot eat the drops of an unrelated later death.
 */
@EventBusSubscriber(modid = MODID)
public class ErasureDropHandler {
    public static final String ERASED_AT = "ethan_veritas_erased_at";
    private static final long VALID_WINDOW_TICKS = 100L;

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (wasJustErased(event.getEntity().getPersistentData().getLong(ERASED_AT), event.getEntity().level().getGameTime(),
                event.getEntity().getPersistentData().contains(ERASED_AT))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (wasJustErased(event.getEntity().getPersistentData().getLong(ERASED_AT), event.getEntity().level().getGameTime(),
                event.getEntity().getPersistentData().contains(ERASED_AT))) {
            event.setCanceled(true);
        }
    }

    private static boolean wasJustErased(long erasedAt, long now, boolean stamped) {
        return stamped && now - erasedAt <= VALID_WINDOW_TICKS;
    }
}
