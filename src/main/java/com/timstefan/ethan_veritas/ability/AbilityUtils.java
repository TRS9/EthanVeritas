package com.timstefan.ethan_veritas.ability;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
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

    /** Per-mode cooldown stored on the skill instance; returns true (and stamps the time) when the ability may fire. */
    public static boolean tryCooldown(ManasSkillInstance instance, LivingEntity entity, String key, long ticks) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(key) && now - tag.getLong(key) < ticks) return false;
        tag.putLong(key, now);
        instance.markDirty();
        return true;
    }

    /** Deducts the magicule price if the wielder can afford it; false means the ability fizzles. */
    public static boolean payMagicule(LivingEntity entity, double cost) {
        if (cost <= 0.0D) return true;
        IExistence existence = TensuraStorages.getExistenceFrom(entity);
        if (existence.getMagicule() < cost) return false;
        existence.setMagicule(existence.getMagicule() - cost);
        return true;
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

    /** Blink along the look vector to the furthest collision-free spot within range; true if moved. */
    public static boolean blink(LivingEntity entity, double range) {
        Vec3 look = entity.getLookAngle();
        for (double distance = range; distance >= 2.0D; distance -= 1.0D) {
            Vec3 target = entity.position().add(look.scale(distance));
            if (entity.level().noCollision(entity, entity.getBoundingBox().move(target.subtract(entity.position())))) {
                entity.teleportTo(target.x(), target.y(), target.z());
                return true;
            }
        }
        return false;
    }

    public static void removeHarmfulEffects(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> !effect.value().isBeneficial())
                .toList();
        harmful.forEach(entity::removeEffect);
    }
}
