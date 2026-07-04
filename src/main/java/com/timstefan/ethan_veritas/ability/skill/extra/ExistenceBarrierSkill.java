package com.timstefan.ethan_veritas.ability.skill.extra;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.sound.TensuraSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

/**
 * Existence Barrier - Multilayer Barrier evolved to the Digital Nature tier.
 * <p>
 * Extra skill, never purchasable: granted by Information King: Ain Soph Aur and kept
 * through the exchange into Ain. Structured exactly like the base mod's
 * MultilayerBarrierSkill - a modifier on the same barrier attribute the base mod's
 * damage handling and HUD already understand - but holding 10x maximum health
 * instead of the Multilayer multiplier.
 * <p>
 * [Press] raises the barrier; pressing again drops it.
 */
public class ExistenceBarrierSkill extends Skill {
    protected static final ResourceLocation EXISTENCE_BARRIER = ResourceLocation.fromNamespaceAndPath(MODID, "existence_barrier");
    private static final double BARRIER_MULTIPLIER = 10.0D;

    public ExistenceBarrierSkill() {
        super(SkillType.EXTRA);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/skill/extra/existence_barrier.png");
    }

    @Override
    public boolean checkAcquiringRequirement(Player player, double ep) {
        // Only ever granted by Ain Soph Aur / Ain.
        return false;
    }

    @Override
    public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode) {
        // An authority of a Digital Nature existence: no magicule upkeep.
        return 0.0D;
    }

    @Override
    public void onForgetSkill(ManasSkillInstance instance, LivingEntity entity) {
        super.onForgetSkill(instance, entity);
        AttributeInstance attribute = entity.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);
        if (attribute != null && attribute.getModifier(EXISTENCE_BARRIER) != null) {
            attribute.removeModifier(EXISTENCE_BARRIER);
        }
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        AttributeInstance attribute = entity.getAttribute(TensuraAttributes.MULTILAYER_BARRIER);
        if (attribute == null) return;

        if (attribute.getModifier(EXISTENCE_BARRIER) != null) {
            attribute.removeModifier(EXISTENCE_BARRIER);
            if (attribute.getValue() <= 0.0D) {
                attribute.removeModifiers();
            }
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    (SoundEvent) TensuraSoundEvents.BARRIER_BREAK.get(), TensuraSkill.ABILITY_SOUND, 1.0F, 1.0F);
        } else {
            instance.addMasteryPoint(entity);
            instance.setCoolDown(100, mode);
            attribute.addOrReplacePermanentModifier(new AttributeModifier(EXISTENCE_BARRIER,
                    entity.getMaxHealth() * BARRIER_MULTIPLIER, AttributeModifier.Operation.ADD_VALUE));
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    (SoundEvent) TensuraSoundEvents.DEFENCE_ACTIVATE.get(), TensuraSkill.ABILITY_SOUND, 1.0F, 1.0F);
        }
    }
}
