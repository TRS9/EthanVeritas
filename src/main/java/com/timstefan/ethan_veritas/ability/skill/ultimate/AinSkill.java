package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Information God: Ain - Stage 5, the self-directed evolution of Ain Soph Aur.
 * <p>
 * Never purchasable: learning it happens the moment Ain Soph Aur reaches full
 * mastery, and the King-tier skill is removed in the exchange - Ain carries
 * everything it did. Toggled like any other skill (ALT + slot).
 * <p>
 * While toggled: Existence Barrier (a standing barrier of 10x maximum health,
 * Multilayer Barrier writ divine) and Omniscient Dominion (hostiles revealed
 * through all cover). Press: Dimensional Dominion blink. Sneak-press: Absolute
 * Erasure - erases whatever is gazed upon, block or existence; erasing a living
 * existence demands 1.5x its EP and burns magicules that grow as the gap
 * narrows. Absolute Origin re-declares the wielder's own existence when a
 * fatal blow lands. Parallel Existence is granted as the base mod's Body
 * Double skill on learning.
 */
public class AinSkill extends Skill {
    private static final String ORIGIN_TIME = "AbsoluteOriginTime";
    private static final String ERASURE_TIME = "AbsoluteErasureTime";
    private static final String LAST_WARP_TIME = "LastWarpTime";
    private static final long ACTIVE_COOLDOWN = 12000L; // 10 minutes
    private static final double ERASURE_RANGE = 64.0D;
    private static final double BLINK_RANGE = 48.0D;

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
        // Base upkeep is nothing; Absolute Erasure charges its own scaled cost directly.
        return 0.0D;
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        // Parallel Existence: distributed continuity through the base mod's Body Double.
        SkillHelper.learnSkill(entity, ExtraSkills.BODY_DOUBLE.get());
        // The exchange: Ain includes everything Ain Soph Aur did, so the King tier is removed.
        // Deferred a tick so we never mutate the skill storage while it is being iterated.
        if (entity.getServer() != null) {
            entity.getServer().execute(() ->
                    SkillAPI.getSkillsFrom(entity).forgetSkill(AllSkills.AIN_SOPH_AUR.get()));
        }
    }

    /** Toggled exactly like every other skill (ALT + slot). */
    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        entity.setAbsorptionAmount(Math.max(entity.getAbsorptionAmount(), existenceBarrierStrength(entity)));
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        entity.setAbsorptionAmount(0.0F);
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isToggled();
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (entity.level().isClientSide()) return;

        // Existence Barrier: a standing barrier of 10x maximum health that re-forms every 5 seconds.
        if (entity.tickCount % 100 == 0 && entity.getAbsorptionAmount() < existenceBarrierStrength(entity)) {
            entity.setAbsorptionAmount(existenceBarrierStrength(entity));
        }

        // Omniscient Dominion: hostile existences within the passive domain are always perceived.
        if (entity.tickCount % 40 == 0) {
            for (LivingEntity hostile : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(32.0D),
                    other -> other instanceof Enemy && other.isAlive())) {
                hostile.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
            }
        }
    }

    /** Absolute Origin: a fatal blow is answered by re-declaring the wielder's own existence from nothing. */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity entity, DamageSource source) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(ORIGIN_TIME) && now - tag.getLong(ORIGIN_TIME) < ACTIVE_COOLDOWN) {
            return true;
        }
        tag.putLong(ORIGIN_TIME, now);
        instance.markDirty();
        entity.setHealth(entity.getMaxHealth());
        entity.clearFire();
        removeHarmfulEffects(entity);
        if (entity instanceof Player player) {
            player.getFoodData().setFoodLevel(20);
        }
        return false;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;
        if (entity.isShiftKeyDown()) {
            absoluteErasure(instance, entity);
        } else {
            dimensionalDominion(instance, entity);
        }
    }

    /** Dimensional Dominion: relocation within recognized space, carried over from Ain Soph Aur. */
    private void dimensionalDominion(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(LAST_WARP_TIME) && now - tag.getLong(LAST_WARP_TIME) < 20L) return;

        Vec3 look = entity.getLookAngle();
        for (double distance = BLINK_RANGE; distance >= 2.0D; distance -= 1.0D) {
            Vec3 target = entity.position().add(look.scale(distance));
            if (entity.level().noCollision(entity, entity.getBoundingBox().move(target.subtract(entity.position())))) {
                entity.teleportTo(target.x(), target.y(), target.z());
                tag.putLong(LAST_WARP_TIME, now);
                instance.markDirty();
                return;
            }
        }
    }

    /**
     * Absolute Erasure: whatever the wielder gazes upon is removed without a trace.
     * Blocks are simply unwritten. A living existence can only be erased while the
     * wielder's EP exceeds 1.5x the target's, and the magicule price rises as that
     * gap narrows: cost = 1.5 * targetEP^2 / ownEP.
     */
    private void absoluteErasure(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(ERASURE_TIME) && now - tag.getLong(ERASURE_TIME) < ACTIVE_COOLDOWN) return;

        LivingEntity target = findLookTarget(entity, ERASURE_RANGE);
        if (target != null) {
            IExistence own = TensuraStorages.getExistenceFrom(entity);
            IExistence other = TensuraStorages.getExistenceFrom(target);
            double ownEP = own.getEP();
            double targetEP = other.getEP();
            if (ownEP < targetEP * 1.5D) return; // the existence is too heavy to unwrite - no cooldown spent
            double cost = ownEP <= 0.0D ? 0.0D : (1.5D * targetEP * targetEP) / ownEP;
            if (own.getMagicule() < cost) return; // not enough magicules to close the declaration
            own.setMagicule(own.getMagicule() - cost);
            if (target instanceof Player) {
                target.kill(); // players die normally rather than being deleted from the world
            } else {
                target.discard(); // no drops, no XP, no trace
            }
            tag.putLong(ERASURE_TIME, now);
            instance.markDirty();
            instance.addMasteryPoint(entity);
            return;
        }

        // Nothing living in the gaze: unwrite the block instead, droplessly.
        HitResult hit = entity.pick(ERASURE_RANGE, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            if (entity.level().destroyBlock(blockHit.getBlockPos(), false, entity)) {
                tag.putLong(ERASURE_TIME, now);
                instance.markDirty();
                instance.addMasteryPoint(entity);
            }
        }
    }

    private static float existenceBarrierStrength(LivingEntity entity) {
        return entity.getMaxHealth() * 10.0F;
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
