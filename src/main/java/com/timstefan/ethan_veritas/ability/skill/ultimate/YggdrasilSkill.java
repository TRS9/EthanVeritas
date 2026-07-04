package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Primal King: Yggdrasil - the elemental/combat half of the Dual Ultimate Skill Awakening.
 * <p>
 * Elemental Command and the defensive sub-abilities from the design document map onto
 * skills the base mod already ships (the elemental Manipulation extras, Multilayer Barrier,
 * Spatial Motion), so learning Yggdrasil grants those instead of duplicating them.
 * Yggdrasil itself carries Natural Resistance and the Sovereign's Dominion ally aura
 * as a toggle.
 */
public class YggdrasilSkill extends Skill {

    public YggdrasilSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/yggdrasil.png");
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // Born from the fusion of all elementals: every Greater Spirit (Flame, Water, Wind,
        // Earth, Space, Light, Darkness) plus the same transcendence gate as Thoth.
        return ProgressionChecks.hasAllGreaterSpirits(player) && ProgressionChecks.isSaintDemonLordOrHero(player);
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        // Elemental Command: apprentice access to the elemental schools the base mod models as Manipulation extras.
        SkillHelper.learnSkill(entity, ExtraSkills.FLAME_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.WATER_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.WIND_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.EARTH_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.LIGHTNING_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.SPATIAL_MANIPULATION.get());
        // Multilayer Barrier and Spatial Authority already exist in the base mod.
        SkillHelper.learnSkill(entity, ExtraSkills.MULTILAYER_BARRIER.get());
        SkillHelper.learnSkill(entity, ExtraSkills.SPATIAL_MOTION.get());
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return 10.0D;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isToggled();
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        // Natural Resistance while active. Vanilla Resistance I (20% all damage) stands in for
        // the design's 25% elemental-only resistance, which has no per-element hook in the base mod.
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));

        // Sovereign's Dominion: allies inside the aura fight harder. Radius doubles once mastered
        // (the design document's Elemental Sovereignty refinement).
        if (entity.tickCount % 20 == 0) {
            double radius = instance.isMastered(entity) ? 20.0D : 10.0D;
            for (LivingEntity ally : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
                    other -> other != entity && other.isAlliedTo(entity))) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false));
            }
        }
    }
}
