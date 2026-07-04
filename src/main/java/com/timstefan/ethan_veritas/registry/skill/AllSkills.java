package com.timstefan.ethan_veritas.registry.skill;

import com.timstefan.ethan_veritas.ability.skill.ultimate.AinSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.AinSophAurSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.ThothSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.YggdrasilSkill;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

public class AllSkills {

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(MODID, SkillAPI.getSkillRegistryKey());

    // The Dual Ultimate Skill Awakening pair (design doc 2.2). Entry-tier content
    // (Analyst, Analytical Appraisal, Thought Acceleration) already exists in the
    // base mod and is required by these skills instead of being re-registered here.
    public static final RegistrySupplier<ThothSkill> THOTH = SKILLS.register("thoth", ThothSkill::new);
    public static final RegistrySupplier<YggdrasilSkill> YGGDRASIL = SKILLS.register("yggdrasil", YggdrasilSkill::new);

    // Digital Nature race intrinsic (Stage 4).
    public static final RegistrySupplier<AinSophAurSkill> AIN_SOPH_AUR = SKILLS.register("ain_soph_aur", AinSophAurSkill::new);

    // Information God tier (Stage 5) - granted automatically by mastering Ain Soph Aur, never purchasable.
    public static final RegistrySupplier<AinSkill> AIN = SKILLS.register("ain", AinSkill::new);

    public static void register() {
        SKILLS.register();
    }
}
