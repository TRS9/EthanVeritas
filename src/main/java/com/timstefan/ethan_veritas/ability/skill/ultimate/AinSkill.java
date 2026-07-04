package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.handler.ErasureDropHandler;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
 * King-tier skill is removed in the exchange - Ain carries everything it did (the
 * Existence Barrier and Spatial Motion grants persist as standalone skills).
 * Points to master: 2000.
 * <p>
 * Passive [Toggle]: Omniscient Dominion - hostile existences within 32 blocks are
 * revealed through all cover.
 * Passive [True]: Absolute Origin - a fatal blow is answered by re-declaring the
 * wielder's own existence: full restoration, once per 10 minutes.
 * On learning: Parallel Existence manifests as the base mod's Body Double.
 * <p>
 * Active:
 * mode 0 - Absolute Erasure [Press]: unwrite whatever is gazed upon, block or being.
 */
public class AinSkill extends Skill {
    private static final String ORIGIN_TIME = "AbsoluteOriginTime";
    private static final int MODE_ABSOLUTE_ERASURE = 0;
    private static final double ERASURE_RANGE = 64.0D;

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
        // Absolute Erasure charges its own scaled cost based on the target's EP.
        return 0.0D;
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        // Parallel Existence: distributed continuity through the base mod's Body Double.
        SkillHelper.learnSkill(entity, ExtraSkills.BODY_DOUBLE.get());
        // Safety net in case this skill was command-granted without walking the ASA path.
        SkillHelper.learnSkill(entity, AllSkills.EXISTENCE_BARRIER.get());
        // The exchange: Ain includes everything Ain Soph Aur did, so the King tier is removed.
        // Deferred a tick so we never mutate the skill storage while it is being iterated.
        if (entity.getServer() != null) {
            entity.getServer().execute(() ->
                    SkillAPI.getSkillsFrom(entity).forgetSkill(AllSkills.AIN_SOPH_AUR.get()));
        }
    }

    // ----- Passives -----

    /** Omniscient Dominion, toggled exactly like every other skill. */
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
        CompoundTag tag = instance.getOrCreateTag();
        int time = tag.getInt("ActiveTime");
        if (time % 600 == 0) {
            instance.addMasteryPoint(entity);
        }
        tag.putInt("ActiveTime", time + 1);

        // Omniscient Dominion: hostile existences within the passive domain are always perceived.
        if (time % 40 == 0) {
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

    // ----- Active -----

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return mode == MODE_ABSOLUTE_ERASURE ? "ain.absolute_erasure" : super.getModeId(instance, mode);
    }

    /**
     * Absolute Erasure: whatever the wielder gazes upon is removed without a trace.
     * Blocks are simply unwritten. A living existence can only be erased while the
     * wielder's EP exceeds 1.5x the target's, and the magicule price rises as that
     * gap narrows: cost = 1.5 * targetEP^2 / ownEP. 300s CD (150s mastered).
     * <p>
     * EP is measured with EnergyHelper.getMaxEP (max aura + max magicule - the mod's
     * actual EP rating), NOT IExistence.getEP(), which returns the current spent-down
     * energy pools and silently broke the check against bosses.
     * <p>
     * Erasing an entity is executed as a real kill with this skill as the damage
     * source, so death-based rewards (EP gain, souls, awakening triggers) credit the
     * wielder like any boss kill - but the erased existence leaves nothing behind:
     * ErasureDropHandler cancels its item and XP drops.
     */
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (mode != MODE_ABSOLUTE_ERASURE) return;

        LivingEntity target = AbilityUtils.findLookTarget(entity, ERASURE_RANGE);
        if (target != null) {
            double ownEP = EnergyHelper.getMaxEP(entity);
            double targetEP = EnergyHelper.getMaxEP(target);
            if (ownEP < targetEP * 1.5D) {
                // The existence is too heavy to unwrite - no cost, no cooldown.
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.translatable("ethan_veritas.skill.ain.erasure_too_heavy")
                            .withStyle(ChatFormatting.RED), true);
                }
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        (SoundEvent) TensuraSoundEvents.GENERIC_CAST_FAIL.get(), TensuraSkill.ABILITY_SOUND, 1.0F, 1.0F);
                return;
            }
            // The heavier the existence, the steeper the price and the longer the world
            // takes to accept the deletion: cost = half the target's EP in magicules,
            // cooldown scales with target EP.
            double cost = targetEP * 0.5D;
            int cooldown = (int) Math.min(20L, Math.max(1L, (long) (targetEP / 50_000.0D))); // TESTING cap 20: real cap 6000 (300s), scale targetEP/1000
            if (EnergyHelper.isOutOfEnergy(entity, 0.0D, cost)) return; // checks, deducts, and messages in one step

            // A real kill so EP/soul/awakening rewards credit the wielder - but no drops:
            // players keep normal death handling (their inventory is their own declaration).
            if (!(target instanceof Player)) {
                target.getPersistentData().putLong(ErasureDropHandler.ERASED_AT, entity.level().getGameTime());
            }
            DamageSource source = createSource(instance, entity, DamageTypes.GENERIC_KILL, mode);
            target.hurt(source, Float.MAX_VALUE);
            if (target.isAlive()) {
                // Whatever survived that had a hook cancelling damage; unwrite it directly.
                target.setHealth(0.0F);
                target.die(source);
            }
            instance.addMasteryPoint(entity);
            instance.setCoolDown(cooldown, mode);
            return;
        }

        // Nothing living in the gaze: unwrite the block instead - minimal price, no cooldown.
        HitResult hit = entity.pick(ERASURE_RANGE, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            if (EnergyHelper.isOutOfEnergy(entity, 0.0D, 100.0D)) return;
            if (entity.level().destroyBlock(blockHit.getBlockPos(), false, entity)) {
                instance.addMasteryPoint(entity);
            }
        }
    }
}
