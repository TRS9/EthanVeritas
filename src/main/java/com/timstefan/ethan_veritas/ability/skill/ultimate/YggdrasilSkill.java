package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.ResistanceSkills;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * mastery) and Sovereign's Dominion - allies within 10 blocks (20 mastered) gain
 * Inspiration. Learning or toggling Yggdrasil grants the base mod's elemental suite:
 * the six Manipulations, Multilayer Barrier, Spatial Motion (spatial movement comes
 * from the base skill rather than a duplicate here), and the elemental resistances.
 * <p>
 * Active modes:
 * mode 0 - Conceptual Emission [Press]: fused-element burst at the point in your gaze. 50K MP.
 * mode 1 - Primal Tempest [Hold]: a storm of all elements raging around the wielder
 * for as long as the key is held (up to 20s, 30s mastered); releasing disperses it.
 * Range, duration and damage grow with mastery. 5K MP per second.
 */
public class YggdrasilSkill extends Skill {
    private static final int MODE_CONCEPTUAL_EMISSION = 0;
    private static final int MODE_PRIMAL_TEMPEST = 1;

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
        SkillHelper.learnSkill(entity, ResistanceSkills.COLD_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.DARKNESS_ATTACK_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.EARTH_ATTACK_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.ELECTRICITY_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.FLAME_ATTACK_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.HEAT_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.LIGHT_ATTACK_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.SPATIAL_ATTACK_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.THERMAL_FLUCTUATION_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.WATER_ATTACK_RESISTANCE.get());
        SkillHelper.learnSkill(entity, ResistanceSkills.WIND_ATTACK_RESISTANCE.get());
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

    // ----- Passive: Natural Resistance + Sovereign's Dominion (toggle, no upkeep) -----

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isToggled();
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        // NOTE: ManasCore only calls onTick every 100 game ticks (5s) - fine for
        // these slow passives, useless for anything continuous (see onHeld below).
        boolean mastered = instance.isMastered(entity);

        // Mastery grows with active use, like the base mod's Thought Acceleration.
        CompoundTag tag = instance.getOrCreateTag();
        int time = tag.getInt("ActiveTime");
        if (time % 6 == 0) {
            instance.addMasteryPoint(entity);
        }
        tag.putInt("ActiveTime", time + 1);

        // Natural Resistance at ultimate scale: 40% (60% mastered) plus immunity to fire.
        // Durations bridge the 5s gap between onTick invocations.
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, mastered ? 2 : 1, true, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false, true));

        // Sovereign's Dominion: Inspiration for everyone fighting under this aura.
        double radius = mastered ? 20.0D : 10.0D;
        for (LivingEntity ally : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius),
                other -> other != entity && other.isAlliedTo(entity))) {
            ally.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.INSPIRATION),
                    120, mastered ? 3 : 1, true, false, true));
        }
    }

    // ----- Primal Tempest (HELD, the Mortal Fear / Destroyer Haki pattern) -----
    // onHeld runs every single game tick while the ability key is held, unlike
    // onTick; releasing the key ends the storm and starts the cooldown.

    @Override
    public boolean shouldTriggerReleaseOnHeldInterrupt(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        return mode == MODE_PRIMAL_TEMPEST;
    }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isMastered(entity) ? 600 : 400; // 30s mastered, 20s not
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (mode != MODE_PRIMAL_TEMPEST) return false;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return false;
        boolean mastered = instance.isMastered(entity);
        if (heldTicks >= getMaxHeldTime(instance, entity)) return false;

        // Upkeep drained once a second (getMagiculeCost for this mode), Mortal Fear style.
        if (heldTicks % 20 == 0 && EnergyHelper.isOutOfEnergy(entity, instance, mode)) return false;
        if (heldTicks > 0 && heldTicks % 100 == 0) {
            instance.addMasteryPoint(entity);
        }

        double radius = mastered ? 10.0D : 6.0D;

        // All elements raging at once: a rising spiral of flame, frost, lightning,
        // wind and light circling the wielder.
        for (int arm = 0; arm < 6; arm++) {
            double angle = Math.toRadians((heldTicks * 14 + arm * 60) % 360);
            double armRadius = 1.5D + (arm % 3) * (radius - 1.5D) / 3.0D;
            double x = entity.getX() + Math.cos(angle) * armRadius;
            double z = entity.getZ() + Math.sin(angle) * armRadius;
            double y = entity.getY() + 0.3D + arm * 0.7D;
            SimpleParticleType type = switch (arm % 5) {
                case 0 -> ParticleTypes.FLAME;
                case 1 -> ParticleTypes.SNOWFLAKE;
                case 2 -> ParticleTypes.ELECTRIC_SPARK;
                case 3 -> ParticleTypes.CLOUD;
                default -> ParticleTypes.END_ROD;
            };
            serverLevel.sendParticles(type, x, y, z, 3, 0.15D, 0.15D, 0.15D, 0.02D);
        }

        // The storm bites twice a second: fused-element damage plus a deep chill.
        if (heldTicks % 10 == 0) {
            float damage = mastered ? 40.0F : 20.0F;
            for (LivingEntity victim : entity.level().getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(radius),
                    other -> other != entity && !other.isAlliedTo(entity) && other.isAlive()
                            && other.distanceTo(entity) <= radius)) {
                victim.hurt(entity.damageSources().indirectMagic(entity, entity), damage);
                victim.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.CHILL),
                        60, mastered ? 1 : 0, false, true));
            }
        }
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (mode == MODE_PRIMAL_TEMPEST && heldTicks > 0) {
            instance.setCoolDown(10, mode); // TESTING: cooldown is in SECONDS; tune for release
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
            case MODE_PRIMAL_TEMPEST -> "yggdrasil.primal_tempest";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case MODE_CONCEPTUAL_EMISSION -> 50_000.0D;
            case MODE_PRIMAL_TEMPEST -> 5_000.0D; // drained per second while the storm is held
            default -> 0.0D;
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        // Instances saved before Primal Tempest existed carry a 1-slot cooldown list in
        // their NBT; pad it so mode-1 cooldowns actually register.
        AbilityUtils.ensureCooldownCapacity(instance, getModes(instance));
        boolean mastered = instance.isMastered(entity);
        switch (mode) {
            case MODE_CONCEPTUAL_EMISSION -> {
                if (EnergyHelper.isOutOfEnergy(entity, instance, mode)) return;

                // Fused-element burst centered along the gaze, at peer-ultimate scale
                // (reference: Absolute Severance 700/1400, Nova Break 5000): heavy magic
                // damage, ignition, and Fragility shredding the survivors' defenses.
                Vec3 center = entity.getEyePosition().add(entity.getLookAngle().scale(8.0D));
                float damage = mastered ? 600.0F : 300.0F;
                double radius = mastered ? 8.0D : 6.0D;
                for (LivingEntity victim : entity.level().getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(20.0D),
                        other -> other != entity && !other.isAlliedTo(entity) && other.position().distanceTo(center) <= radius)) {
                    victim.hurt(entity.damageSources().indirectMagic(entity, entity), damage);
                    victim.setRemainingFireTicks(100);
                    victim.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.FRAGILITY),
                            200, mastered ? 1 : 0, false, true));
                }
                // Every element detonating at once, not just a puff of fire.
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x(), center.y(), center.z(), 2, 0.5D, 0.5D, 0.5D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.FLAME, center.x(), center.y(), center.z(), 80, radius * 0.5D, radius * 0.4D, radius * 0.5D, 0.15D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x(), center.y(), center.z(), 60, radius * 0.5D, radius * 0.4D, radius * 0.5D, 0.3D);
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x(), center.y(), center.z(), 50, radius * 0.5D, radius * 0.4D, radius * 0.5D, 0.1D);
                    serverLevel.sendParticles(ParticleTypes.END_ROD, center.x(), center.y(), center.z(), 40, radius * 0.4D, radius * 0.3D, radius * 0.4D, 0.1D);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, center.x(), center.y(), center.z(), 40, radius * 0.5D, radius * 0.3D, radius * 0.5D, 0.2D);
                }
                instance.addMasteryPoint(entity);
                instance.setCoolDown(1, mode); // TESTING: cooldown is in SECONDS; tune for release
            }
            // MODE_PRIMAL_TEMPEST is a held ability: see onHeld/onRelease.
            default -> {
            }
        }
    }
}
