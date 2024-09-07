package com.example.addon.mixin.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={FluidBlock.class})
public class MixinBlockLiquid extends Block {
    public MixinBlockLiquid(Settings settings) {
        super(settings);
    }
//    @Inject(method={"getCollisionBoundingBox"}, at={@At(value="HEAD")}, cancellable=true)
//    public void getCollisionBoundingBoxHook(IBlockState blockState, B worldIn, BlockPos pos, CallbackInfoReturnable<AxisAlignedBB> info) {
//        JesusEvent event = new JesusEvent(0, pos);
//        MinecraftForge.EVENT_BUS.post((Event)event);
//        if (event.isCanceled()) {
//            info.setReturnValue(event.getBoundingBox());
//        }
//    }
    public static void canCollide(BlockState blockState, boolean hitIfLiquid, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(hitIfLiquid && (Integer)blockState.get((Property)FluidBlock.LEVEL) == 0
           // || LiquidInteract.getInstance().isOn()
        );
    }
}
