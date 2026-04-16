package net.tntim1.psychic.fluids;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Consumer;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, "psychic");
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "psychic");

    public enum Essence {
        ENERGIA  ("energia_essence_fluid",  0xFFFFAA00),
        KYKLOS   ("kyklos_essence_fluid",   0xFF00CCFF),
        METABOLE ("metabole_essence_fluid", 0xFF00FF88),
        MISOS    ("misos_essence_fluid",    0xFFFF2200),
        MOUSIKE  ("mousike_essence_fluid",  0xFFDD44FF),
        PHTHORA  ("phthora_essence_fluid",  0xFF443300),
        TECHNE   ("techne_essence_fluid",   0xFF88AACC),
        THYMOS   ("thymos_essence_fluid",   0xFFFF6600);

        public final String name;
        public final int color;
        Essence(String name, int color) { this.name = name; this.color = color; }
    }

    public static final RegistryObject<FlowingFluid>[] SOURCES  = new RegistryObject[8];
    public static final RegistryObject<FlowingFluid>[] FLOWINGS = new RegistryObject[8];
    public static final RegistryObject<FluidType>[]    TYPES    = new RegistryObject[8];

    public static boolean isPsychicFluid(Fluid fluid) {
        for (var src : SOURCES)  if (src != null && fluid == src.get())  return true;
        for (var flo : FLOWINGS) if (flo != null && fluid == flo.get())  return true;
        return false;
    }

    static {
        Essence[] values = Essence.values();
        for (int i = 0; i < values.length; i++) {
            final Essence essence = values[i];
            final int idx = i;
            final int color = essence.color;

            TYPES[i] = FLUID_TYPES.register(essence.name + "_type", () ->
                    new FluidType(FluidType.Properties.create()
                            .density(900)
                            .viscosity(900)
                            .lightLevel(6)) {
                        @Override
                        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                            consumer.accept(new IClientFluidTypeExtensions() {
                                @Override
                                public ResourceLocation getStillTexture() {
                                    return new ResourceLocation("minecraft:block/water_still");
                                }
                                @Override
                                public ResourceLocation getFlowingTexture() {
                                    return new ResourceLocation("minecraft:block/water_flow");
                                }
                                @Override
                                public int getTintColor() {
                                    return color;
                                }
                            });
                        }
                    });

            final ForgeFlowingFluid.Properties[] props = new ForgeFlowingFluid.Properties[1];

            SOURCES[i] = FLUIDS.register(essence.name + "_source", () -> {
                props[0] = buildProps(essence, idx);
                return new ForgeFlowingFluid.Source(props[0]);
            });
            FLOWINGS[i] = FLUIDS.register(essence.name + "_flowing", () ->
                    new ForgeFlowingFluid.Flowing(props[0]));
        }
    }

    private static ForgeFlowingFluid.Properties buildProps(Essence essence, int idx) {
        return new ForgeFlowingFluid.Properties(
                TYPES[idx],
                SOURCES[idx],
                FLOWINGS[idx])
                .slopeFindDistance(0)
                .levelDecreasePerBlock(8);
    }
}