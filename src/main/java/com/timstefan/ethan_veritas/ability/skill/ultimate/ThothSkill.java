package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.ability.AbilityUtils;
import com.timstefan.ethan_veritas.ability.ProgressionChecks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import io.github.manasmods.tensura.registry.skill.ExtraSkills;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.registry.block.TensuraBlocks;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
 * (stronger on mastery), shown as Haste while running; no upkeep, mastery grows
 * with active time (base ThoughtAcceleration pattern). Learning or toggling Thoth
 * grants the base mod's information suite.
 * <p>
 * Active modes:
 * mode 0 - Skill Interference [Press]: silence the existence in your gaze. 5K MP.
 * mode 1 - Universal Detect [Press]: reveal every living existence around you. 1K MP.
 */
public class ThothSkill extends Skill {
    private static final ResourceLocation ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_attack_speed");
    private static final ResourceLocation BREAK_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_break_speed");
    private static final ResourceLocation CHANT_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_chant_speed");
    private static final ResourceLocation MOVE_SPEED = ResourceLocation.fromNamespaceAndPath(MODID, "thoth_move_speed");

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
        // Idempotent: re-run on toggle to cover skills obtained via commands (which skip onLearnSkill).
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

    // ----- Passive: Parallel Computation (toggle, no upkeep - base ThoughtAcceleration pattern) -----

    @Override
    public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) {
        return true;
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        grantInformationSuite(entity);
        // Parallel Computation at ultimate scale (reference: other addon ultimates double
        // casting speed and stack reaction buffs).
        boolean mastered = instance.isMastered(entity);
        addModifier(entity.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED, mastered ? 0.80D : 0.40D);
        addModifier(entity.getAttribute(Attributes.BLOCK_BREAK_SPEED), BREAK_SPEED, mastered ? 0.80D : 0.40D);
        addModifier(entity.getAttribute(TensuraAttributes.CHANT_SPEED), CHANT_SPEED, mastered ? 1.00D : 0.50D);
        addModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_SPEED, mastered ? 0.20D : 0.10D);
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
        // Mastery grows with active use, like the base mod's Thought Acceleration.
        CompoundTag tag = instance.getOrCreateTag();
        int time = tag.getInt("ActiveTime");
        if (time % 600 == 0) {
            instance.addMasteryPoint(entity);
        }
        tag.putInt("ActiveTime", time + 1);

        // Visible feedback that Parallel Computation is running.
        if (time % 40 == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, instance.isMastered(entity) ? 1 : 0, true, false, true));
        }
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
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case MODE_SKILL_INTERFERENCE -> 25_000.0D;
            case MODE_UNIVERSAL_DETECT -> 5_000.0D;
            default -> 0.0D;
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        boolean mastered = instance.isMastered(entity);
        switch (mode) {
            case MODE_SKILL_INTERFERENCE -> {
                // Silence the target's skill usage. 8s CD (5s mastered).
                LivingEntity target = AbilityUtils.findLookTarget(entity, 32.0D);
                if (target == null) {
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            (SoundEvent) TensuraSoundEvents.GENERIC_CAST_FAIL.get(), TensuraSkill.ABILITY_SOUND, 1.0F, 1.0F);
                    return;
                }
                if (EnergyHelper.isOutOfEnergy(entity, instance, mode)) return;
                // Ultimate-scale interference: a long silence plus crippled attacks.
                target.addEffect(new MobEffectInstance(TensuraMobEffects.getReference(TensuraMobEffects.SILENCE),
                        mastered ? 600 : 300, 0, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, mastered ? 600 : 300, 1, false, true));
                // The interference made visible: glyphs unravelling around the sealed existence.
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.75D, target.getZ(),
                            60, target.getBbWidth() * 0.8D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.8D, 0.5D);
                    serverLevel.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            25, target.getBbWidth() * 0.6D, target.getBbHeight() * 0.4D, target.getBbWidth() * 0.6D, 0.05D);
                }
                instance.addMasteryPoint(entity);
                instance.setCoolDown(1, mode); // TESTING: was mastered ? 100 : 160
            }
            case MODE_UNIVERSAL_DETECT -> {
                // Radar ping: everything alive within 64 blocks glows through walls for 30s,
                // and Magic Ore veins within 24 blocks are sensed and marked. 10s CD.
                if (EnergyHelper.isOutOfEnergy(entity, instance, mode)) return;
                for (LivingEntity revealed : entity.level().getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(64.0D), other -> other != entity && other.isAlive())) {
                    revealed.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false));
                }
                senseMagicOre(entity);
                instance.addMasteryPoint(entity);
                instance.setCoolDown(1, mode); // TESTING: was 200
            }
            default -> {
            }
        }
    }

    /**
     * Information Dominion over the terrain: Magic Ore within 24 blocks is sensed,
     * marked with light pillars, and reported with the nearest vein's coordinates.
     * (True through-wall block highlighting needs a client shader; the particle
     * markers + coordinates are the honest server-side equivalent.)
     */
    private static void senseMagicOre(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        Block magicOre = (Block) TensuraBlocks.MAGIC_ORE.get();
        Block deepslateMagicOre = (Block) TensuraBlocks.DEEPSLATE_MAGIC_ORE.get();
        BlockPos origin = entity.blockPosition();
        BlockPos nearest = null;
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-24, -24, -24), origin.offset(24, 24, 24))) {
            BlockState state = serverLevel.getBlockState(pos);
            if (!state.is(magicOre) && !state.is(deepslateMagicOre)) continue;
            found++;
            if (nearest == null || pos.distSqr(origin) < nearest.distSqr(origin)) {
                nearest = pos.immutable();
            }
            if (found <= 64) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                        12, 0.1D, 0.8D, 0.1D, 0.02D);
            }
        }
        if (entity instanceof Player player) {
            if (nearest != null) {
                player.displayClientMessage(Component.translatable("ethan_veritas.skill.thoth.ore_sense",
                        found, nearest.getX(), nearest.getY(), nearest.getZ()).withStyle(ChatFormatting.AQUA), true);
            } else {
                player.displayClientMessage(Component.translatable("ethan_veritas.skill.thoth.ore_sense_none")
                        .withStyle(ChatFormatting.GRAY), true);
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
        AttributeInstance moveSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) moveSpeed.removeModifier(MOVE_SPEED);
    }
}
