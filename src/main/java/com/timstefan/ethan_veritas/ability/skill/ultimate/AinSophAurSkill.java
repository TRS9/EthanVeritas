package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Information King: Ain Soph Aur - Stage 4, the convergence of Thoth and Yggdrasil,
 * intrinsic to the Digital Nature race (granted on evolution only while carrying both
 * halves of the Dual Awakening). Points to master: 2000; full mastery evolves it into
 * Information God: Ain, REPLACING this skill.
 * <p>
 * Passive [True]: Existence Barrier - 10% (20% mastered) reduction of all damage,
 * hooked on the damage event itself so nothing can dispel it. Parallel Existence -
 * the first fatal blow per 5 minutes (2.5 mastered) resolves to 1 HP instead.
 * Both defer to Ain's elevated versions once Ain is owned.
 * <p>
 * Active modes (scroll to switch):
 * mode 0 - Dimensional Dominion [Press]: blink 16 blocks (32 mastered).
 * mode 1 - Infons Manipulation [Press]: rewrite information - silence the target's
 * skills and unwrite your own afflictions.
 */
public class AinSophAurSkill extends Skill {
    private static final String PARALLEL_EXISTENCE_TIME = "ParallelExistenceTime";

    private static final int MODE_DIMENSIONAL_DOMINION = 0;
    private static final int MODE_INFONS_MANIPULATION = 1;

    public AinSophAurSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/ain_soph_aur.png");
    }

    @Override
    public int getMaxMastery() {
        return 2000;
    }

    @Override
    public double getDefaultAcquiringMagiculeCost() {
        return 1_000_000.0D;
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // The convergence of Thoth and Yggdrasil, possible only for a Digital Nature existence.
        return SkillUtils.hasSkill(player, AllSkills.THOTH.get())
                && SkillUtils.hasSkill(player, AllSkills.YGGDRASIL.get())
                && ProgressionChecks.isDigitalNature(player);
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        // A Digital Nature existence pays no upkeep; the actives charge their own price.
        return 0.0D;
    }

    // ----- Passives -----

    /** Existence Barrier: always-on damage reduction that no enemy ability can toggle off. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity entity, DamageSource source, Changeable<Float> damage) {
        // Once Ain is acquired, its elevated Existence Barrier takes over entirely - never stack the two.
        if (SkillUtils.hasSkill(entity, AllSkills.AIN.get())) return true;
        float reduction = instance.isMastered(entity) ? 0.8F : 0.9F;
        damage.set(damage.get() * reduction);
        return true;
    }

    /** Parallel Existence: the first fatal hit per cooldown window leaves the owner at 1 HP. */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity entity, DamageSource source) {
        // Ain's distributed continuity replaces this once learned.
        if (SkillUtils.hasSkill(entity, AllSkills.AIN.get())) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        long cooldown = instance.isMastered(entity) ? 3000L : 6000L; // 2.5 or 5 minutes
        if (tag.contains(PARALLEL_EXISTENCE_TIME) && now - tag.getLong(PARALLEL_EXISTENCE_TIME) < cooldown) {
            return true;
        }
        tag.putLong(PARALLEL_EXISTENCE_TIME, now);
        instance.markDirty();
        entity.setHealth(1.0F);
        return false;
    }

    /**
     * Stage 5 trigger: full mastery of Ain Soph Aur is the in-game analog of
     * reverse-engineering ability adjust - it immediately yields Information God: Ain,
     * which removes this skill in the exchange (see AinSkill.onLearnSkill).
     */
    @Override
    public void onSkillMastered(ManasSkillInstance instance, LivingEntity entity) {
        SkillHelper.learnSkill(entity, AllSkills.AIN.get());
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
            case MODE_DIMENSIONAL_DOMINION -> "ain_soph_aur.dimensional_dominion";
            case MODE_INFONS_MANIPULATION -> "ain_soph_aur.infons_manipulation";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;
        boolean mastered = instance.isMastered(entity);
        switch (mode) {
            case MODE_DIMENSIONAL_DOMINION -> {
                // Relocation within recognized space: 16 blocks (32 mastered), 1s between uses.
                if (!AbilityUtils.tryCooldown(instance, entity, "DimensionalDominionTime", 20L)) return;
                if (AbilityUtils.blink(entity, mastered ? 32.0D : 16.0D)) {
                    instance.addMasteryPoint(entity);
                }
            }
            case MODE_INFONS_MANIPULATION -> {
                // Read/write authority over information: silence the gazed existence's skills
                // and unwrite your own afflictions. 10s CD, 2K MP.
                if (!AbilityUtils.tryCooldown(instance, entity, "InfonsManipulationTime", 200L)) return;
                if (!AbilityUtils.payMagicule(entity, 2_000.0D)) return;
                LivingEntity target = AbilityUtils.findLookTarget(entity, 32.0D);
                if (target != null) {
                    target.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.SILENCE),
                            mastered ? 200 : 100, 0, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, mastered ? 200 : 100, 0, false, true));
                }
                AbilityUtils.removeHarmfulEffects(entity);
                instance.addMasteryPoint(entity);
            }
            default -> {
            }
        }
    }
}
