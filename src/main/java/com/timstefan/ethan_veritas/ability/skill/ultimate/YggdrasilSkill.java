package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Primal King: Yggdrasil - the elemental half of the Dual Ultimate Skill Awakening,
 * born from the simultaneous fusion of every Greater Spirit.
 * <p>
 * Obtain: 500K MP, requires all seven Greater Spirits (Flame, Water, Wind, Earth,
 * Space, Light, Darkness) + transcendence. Points to master: 2000.
 * <p>
 * Passive [Toggle]: Natural Resistance (Resistance, Fire Resistance; stronger on
 * mastery) and Sovereign's Dominion - subordinates and allies within 10 blocks
 * (20 mastered) gain Inspiration. Learning or first toggling Yggdrasil grants the
 * base mod's elemental suite (the six Manipulations, Multilayer Barrier, Spatial Motion).
 * <p>
 * Active modes (scroll to switch):
 * mode 0 - Conceptual Emission [Press]: elemental burst around the point in your gaze.
 * mode 1 - Spatial Authority [Press]: short-range blink.
 */
public class YggdrasilSkill extends Skill {
    private static final int MODE_CONCEPTUAL_EMISSION = 0;
    private static final int MODE_SPATIAL_AUTHORITY = 1;

    public YggdrasilSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/yggdrasil.png");
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 500_000.0D;
    }

    @Override
    public int getMaxMastery() {
        return 2000;
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // Born from the fusion of all elementals: every Greater Spirit plus the same transcendence gate as Thoth.
        return ProgressionChecks.hasAllGreaterSpirits(player) && ProgressionChecks.isSaintDemonLordOrHero(player);
    }

    // ----- Sub-skill grants -----

    private static void grantElementalSuite(LivingEntity entity) {
        // Idempotent; re-run on toggle to also cover command-granted copies of this skill.
        SkillHelper.learnSkill(entity, ExtraSkills.FLAME_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.WATER_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.WIND_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.EARTH_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.LIGHTNING_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.SPATIAL_MANIPULATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.MULTILAYER_BARRIER.get());
        SkillHelper.learnSkill(entity, ExtraSkills.SPATIAL_MOTION.get());
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        grantElementalSuite(entity);
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        grantElementalSuite(entity);
    }

    // ----- Passive: Natural Resistance + Sovereign's Dominion (toggle) -----

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
        boolean mastered = instance.isMastered(entity);

        if (entity.tickCount % 40 == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, mastered ? 1 : 0, true, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
        }

        // Sovereign's Dominion: Inspiration for everyone fighting under this aura.
        if (entity.tickCount % 20 == 0) {
            double radius = mastered ? 20.0D : 10.0D;
            for (LivingEntity ally : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
                    other -> other != entity && other.isAlliedTo(entity))) {
                ally.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.INSPIRATION),
                        60, mastered ? 3 : 1, true, false, true));
            }
        }
    }

    // ----- Active modes -----

    @Override
    public int getModes(ManasSkillInstance instance) {
        return 2;
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return (mode + (reverse ? -1 : 1) + getModes(instance)) % getModes(instance);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case MODE_CONCEPTUAL_EMISSION -> "yggdrasil.conceptual_emission";
            case MODE_SPATIAL_AUTHORITY -> "yggdrasil.spatial_authority";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;
        boolean mastered = instance.isMastered(entity);
        switch (mode) {
            case MODE_CONCEPTUAL_EMISSION -> {
                // Fused-element burst centered 6 blocks along the gaze: magic damage + ignition. 5s CD, 10K MP.
                if (!AbilityUtils.tryCooldown(instance, entity, "ConceptualEmissionTime", 100L)) return;
                if (!AbilityUtils.payMagicule(entity, 10_000.0D)) return;
                Vec3 center = entity.getEyePosition().add(entity.getLookAngle().scale(6.0D));
                float damage = mastered ? 20.0F : 12.0F;
                for (LivingEntity victim : entity.level().getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(12.0D),
                        other -> other != entity && !other.isAlliedTo(entity) && other.position().distanceTo(center) <= 5.0D)) {
                    victim.hurt(entity.damageSources().indirectMagic(entity, entity), damage);
                    victim.setRemainingFireTicks(60);
                }
                instance.addMasteryPoint(entity);
            }
            case MODE_SPATIAL_AUTHORITY -> {
                // 8-block blink (12 mastered). 5s CD, 500 MP.
                if (!AbilityUtils.tryCooldown(instance, entity, "SpatialAuthorityTime", 100L)) return;
                if (!AbilityUtils.payMagicule(entity, 500.0D)) return;
                if (AbilityUtils.blink(entity, mastered ? 12.0D : 8.0D)) {
                    instance.addMasteryPoint(entity);
                }
            }
            default -> {
            }
        }
    }
}
