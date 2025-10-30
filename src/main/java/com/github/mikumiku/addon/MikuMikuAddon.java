package com.github.mikumiku.addon;

import com.github.mikumiku.addon.commands.CommandMiku;
import com.github.mikumiku.addon.hud.HudMiku;
import com.github.mikumiku.addon.modules.*;
import lombok.extern.slf4j.Slf4j;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo.Type;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

@Slf4j
public class MikuMikuAddon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Miku");
    public static final Category CATEGORY_MIKU_COMBAT = new Category("Miku 战斗");
    public static final HudGroup HUD_GROUP = new HudGroup("Miku");

    @Override
    public void onInitialize() {
        log.info("Initializing Meteor Addon Miku");

        // Modules
        Modules modules = Modules.get();
//        modules.add(new ModuleExample());
        modules.add(new PlayerAlert());
        modules.add(new AutoTrashModule());
        modules.add(new LiquidFiller());
        modules.add(new AutoMiner());
        modules.add(new AutoUseItems());
        modules.add(new TreeAura());
        modules.add(new SeedMine());
        modules.add(new OnekeyFireWork());
        modules.add(new StructureFinder());
        modules.add(new ElytraFinder());
        modules.add(new ShulkerBoxItemFetcher());
        modules.add(new VillagerRoller());
        modules.add(new LitematicaMover());
        modules.add(new AutoWalk());
        modules.add(new HandsomeSpin());
        modules.add(new AutoCrystalBlock());
        modules.add(new AutoLog());
        modules.add(new InfiniteElytra());
        modules.add(new GhostMine());
        modules.add(new AutoWither());
        modules.add(new AnchorAuraPlus());
        modules.add(new AutoEz());
        modules.add(new RoadBuilder());
        modules.add(new SelfTrapPlusPlus());
        modules.add(new AutoSM());
        modules.add(new AutoLoginPlus());
        modules.add(new NetherSearchArea());
        modules.add(new NoFall());
        modules.add(new EntityList());
        modules.add(new AutoTouchFire());
        modules.add(new FarmHelper());
        modules.add(new AutoXP());
        modules.add(new ChestAura());
        modules.add(new FastFall());
        modules.add(new TridentFly());
        modules.add(new ElytraFlyPlus());
        modules.add(new Hover());
        modules.add(new HighwayBlocker());
        modules.add(new HighwayClearer());
        modules.add(new Criticals());
        modules.add(new MaceCombo());
        modules.add(new AutoFollowPlayer());
        modules.add(new OneKeyPearl());
        modules.add(new NoJumpDelay());
        modules.add(new Scaffold());
        modules.add(new BedrockFinder());
        modules.add(new ChestplateFly());
        modules.add(new UserGuide());
        modules.add(new AutoSlab());
        modules.add(new CometTunnel());
        modules.add(new FishingRodFace());
        modules.add(new KillAuraMiku());
        modules.add(new BestPrinter());
        modules.add(new Nuker());
        ChatUtils.warning("Miku插件完全不开源收费贼贵，这是开源版本。");
        ChatUtils.warning("Miku插件群：太吵了天天@你让你交钱");
        Commands.add(new CommandMiku());
        Hud.get().register(HudMiku.INFO);

        try {
            Config config = Config.get();
            config.customFont.set(true);
            FontFamily dengxianFamily = Fonts.getFamily("Dengxian");
            if (dengxianFamily != null) {
                FontFace dengxianFont = dengxianFamily.get(Type.Regular);
                if (dengxianFont != null) {
                    config.font.set(dengxianFont);
                }
            }
        } catch (Exception var6) {
        }
    }

    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(CATEGORY_MIKU_COMBAT);
    }

    public String getPackage() {
        return "com.github.mikumiku.addon";
    }

    public GithubRepo getRepo() {
        return new GithubRepo("mikumiku7", "meteor-miku");
    }
}
