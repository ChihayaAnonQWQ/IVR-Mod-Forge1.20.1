package net.hulan.ksd;

import mtr.CreativeModeTabs;
import mtr.Registry;
import mtr.RegistryObject;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.DeferredRegisterHolder;
import mtr.forge.mappings.ForgeUtilities;
import mtr.mappings.RegistryUtilities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KSDMain.MOD_ID)
public class KSDForgeMain {

    private static final DeferredRegisterHolder<Item> ITEMS = new DeferredRegisterHolder<>(KSDMain.MOD_ID, ForgeUtilities.registryGetItem());
    private static final DeferredRegisterHolder<Block> BLOCKS = new DeferredRegisterHolder<>(KSDMain.MOD_ID, ForgeUtilities.registryGetBlock());

    static {
        KSDMain.init(
                KSDForgeMain::registerItem,
                KSDForgeMain::registerBlock,
                KSDForgeMain::registerBlock);
    }

    public KSDForgeMain() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeUtilities.registerModEventBus(KSDMain.MOD_ID, eventBus);
        ITEMS.register();
        BLOCKS.register();
        eventBus.register(KSDForgeEventBus.class);
    }

    private static void registerItem(String path, RegistryObject<Item> item) {
        ITEMS.register(path, () -> {
            final Item itemObject = item.get();
            if (itemObject instanceof ItemWithCreativeTabBase) {
                Registry.registerCreativeModeTab(((ItemWithCreativeTabBase) itemObject).creativeModeTab.resourceLocation, itemObject);
            } else if (itemObject instanceof ItemWithCreativeTabBase.ItemPlaceOnWater) {
                Registry.registerCreativeModeTab(((ItemWithCreativeTabBase.ItemPlaceOnWater) itemObject).creativeModeTab.resourceLocation, itemObject);
            }
            return itemObject;
        });
    }

    private static void registerBlock(String path, RegistryObject<Block> block) {
        BLOCKS.register(path, block::get);
    }

    private static void registerBlock(String path, RegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTabWrapper) {
        registerBlock(path, block);
        ITEMS.register(path, () -> {
            final BlockItem blockItem = new BlockItem(block.get(), RegistryUtilities.createItemProperties(creativeModeTabWrapper::get));
            Registry.registerCreativeModeTab(creativeModeTabWrapper.resourceLocation, blockItem);
            return blockItem;
        });
    }

    public static class KSDForgeEventBus {

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(KSDClientMain::init);
        }
    }
}
