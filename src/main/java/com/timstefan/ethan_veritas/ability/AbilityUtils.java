package com.timstefan.ethan_veritas.ability;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Shared plumbing for this addon's skill actives. */
public final class AbilityUtils {

    private AbilityUtils() {
    }

    /** The living entity in the wielder's gaze, or null. */
    public static LivingEntity findLookTarget(LivingEntity entity, double range) {
        Vec3 start = entity.getEyePosition();
        Vec3 direction = entity.getLookAngle().scale(range);
        Vec3 end = start.add(direction);
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : entity.level().getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().expandTowards(direction).inflate(1.0D),
                other -> other != entity && other.isAlive())) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isPresent()) {
                double distance = hit.get().distanceToSqr(start);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = candidate;
                }
            }
        }
        return closest;
    }

    public static void removeHarmfulEffects(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> !effect.value().isBeneficial())
                .toList();
        harmful.forEach(entity::removeEffect);
    }

    /** Unwrites every beneficial effect - Infons-level removal of a target's active advantages. */
    public static void removeBeneficialEffects(LivingEntity entity) {
        List<Holder<MobEffect>> beneficial = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.value().isBeneficial())
                .toList();
        beneficial.forEach(entity::removeEffect);
    }

    /**
     * Upgrades every acquired resistance skill to its nullification - the exact
     * routine the base mod runs on True Demon Lord / True Hero awakening
     * (RaceHelper.awakening), reused for the Digital Nature evolution and the
     * acquisition of Information God: Ain.
     */
    public static void upgradeResistancesToNullifications(LivingEntity entity) {
        Skills storage = SkillAPI.getSkillsFrom(entity);
        for (ManasSkillInstance instance : List.copyOf(storage.getLearnedSkills())) {
            if (instance.getSkill() instanceof ResistSkill resistSkill) {
                storage.getSkill(resistSkill).ifPresent(skillInstance -> resistSkill.evolveToNullification(skillInstance, entity));
            }
        }
    }
}
