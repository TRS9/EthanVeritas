package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Logos King: Thoth - the information/utility half of the Dual Ultimate Skill Awakening.
 * <p>
 * Where the base mod already implements a sub-ability from the design document
 * (Analytical Appraisal, Universal Detect -> Magic Sense/Universal Perception,
 * Skill Interference -> Magic Jamming, Thought Acceleration), learning Thoth grants
 * the base mod's skill instead of duplicating it. Thoth itself only carries the
 * Parallel Computation toggle (cognition/attack/cast speed).
 */
public class ThothSkill extends Skill {
    private static final ResourceLocation ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_attack_speed");
    private static final ResourceLocation BREAK_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_break_speed");
    private static final ResourceLocation CHANT_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_chant_speed");

    public ThothSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        // The base Skill implementation resolves icons in the "tensura" namespace, so addon skills must override this.
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/thoth.png");
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // Analyst evolves into Thoth once its wielder transcends: Human Saint line, True Demon Lord or True Hero.
        return SkillUtils.hasSkill(player, UniqueSkills.ANALYST.get()) && ProgressionChecks.isSaintDemonLordOrHero(player);
    }

    @Override
    public void onLearnSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onLearnSkill(instance, entity);
        SkillHelper.learnSkill(entity, ExtraSkills.ANALYTICAL_APPRAISAL.get());
        SkillHelper.learnSkill(entity, ExtraSkills.THOUGHT_ACCELERATION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.MAGIC_SENSE.get());
        SkillHelper.learnSkill(entity, ExtraSkills.UNIVERSAL_PERCEPTION.get());
        SkillHelper.learnSkill(entity, ExtraSkills.MAGIC_JAMMING.get());
    }

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return 5.0D;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        addModifier(entity.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED, 0.10D);
        addModifier(entity.getAttribute(Attributes.BLOCK_BREAK_SPEED), BREAK_SPEED, 0.10D);
        addModifier(entity.getAttribute(TensuraAttributes.CHANT_SPEED), CHANT_SPEED, instance.isMastered(entity) ? 0.25D : 0.15D);
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
