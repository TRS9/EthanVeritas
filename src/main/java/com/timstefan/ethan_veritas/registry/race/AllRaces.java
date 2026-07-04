package com.timstefan.ethan_veritas.registry.race;

import com.timstefan.ethan_veritas.race.DigitalNatureRace;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;

import static com.timstefan.ethan_veritas.Ethan_veritas.MODID;

public class AllRaces {

    // ManasCore races are registered through Architectury's cross-loader DeferredRegister,
    // not the net.neoforged.neoforge.registries one used for blocks/items in the main mod class.
    public static final DeferredRegister<ManasRace> RACES = DeferredRegister.create(MODID, RaceAPI.getRaceRegistryKey());

    // The one genuine race change in the Ethan Veritas progression (design doc 2.5).
    public static final RegistrySupplier<DigitalNatureRace> DIGITAL_NATURE = RACES.register("digital_nature", DigitalNatureRace::new);

    public static void register() {
        RACES.register();
    }
}
