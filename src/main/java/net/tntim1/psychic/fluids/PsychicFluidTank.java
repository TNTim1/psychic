package net.tntim1.psychic.fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class PsychicFluidTank extends FluidTank {

    private Runnable onFill;
    private Runnable onDrain;

    public PsychicFluidTank(int capacity) {
        // Accept any psychic essence, reject everything else
        super(capacity, stack -> ModFluids.isPsychicFluid(stack.getFluid()));
    }

    public PsychicFluidTank onFill(Runnable r)  { this.onFill  = r; return this; }
    public PsychicFluidTank onDrain(Runnable r) { this.onDrain = r; return this; }

    public int fillWithCallback(FluidStack resource, FluidAction action) {
        // Enforce: can't mix essences
        if (!fluid.isEmpty() && !fluid.isFluidEqual(resource)) return 0;
        int filled = super.fill(resource, action);
        if (filled > 0 && action.execute() && onFill != null) onFill.run();
        return filled;
    }
    public boolean isFull() {
        return getFluidAmount() >= getCapacity();
    }

    public FluidStack drainWithCallback(int maxDrain, FluidAction action) {
        FluidStack drained = super.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute() && onDrain != null) onDrain.run();
        return drained;
    }

    public float getFillFraction() {
        return capacity == 0 ? 0 : (float) getFluidAmount() / capacity;
    }

    /** Which essence index is stored, or -1 if empty */
    public int getEssenceIndex() {
        if (fluid.isEmpty()) return -1;
        for (int i = 0; i < ModFluids.SOURCES.length; i++) {
            if (fluid.getFluid() == ModFluids.SOURCES[i].get() ||
                    fluid.getFluid() == ModFluids.FLOWINGS[i].get()) return i;
        }
        return -1;
    }
}