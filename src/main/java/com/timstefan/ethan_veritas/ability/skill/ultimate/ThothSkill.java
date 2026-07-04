package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Logos King: Thoth - the information half of the Dual Ultimate Skill Awakening.
 * <p>
 * Obtain: 500K MP, requires the Analyst unique skill + transcendence (Human Saint
 * line, True Demon Lord, or True Hero). Points to master: 2000.
 * <p>
 * Passive [Toggle] Parallel Computation - attack/mining/casting acceleration
 * (stronger on mastery), shown as Haste while running. Learning or first toggling
 * Thoth grants the base mod's information suite (Analytical Appraisal, Thought
 * Acceleration, Magic Sense, Universal Perception, Magic Jamming).
 * <p>
 * Active modes (scroll to switch):
 * mode 0 - Skill Interference [Press]: silence the existence in your gaze.
 * mode 1 - Universal Detect [Press]: reveal every living existence around you.
 */
public class ThothSkill extends Skill {
    private static final ResourceLocation ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_attack_speed");
    private static final ResourceLocation BREAK_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_break_speed");
    private static final ResourceLocation CHANT_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_chant_speed");

    private static final int MODE_SKILL_INTERFERENCE = 0;
    private static final int MODE_UNIVERSAL_DETECT = 1;

    public ThothSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        // The base Skill implementation resolves icons in the "tensura" namespace, so addon skills must override this.
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/thoth.png");
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
        // Analyst evolves into Thoth once its wielder transcends: Human Saint line, True Demon Lord or True Hero.
        return SkillUtils.hasSkill(player, UniqueSkills.ANALYST.get()) && ProgressionChecks.isSaintDemonLordOrHero(player);
    }

    // ----- Sub-skill grants -----

    private static void grantInformationSuite(LivingEntity entity) {
        // Base-mod skills stand in for the design's sub-abilities instead of duplicates.
        // Idempotent: SkillHelper.learnSkill no-ops for skills already known, so this is
        // also re-run on toggle to cover skills obtained via commands (which skip onLearnSkill).
        SkillHelper.learnSkill(entity, ExtraSkills.ANALYTICAL_APPRAISAL.get());
        SkillHelper.learnSkill(entity, ExtraSkills.THOUGHT_ACCELERATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.MAGIC_SENSE.get());
        SkillHelper.learnSkill(entity, ExtraSkills.UNIVERSAL_PERCEPTION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.MAGIC_JAMMING.get());
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        grantInformationSuite(entity);
    }

    // ----- Passive: Parallel Computation (toggle) -----

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        // Toggle upkeep only; the actives charge their own price when pressed.
        return 10.0D;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        grantInformationSuite(entity);
        boolean mastered = instance.isMastered(entity);
        addModifier(entity.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED, mastered ? 0.40D : 0.20D);
        addModifier(entity.getAttribute(Attributes.BLOCK_BREAK_SPEED), BREAK_SPEED, mastered ? 0.40D : 0.20D);
        addModifier(entity.getAttribute(TensuraAttributes.CHANT_SPEED), CHANT_SPEED, mastered ? 0.50D : 0.25D);
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        removeModifiers(entity);
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onForgetSkill(instance, entity);
        removeModifiers(entity);
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) {
        return instance.isToggled();
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        if (entity.level().isClientSide() || entity.tickCount % 40 != 0) return;
        // Visible feedback that Parallel Computation is running.
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, instance.isMastered(entity) ? 1 : 0, true, false, true));
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
            case MODE_SKILL_INTERFERENCE -> "thoth.skill_interference";
            case MODE_UNIVERSAL_DETECT -> "thoth.universal_detect";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;
        boolean mastered = instance.isMastered(entity);
        switch (mode) {
            case MODE_SKILL_INTERFERENCE -> {
                // Silence the target's next skill usage window. 8s CD (5s mastered), 5K MP.
                LivingEntity target = AbilityUtils.findLookTarget(entity, 32.0D);
                if (target == null) return;
                if (!AbilityUtils.tryCooldown(instance, entity, "SkillInterferenceTime", mastered ? 100L : 160L)) return;
                if (!AbilityUtils.payMagicule(entity, 5_000.0D)) return;
                target.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.SILENCE),
                        mastered ? 320 : 160, 0, false, true));
                instance.addMasteryPoint(entity);
            }
            case MODE_UNIVERSAL_DETECT -> {
                // Radar ping: everything alive within 64 blocks glows through walls for 15s. 10s CD, 1K MP.
                if (!AbilityUtils.tryCooldown(instance, entity, "UniversalDetectTime", 200L)) return;
                if (!AbilityUtils.payMagicule(entity, 1_000.0D)) return;
                for (LivingEntity revealed : entity.level().getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(64.0D), other -> other != entity && other.isAlive())) {
                    revealed.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
                }
                instance.addMasteryPoint(entity);
            }
            default -> {
            }
        }
    }

    private static void addModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute != null && !attribute.hasModifier(id)) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeModifiers(LivingEntity entity) {
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(ATTACK_SPEED);
        AttributeInstance breakSpeed = entity.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (breakSpeed != null) breakSpeed.removeModifier(BREAK_SPEED);
        AttributeInstance chantSpeed = entity.getAttribute(TensuraAttributes.CHANT_SPEED);
        if (chantSpeed != null) chantSpeed.removeModifier(CHANT_SPEED);
    }
}
