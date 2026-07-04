package com.timstefan.ethan_veritas.race;

import com.mojang.datafixers.util.Pair;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Digital Nature - the single actual race change in the Ethan Veritas progression.
 * Everything below this tier is pure skill acquisition on top of whatever race the
 * player already has; this race is the endgame threshold shared by True-Dragon-tier
 * existences in the source material.
 * <p>
 * Registered under the shared spiritual race tag (data/tensura/tags/manascore_race/races)
 * so it behaves like the base mod's other spiritual endgame races. The design document's
 * "cooldown-only resource model" was dropped in favor of the base mod's magicule system;
 * the race instead gets a True-Dragon-scale energy pool.
 */
public class DigitalNatureRace extends TensuraRace {

    public DigitalNatureRace() {
        super(ManasRace.Difficulty.EXTREME);
        addAttributeModifier(Attributes.MAX_HEALTH, ResourceLocation.fromNamespaceAndPath(MODID, "digital_nature_health"), 20.0D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(MODID, "digital_nature_speed"), 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public Pair<Double, Double> getBaseAuraRange() {
        return Pair.of(200_000.0D, 400_000.0D);
    }

    @Override
    public Pair<Double, Double> getBaseMagiculeRange() {
        return Pair.of(200_000.0D, 400_000.0D);
    }

    @Override
    public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance instance, LivingEntity entity) {
        List<ManasSkill> skills = new ArrayList<>();
        skills.add(AllSkills.AIN_SOPH_AUR.get());
        // Stopped World Navigation / Dimensional travel flavor via base mod skills.
        skills.add(ExtraSkills.SPATIAL_MOTION.get());
        skills.add(ExtraSkills.UNIVERSAL_PERCEPTION.get());
        return skills;
    }
}
