package com.timstefan.ethan_veritas.handler;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

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
 * <p>
 * Listeners are registered programmatically from the mod constructor (init())
 * instead of via annotation scanning, so their registration is never in doubt.
 */
public class ErasureDropHandler {
    public static final String ERASED_AT = "ethan_veritas_erased_at";
    private static final long VALID_WINDOW_TICKS = 100L;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ErasureDropHandler::onDrops);
        NeoForge.EVENT_BUS.addListener(ErasureDropHandler::onExperienceDrop);
    }

    public static void onDrops(LivingDropsEvent event) {
        if (wasJustErased(event.getEntity().getPersistentData().getLong(ERASED_AT), event.getEntity().level().getGameTime(),
                event.getEntity().getPersistentData().contains(ERASED_AT))) {
            event.setCanceled(true);
        }
    }

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
