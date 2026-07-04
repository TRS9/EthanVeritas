package com.timstefan.ethan_veritas.ability;

import com.timstefan.ethan_veritas.race.DigitalNatureRace;
import com.timstefan.ethan_veritas.registry.skill.AllSkills;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.ability.SkillUtils;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.ability.magic.spiritual.SpiritualMagic;
import io.github.manasmods.tensura.race.human.HumanSaintRace;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.storage.spirit.ISpiritWielder;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Shared unlock conditions for the Ethan Veritas progression. */
public final class ProgressionChecks {

    /** The seven elemental spirits required for Yggdrasil (the design's eight-fold fusion, as the base mod models elements). */
    private static final List<Element> YGGDRASIL_ELEMENTS = List.of(
            Element.FLAME, Element.WATER, Element.WIND, Element.EARTH,
            Element.SPACE, Element.LIGHT, Element.DARKNESS);

    private ProgressionChecks() {
    }

    /** Stage-2 transcendence gate: Human Saint line (includes Divine Human), True Demon Lord, or True Hero. */
    public static boolean isSaintDemonLordOrHero(LivingEntity entity) {
        IExistence existence = TensuraStorages.getExistenceFrom(entity);
        if (existence.isTrueDemonLord() || existence.isTrueHero()) return true;
        return RaceAPI.getRaceFrom(entity).getRace()
                .map(instance -> instance.getRace() instanceof HumanSaintRace)
                .orElse(false);
    }

    /** True when every Yggdrasil element is held at Greater spirit level or above. */
    public static boolean hasAllGreaterSpirits(LivingEntity entity) {
        ISpiritWielder spirits = TensuraStorages.getSpiritFrom(entity);
        for (Element element : YGGDRASIL_ELEMENTS) {
            SpiritualMagic.SpiritLevel level = spirits.getSpiritLevel(element);
            if (level != SpiritualMagic.SpiritLevel.GREATER && level != SpiritualMagic.SpiritLevel.LORD) {
                return false;
            }
        }
        return true;
    }

    public static boolean isDigitalNature(LivingEntity entity) {
        return RaceAPI.getRaceFrom(entity).getRace()
                .map(instance -> instance.getRace() instanceof DigitalNatureRace)
                .orElse(false);
    }

    /**
     * The Digital Nature gate, checked from every race's evolution list: an
     * analytical-department ULTIMATE skill (currently Thoth - the base mod ships no
     * ultimates of its own in 2.0). The EP side of the gate is NOT checked here:
     * it lives in the race's own EvolutionRequirements, so the evolution screen can
     * show it as a proper progress bar instead of silently hiding the option.
     */
    public static boolean canReachDigitalNature(LivingEntity entity) {
        return SkillUtils.hasSkill(entity, AllSkills.THOTH.get());
    }
}
