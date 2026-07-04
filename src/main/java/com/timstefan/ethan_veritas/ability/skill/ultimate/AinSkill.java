package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Information God: Ain - Stage 5, the self-directed evolution of Ain Soph Aur.
 * Never purchasable: granted the moment Ain Soph Aur reaches full mastery, and the
 * King-tier skill is removed in the exchange - Ain carries everything it did.
 * Points to master: 2000.
 * <p>
 * Passive [Toggle]: Existence Barrier - a standing barrier of 10x maximum health
 * (Multilayer Barrier writ divine), re-forming every 5 seconds. Omniscient Dominion -
 * hostile existences within 32 blocks are revealed through all cover.
 * Passive [True]: Absolute Origin - a fatal blow is answered by re-declaring the
 * wielder's own existence: full restoration, once per 10 minutes. Parallel Existence
 * is granted as the base mod's Body Double on learning.
 * <p>
 * Active modes (scroll to switch):
 * mode 0 - Dimensional Dominion [Press]: blink 48 blocks.
 * mode 1 - Absolute Erasure [Press]: unwrite whatever is gazed upon, block or being.
 */
public class AinSkill extends Skill {
    private static final String ORIGIN_TIME = "AbsoluteOriginTime";

    private static final int MODE_DIMENSIONAL_DOMINION = 0;
    private static final int MODE_ABSOLUTE_ERASURE = 1;

    public AinSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/ain.png");
    }

    @Override
    public int getMaxMastery() {
        return 2000;
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

    // ----- Passives -----

    /** Toggled exactly like every other skill. */
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
        if (tag.contains(ORIGIN_TIME) && now - tag.getLong(ORIGIN_TIME) < 12000L) {
            return true;
        }
        tag.putLong(ORIGIN_TIME, now);
        instance.markDirty();
        entity.setHealth(entity.getMaxHealth());
        entity.clearFire();
        AbilityUtils.removeHarmfulEffects(entity);
        if (entity instanceof Player player) {
            player.getFoodData().setFoodLevel(20);
        }
        return false;
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
            case MODE_DIMENSIONAL_DOMINION -> "ain.dimensional_dominion";
            case MODE_ABSOLUTE_ERASURE -> "ain.absolute_erasure";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;
        switch (mode) {
            case MODE_DIMENSIONAL_DOMINION -> {
                if (!AbilityUtils.tryCooldown(instance, entity, "DimensionalDominionTime", 20L)) return;
                if (AbilityUtils.blink(entity, 48.0D)) {
                    instance.addMasteryPoint(entity);
                }
            }
            case MODE_ABSOLUTE_ERASURE -> absoluteErasure(instance, entity);
            default -> {
            }
        }
    }

    /**
     * Absolute Erasure: whatever the wielder gazes upon is removed without a trace.
     * Blocks are simply unwritten. A living existence can only be erased while the
     * wielder's EP exceeds 1.5x the target's, and the magicule price rises as that
     * gap narrows: cost = 1.5 * targetEP^2 / ownEP. 300s CD (150s mastered).
     */
    private void absoluteErasure(ManasSkillInstance instance, LivingEntity entity) {
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        long cooldown = instance.isMastered(entity) ? 3000L : 6000L;
        if (tag.contains("AbsoluteErasureTime") && now - tag.getLong("AbsoluteErasureTime") < cooldown) return;

        LivingEntity target = AbilityUtils.findLookTarget(entity, 64.0D);
        if (target != null) {
            IExistence own = TensuraStorages.getExistenceFrom(entity);
            IExistence other = TensuraStorages.getExistenceFrom(target);
            double ownEP = own.getEP();
            double targetEP = other.getEP();
            if (ownEP < targetEP * 1.5D) return; // the existence is too heavy to unwrite - no cooldown spent
            double cost = ownEP <= 0.0D ? 0.0D : (1.5D * targetEP * targetEP) / ownEP;
            if (!AbilityUtils.payMagicule(entity, cost)) return; // not enough magicules to close the declaration
            if (target instanceof Player) {
                target.kill(); // players die normally rather than being deleted from the world
            } else {
                target.discard(); // no drops, no XP, no trace
            }
            tag.putLong("AbsoluteErasureTime", now);
            instance.markDirty();
            instance.addMasteryPoint(entity);
            return;
        }

        // Nothing living in the gaze: unwrite the block instead, droplessly.
        HitResult hit = entity.pick(64.0D, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            if (entity.level().destroyBlock(blockHit.getBlockPos(), false, entity)) {
                tag.putLong("AbsoluteErasureTime", now);
                instance.markDirty();
                instance.addMasteryPoint(entity);
            }
        }
    }

    private static float existenceBarrierStrength(LivingEntity entity) {
        return entity.getMaxHealth() * 10.0F;
    }
}
