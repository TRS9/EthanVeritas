package com.timstefan.ethan_veritas.registry.skill;

import com.timstefan.ethan_veritas.ability.skill.common.ExampleCommonSkill;
import com.timstefan.ethan_veritas.ability.skill.extra.ExampleExtraSkill;
import com.timstefan.ethan_veritas.ability.skill.intrinsic.ExampleIntrinsicSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.ExampleUltimateSkill;
import com.timstefan.ethan_veritas.ability.skill.unique.ExampleUniqueSkill;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

public class AllSkills {

    public static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(MODID, SkillAPI.getSkillRegistryKey());

    public static final RegistrySupplier<ManasSkill> EXAMPLE_COMMON = SKILLS.register("example_common", ExampleCommonSkill::new);
    public static final RegistrySupplier<ManasSkill> EXAMPLE_EXTRA = SKILLS.register("example_extra", ExampleExtraSkill::new);
    public static final RegistrySupplier<ManasSkill> EXAMPLE_INTRINSIC = SKILLS.register("example_intrinsic", ExampleIntrinsicSkill::new);
    public static final RegistrySupplier<ManasSkill> EXAMPLE_ULTIMATE = SKILLS.register("example_ultimate", ExampleUltimateSkill::new);
    public static final RegistrySupplier<ManasSkill> EXAMPLE_UNIQUE = SKILLS.register("example_unique", ExampleUniqueSkill::new);

    public static void register() {
        SKILLS.register();
    }
}
