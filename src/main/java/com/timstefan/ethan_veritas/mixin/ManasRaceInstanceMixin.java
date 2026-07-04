package com.timstefan.ethan_veritas.mixin;

import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import com.timstefan.ethan_veritas.race.DigitalNatureRace;
import com.timstefan.ethan_veritas.registry.race.AllRaces;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Digital Nature is reachable from ANY race, not just the human line: Divine Human,
 * demons, beastfolk - whatever the player currently is. The gate is purely mental:
 * an analytical-department Ultimate Skill plus enough EP (see ProgressionChecks).
 * <p>
 * Hooked at the ManasRaceInstance delegate so a single injection covers every race,
 * instead of mixing into each race class individually.
 */
@Mixin(value = ManasRaceInstance.class, remap = false)
public abstract class ManasRaceInstanceMixin {

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true)
    private void ethan_veritas$addDigitalNature(LivingEntity entity, CallbackInfoReturnable<List<ManasRace>> cir) {
        ManasRaceInstance instance = (ManasRaceInstance) (Object) this;
        if (instance.getRace() instanceof DigitalNatureRace) return;
        if (!ProgressionChecks.canReachDigitalNature(entity)) return;
        ManasRace digitalNature = AllRaces.DIGITAL_NATURE.get();
        List<ManasRace> evolutions = new ArrayList<>(cir.getReturnValue());
        if (!evolutions.contains(digitalNature)) {
            evolutions.add(digitalNature);
            cir.setReturnValue(evolutions);
        }
    }
}
