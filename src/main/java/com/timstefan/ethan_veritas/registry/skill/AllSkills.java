package com.timstefan.ethan_veritas.registry.skill;

import com.timstefan.ethan_veritas.ability.skill.ultimate.AinSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.AinSophAurSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.ThothSkill;
import com.timstefan.ethan_veritas.ability.skill.ultimate.YggdrasilSkill;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

public class AllSkills {

    // Register directly against ManasCore's own Registrar (SkillAPI.getSkillRegistry()),
    // exactly like Tensura's own UniqueSkills/CommonSkills/etc. do internally. Creating our
    // own architectury DeferredRegister and calling its no-arg .register() bind - the
    // previous approach - eagerly resolves the "manascore_skill:skills" registry by key,
    // which crashes with "Registry ... does not exist!" if it hasn't been created yet
    // (mod construction order between separate modids isn't reliably guaranteed).
    // SkillAPI.getSkillRegistry() just returns that already-created Registrar directly, so
    // there's no lookup-by-key race to lose.

    public static final RegistrySupplier<ThothSkill> THOTH = register("thoth", ThothSkill::new);
    public static final RegistrySupplier<YggdrasilSkill> YGGDRASIL = register("yggdrasil", YggdrasilSkill::new);

    // Digital Nature race intrinsic (Stage 4).
    public static final RegistrySupplier<AinSophAurSkill> AIN_SOPH_AUR = register("ain_soph_aur", AinSophAurSkill::new);

    // Information God tier (Stage 5) - granted automatically by mastering Ain Soph Aur, never purchasable.
    public static final RegistrySupplier<AinSkill> AIN = register("ain", AinSkill::new);

    private static <E extends ManasSkill> RegistrySupplier<E> register(String name, Supplier<E> ctor) {
        return SkillAPI.getSkillRegistry().register(ResourceLocation.fromNamespaceAndPath(MODID, name), ctor);
    }

    /** No-op body; calling this forces this class's static initializer (and thus registration) to run. */
    public static void init() {
    }
}
