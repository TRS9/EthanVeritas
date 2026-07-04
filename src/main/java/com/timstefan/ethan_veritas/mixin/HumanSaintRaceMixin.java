package com.timstefan.ethan_veritas.mixin;

import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import com.timstefan.ethan_veritas.registry.race.AllRaces;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.tensura.race.human.HumanSaintRace;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 3 -> Stage 4 of the Ethan Veritas path: a Human Saint can evolve into
 * Digital Nature as an alternative to the base mod's Divine Human line.
 * The base mod has no event for extending another race's evolution list,
 * so this follows the same mixin approach the official addon example uses.
 */
@Mixin(value = HumanSaintRace.class, remap = false)
public abstract class HumanSaintRaceMixin {

    /** EP a mind must carry before Digital Nature becomes reachable. */
    private static final double DIGITAL_NATURE_EP_REQUIREMENT = 5_000_000.0D;

    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true)
    private void ethan_veritas$addDigitalNature(ManasRaceInstance instance, LivingEntity entity, CallbackInfoReturnable<List<ManasRace>> cir) {
        // Digital Nature only reveals itself to a mind that could survive it: enough EP
        // plus at least one analyst-type skill (Analyst, Great Sage, Sage, or Thoth).
        if (TensuraStorages.getExistenceFrom(entity).getEP() < DIGITAL_NATURE_EP_REQUIREMENT) return;
        if (!ProgressionChecks.hasAnalystTypeSkill(entity)) return;
        List<ManasRace> evolutions = new ArrayList<>(cir.getReturnValue());
        evolutions.add(AllRaces.DIGITAL_NATURE.get());
        cir.setReturnValue(evolutions);
    }
}
