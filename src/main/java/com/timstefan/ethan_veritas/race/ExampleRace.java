package com.timstefan.ethan_veritas.race;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.tensura.race.TensuraRace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

// Extend TensuraRace (not ManasRace directly) so the new race plugs into Tensura's
// aura/magicule/evolution systems. The only methods TensuraRace still forces you to
// implement are the aura and magicule ranges below.
public class ExampleRace extends TensuraRace {

    public ExampleRace() {
        super(ManasRace.Difficulty.INTERMEDIATE);

        // Stats are no longer overridden as getters (that changed from the older
        // ManasCore API) - instead register vanilla attribute modifiers here.
        addAttributeModifier(Attributes.MAX_HEALTH, ResourceLocation.fromNamespaceAndPath(MODID, "example_race_health"), 12.0D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(MODID, "example_race_speed"), 0.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public Pair<Double, Double> getBaseAuraRange() {
        return Pair.of(800.0D, 1211.0D);
    }

    @Override
    public Pair<Double, Double> getBaseMagiculeRange() {
        return Pair.of(80.0D, 120.0D);
    }
}
