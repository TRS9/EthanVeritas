package com.timstefan.ethan_veritas.ability.skill.ultimate;

import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Information God: Ain - Stage 5, the self-directed evolution of Ain Soph Aur.
 * <p>
 * Never purchasable: it is learned the moment Ain Soph Aur reaches full mastery,
 * the in-game analog of reverse-engineering ability adjust as a solved
 * engineering problem. Once owned, it supersedes Ain Soph Aur's Existence
 * Barrier and Parallel Existence with elevated versions (the lower skill
 * defers to this one so the two never stack).
 * <p>
 * Actives share one press: Absolute Origin (press) recreates the wielder's own
 * body state from nothing; Absolute Erasure (sneak-press) removes a summoned
 * existence - or failing that, the wielder's own afflictions - without a trace.
 * Omniscient Dominion passively reveals hostile existences through any cover.
 * Peer-tier contests and the End of Space and Time remain outside its reach.
 */
public class AinSkill extends Skill {
    private static final String PARALLEL_EXISTENCE_TIME = "ParallelExistenceTime";
    private static final String ORIGIN_TIME = "AbsoluteOriginTime";
    private static final String ERASURE_TIME = "AbsoluteErasureTime";
    private static final long ACTIVE_COOLDOWN = 12000L; // 10 minutes, per the design document's raid-tier pacing

    public AinSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/ain.png");
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // Not acquirable with EP at any threshold - only granted by mastering Ain Soph Aur.
        return false;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return 0.0D;
    }

    /** Existence Barrier, elevated: 30% reduction of all conventional harm, undispellable. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity entity, DamageSource source, Changeable<Float> damage) {
        damage.set(damage.get() * 0.7F);
        return true;
    }

    /** Parallel Existence, elevated: distributed continuity - fatal hits resolve to 1 HP once per minute. */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity entity, DamageSource source) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(PARALLEL_EXISTENCE_TIME) && now - tag.getLong(PARALLEL_EXISTENCE_TIME) < 1200L) {
            return true;
        }
        tag.putLong(PARALLEL_EXISTENCE_TIME, now);
        instance.markDirty();
        entity.setHealth(1.0F);
        return false;
    }

    /** Omniscient Dominion: hostile existences within the passive domain are always perceived. */
    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (entity.level().isClientSide() || entity.tickCount % 40 != 0) return;
        for (LivingEntity hostile : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(32.0D),
                other -> other instanceof Enemy && other.isAlive())) {
            hostile.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
        }
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;
        if (entity.isShiftKeyDown()) {
            absoluteErasure(instance, entity);
        } else {
            absoluteOrigin(instance, entity);
        }
    }

    /** Absolute Origin: re-declares the wielder's own body from nothing - full restoration. */
    private void absoluteOrigin(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(ORIGIN_TIME) && now - tag.getLong(ORIGIN_TIME) < ACTIVE_COOLDOWN) return;
        tag.putLong(ORIGIN_TIME, now);
        instance.markDirty();

        entity.setHealth(entity.getMaxHealth());
        entity.clearFire();
        removeHarmfulEffects(entity);
        if (entity instanceof Player player) {
            player.getFoodData().setFoodLevel(20);
        }
        instance.addMasteryPoint(entity);
    }

    /**
     * Absolute Erasure: the summoned existence in the wielder's gaze is removed without
     * drops, XP or any other trace. Aimed at nothing erasable, it instead strips every
     * affliction from the wielder. Deliberately summon-only against entities so the
     * skill stays raid-utility rather than PvP-dominant, per the design document.
     */
    private void absoluteErasure(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(ERASURE_TIME) && now - tag.getLong(ERASURE_TIME) < ACTIVE_COOLDOWN) return;

        LivingEntity target = findLookTarget(entity, 32.0D);
        if (target != null && ExistenceStorage.isSummon(target)) {
            target.discard();
        } else {
            removeHarmfulEffects(entity);
        }
        tag.putLong(ERASURE_TIME, now);
        instance.markDirty();
        instance.addMasteryPoint(entity);
    }

    private static void removeHarmfulEffects(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> !effect.value().isBeneficial())
                .toList();
        harmful.forEach(entity::removeEffect);
    }

    private static LivingEntity findLookTarget(LivingEntity entity, double range) {
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
}
