package com.timstefan.ethan_veritas.registry.race;

import com.timstefan.ethan_veritas.race.DigitalNatureRace;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

public class AllRaces {

    // Register directly against ManasCore's own Registrar (RaceAPI.getRaceRegistry()) -
    // see AllSkills for why this replaced our own DeferredRegister + no-arg .register() bind.

    // The one genuine race change in the Ethan Veritas progression (design doc 2.5).
    public static final RegistrySupplier<DigitalNatureRace> DIGITAL_NATURE = register("digital_nature", DigitalNatureRace::new);

    private static <E extends ManasRace> RegistrySupplier<E> register(String name, Supplier<E> ctor) {
        return RaceAPI.getRaceRegistry().register(ResourceLocation.fromNamespaceAndPath(MODID, name), ctor);
    }

    /** No-op body; calling this forces this class's static initializer (and thus registration) to run. */
    public static void init() {
    }
}
