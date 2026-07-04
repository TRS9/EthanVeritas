package com.timstefan.ethan_veritas.mixin;

import com.timstefan.ethan_veritas.registry.race.AllRaces;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.tensura.race.human.HumanSaintRace;
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

    @Inject(method = "getNextEvolutions", at = @At("RETURN"), cancellable = true)
    private void ethan_veritas$addDigitalNature(ManasRaceInstance instance, LivingEntity entity, CallbackInfoReturnable<List<ManasRace>> cir) {
        List<ManasRace> evolutions = new ArrayList<>(cir.getReturnValue());
        evolutions.add(AllRaces.DIGITAL_NATURE.get());
        cir.setReturnValue(evolutions);
    }
}
