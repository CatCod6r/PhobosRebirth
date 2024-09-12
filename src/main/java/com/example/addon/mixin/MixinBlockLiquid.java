package com.example.addon.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidBlock.class)
public class MixinBlockLiquid extends Block {

    public MixinBlockLiquid(Settings settings) {
        super(settings);
    }

//    @Inject(method="getCollisionShape", at=@At("HEAD"), cancellable = true)
//    public void getCollisionBoundingBoxHook(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfo ci) {
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
