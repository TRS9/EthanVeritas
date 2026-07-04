package com.timstefan.ethan_veritas.ability.skill.ultimate;

import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Information King: Ain Soph Aur - intrinsic skill of the Digital Nature race.
 * <p>
 * The design document's "Information God" tier is modeled through the base mod's
 * mastery system rather than a second skill: mastering Ain Soph Aur upgrades every
 * sub-ability (longer blink, stronger barrier, halved Parallel Existence cooldown).
 * Sub-abilities: Dimensional Dominion (blink on press), Existence Barrier (passive,
 * undispellable damage reduction), Parallel Existence (first fatal hit leaves 1 HP).
 */
public class AinSophAurSkill extends Skill {
    private static final String PARALLEL_EXISTENCE_TIME = "ParallelExistenceTime";
    private static final String LAST_WARP_TIME = "LastWarpTime";

    public AinSophAurSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/ultimate/ain_soph_aur.png");
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // Normally learned as the Digital Nature race intrinsic; the EP gate only exists
        // as a deliberate, far-endgame fallback path.
        return ep >= 10_000_000.0D;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        // Digital Nature no longer runs on magicules; the base mod's resource system stays,
        // this skill simply costs nothing and is limited by its own cooldowns.
        return 0.0D;
    }

    /**
     * Existence Barrier: always-on damage reduction that no enemy ability can toggle off,
     * since it hooks the damage event directly instead of using a dispellable effect.
     */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity entity, DamageSource source, Changeable<Float> damage) {
        // Once Ain is acquired, its elevated Existence Barrier takes over entirely - never stack the two.
        if (SkillUtils.hasSkill(entity, AllSkills.AIN.get())) return true;
        float reduction = instance.isMastered(entity) ? 0.8F : 0.9F;
        damage.set(damage.get() * reduction);
        return true;
    }

    /**
     * Parallel Existence: the first fatal hit per cooldown window leaves the owner at 1 HP.
     * Returning false cancels the death (verified against the base mod's UnyieldingSkill,
     * where returning true lets the death proceed).
     */
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
     * reverse-engineering ability adjust - it immediately yields Information God: Ain.
     */
    @Override
    public void onSkillMastered(ManasSkillInstance instance, LivingEntity entity) {
        SkillHelper.learnSkill(entity, AllSkills.AIN.get());
    }

    /**
     * Dimensional Dominion: blink along the look vector to the furthest free spot,
     * with a 1 second delay between uses per the design document.
     */
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (entity.level().isClientSide()) return;

        CompoundTag tag = instance.getOrCreateTag();
        long now = entity.level().getGameTime();
        if (tag.contains(LAST_WARP_TIME) && now - tag.getLong(LAST_WARP_TIME) < 20L) return;

        double range = instance.isMastered(entity) ? 32.0D : 16.0D;
        Vec3 look = entity.getLookAngle();
        for (double distance = range; distance >= 2.0D; distance -= 1.0D) {
            Vec3 target = entity.position().add(look.scale(distance));
            if (entity.level().noCollision(entity, entity.getBoundingBox().move(target.subtract(entity.position())))) {
                entity.teleportTo(target.x(), target.y(), target.z());
                tag.putLong(LAST_WARP_TIME, now);
                instance.markDirty();
                instance.addMasteryPoint(entity);
                return;
            }
        }
    }
}
