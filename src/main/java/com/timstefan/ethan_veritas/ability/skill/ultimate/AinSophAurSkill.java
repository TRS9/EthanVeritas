package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.util.EnergyHelper;
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
 * Grants on learning: Existence Barrier (this addon's extra skill - Multilayer
 * Barrier at 10x maximum health) and the base mod's Spatial Motion for dimensional
 * movement, following the base mod's pattern of composing big skills out of
 * standalone extra skills.
 * <p>
 * Passive [True]: Parallel Existence - the first fatal blow per 5 minutes
 * (2.5 mastered) resolves to 1 HP instead. Defers to Ain once Ain is owned.
 * <p>
 * Active:
 * mode 0 - Infons Manipulation [Press]: rewrite information - silence and weaken the
 * existence in your gaze, unwrite your own afflictions. 2K MP.
 */
public class AinSophAurSkill extends Skill {
    private static final String PARALLEL_EXISTENCE_TIME = "ParallelExistenceTime";
    private static final int MODE_INFONS_MANIPULATION = 0;

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
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        // Existence Barrier lives as its own extra skill (Multilayer Barrier writ divine);
        // dimensional movement comes from the base mod's Spatial Motion instead of a duplicate blink.
        SkillHelper.learnSkill(entity, AllSkills.EXISTENCE_BARRIER.get());
        SkillHelper.learnSkill(entity, ExtraSkills.SPATIAL_MOTION.get());
    }

    // ----- Passives -----

    /** Parallel Existence: the first fatal hit per cooldown window leaves the owner at 1 HP. */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity entity, DamageSource source) {
        // Ain's Absolute Origin replaces this once learned.
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

    // ----- Active -----

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return mode == MODE_INFONS_MANIPULATION ? "ain_soph_aur.infons_manipulation" : super.getModeId(instance, mode);
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return mode == MODE_INFONS_MANIPULATION ? 2_000.0D : 0.0D;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (mode != MODE_INFONS_MANIPULATION) return;
        if (EnergyHelper.isOutOfEnergy(entity, instance, mode)) return;
        boolean mastered = instance.isMastered(entity);

        // Read/write authority over information: silence and weaken the gazed existence,
        // and unwrite the wielder's own afflictions.
        LivingEntity target = AbilityUtils.findLookTarget(entity, 32.0D);
        if (target != null) {
            target.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.SILENCE),
                    mastered ? 200 : 100, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, mastered ? 200 : 100, 0, false, true));
        }
        AbilityUtils.removeHarmfulEffects(entity);
        instance.addMasteryPoint(entity);
        instance.setCoolDown(20, mode); // TESTING: was 200
    }
}
