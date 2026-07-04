package com.timstefan.ethan_veritas.race;

import com.mojang.datafixers.util.Pair;
import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.attribute.api.ManasCoreAttributes;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Digital Nature - the single actual race change in the Ethan Veritas progression,
 * and the strongest race in the pack. Reference point: the base mod's Divine Human
 * tops out at 1M aura / 1M magicule, +980 max health, +6000 spiritual health,
 * +3 attack, +0.7 attack speed, +0.3 knockback resistance (HumanConfig defaults);
 * a being of pure information sits above that on every axis.
 * <p>
 * Registered under the shared spiritual race tag (data/tensura/tags/manascore_race)
 * so it behaves like the base mod's other spiritual endgame races. The design
 * document's "cooldown-only resource model" was dropped in favor of the base mod's
 * magicule system; the race instead gets the largest energy pool in the pack.
 */
public class DigitalNatureRace extends TensuraRace {

    public DigitalNatureRace() {
        super(ManasRace.Difficulty.EXTREME);
        // Mirrors DefaultRace.applyDefaultAttributeModifiers, with values above Divine Human's.
        addAttributeModifier(Attributes.MAX_HEALTH, id("digital_nature_health"), 1200.0D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(TensuraAttributes.MAX_SPIRITUAL_HEALTH, id("digital_nature_spiritual_health"), 10_000.0D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, id("digital_nature_attack"), 5.0D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ATTACK_SPEED, id("digital_nature_attack_speed"), 1.0D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("digital_nature_knockback"), 0.5D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, id("digital_nature_speed"), 0.15D, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(ManasCoreAttributes.SWIM_SPEED_MULTIPLIER, id("digital_nature_swim"), 1.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /**
     * The "enough EP" gate for evolving INTO Digital Nature (the base mod asks the
     * TARGET race for its requirements). Shown as a progress bar on the evolution
     * screen. Divine Human's gate is 2M EP; the strongest race matches it.
     */
    @Override
    public Map<EvolutionRequirement, Float> getEvolutionRequirements(ManasRaceInstance previous, LivingEntity entity) {
        return Map.of(new EvolutionRequirement.EPRequirement(2_000_000.0D), 100.0F);
    }

    /**
     * Evolution rewards, run once when the evolution completes: on top of the base
     * floor-raising, shedding material dependence transcends conventional interference -
     * every acquired resistance becomes a nullification, exactly like the True Demon
     * Lord / True Hero awakenings.
     */
    @Override
    public void triggerEvolutionRewards(ManasRaceInstance instance, LivingEntity entity) {
        super.triggerEvolutionRewards(instance, entity);
        AbilityUtils.upgradeResistancesToNullifications(entity);
    }

    @Override
    public Pair<Double, Double> getBaseAuraRange() {
        return Pair.of(2_000_000.0D, 2_500_000.0D);
    }

    @Override
    public Pair<Double, Double> getBaseMagiculeRange() {
        return Pair.of(2_000_000.0D, 2_500_000.0D);
    }

    @Override
    public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance instance, LivingEntity entity) {
        List<ManasSkill> skills = new ArrayList<>();
        // Ain Soph Aur is the convergence of Thoth and Yggdrasil - it only manifests for a
        // Digital Nature that carries both, and never re-manifests once Ain has replaced it.
        if (SkillUtils.hasSkill(entity, AllSkills.THOTH.get())
                && SkillUtils.hasSkill(entity, AllSkills.YGGDRASIL.get())
                && !SkillUtils.hasSkill(entity, AllSkills.AIN.get())) {
            skills.add(AllSkills.AIN_SOPH_AUR.get());
        }
        // Stopped World Navigation / Dimensional travel flavor via base mod skills.
        skills.add(ExtraSkills.SPATIAL_MOTION.get());
        skills.add(ExtraSkills.UNIVERSAL_PERCEPTION.get());
        return skills;
    }
}
