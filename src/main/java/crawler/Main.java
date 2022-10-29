package crawler;

import arc.Events;
import arc.math.Mathf;
import arc.util.CommandHandler;
import crawler.ai.CrawlerAI;
import crawler.boss.BossBullets;
import mindustry.ai.types.SuicideAI;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.world.blocks.defense.BaseShield.BaseShieldBuild;
import useful.Bundle;

import java.util.concurrent.atomic.AtomicInteger;

import static arc.struct.Seq.with;
import static arc.util.Strings.parseInt;
import static arc.util.Timer.schedule;
import static crawler.CrawlerVars.*;
import static crawler.PlayerData.datas;
import static crawler.boss.BossBullets.bullets;
import static mindustry.Vars.*;
import static mindustry.ai.Pathfinder.*;
import static useful.Bundle.*;

public class Main extends Plugin {

    public static boolean waveLaunched, firstWaveLaunched;
    public static float statScaling;

    @Override
    public void init() {
        Bundle.load(Main.class);
        CrawlerVars.load();

        fieldTypes.set(0, () -> new PositionTarget(CrawlerLogic.worldCenter()));

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        content.units().each(type -> type.constructor.get() instanceof WaterMovec, type -> type.flying = true);
        content.units().each(type -> type.payloadCapacity = 36f * tilePayload);

        UnitTypes.crawler.aiController = SuicideAI::new;
        UnitTypes.atrax.aiController = CrawlerAI::new;
        UnitTypes.spiroct.aiController = CrawlerAI::new;
        UnitTypes.arkyid.aiController = CrawlerAI::new;
        UnitTypes.toxopid.aiController = CrawlerAI::new;

        Events.on(PlayEvent.class, event -> CrawlerLogic.play());
        Events.on(SaveLoadEvent.class, event -> CrawlerLogic.startGame());

        Events.on(PlayerJoin.class, event -> CrawlerLogic.join(event.player));

        schedule(() -> sendToChat("events.tip.info"), 120f, 240f);
        schedule(() -> sendToChat("events.tip.upgrades"), 240f, 240f);

        schedule(BossBullets::update, 0f, .1f);

        Events.run(Trigger.update, () -> {
            if (state.gameOver || Groups.player.isEmpty()) return;

            if (!firstWaveLaunched) { // It is really needed, trust me, I'm not a schizoid
                firstWaveLaunched = true;

                announce("events.first-wave", firstWaveDelay);
                schedule(CrawlerLogic::runWave, firstWaveDelay);
                return;
            }

            if (state.rules.defaultTeam.data().unitCount == 0 && waveLaunched) {
                waveLaunched = false;

                CrawlerLogic.gameOver(false);
                return;
            }

            if (state.rules.waveTeam.data().unitCount == 0 && waveLaunched) {
                waveLaunched = false;

                // it can kill somebody
                bullets.clear();

                if (state.wave >= bossWave) {
                    CrawlerLogic.gameOver(true); // it is the end
                    return;
                }

                int delay = waveDelay;
                if (state.wave >= helpMinWave && state.wave % helpSpacing == 0) {
                    CrawlerLogic.spawnReinforcement();
                    delay += helpExtraTime; // megas need time to deliver blocks
                }

                announce("events.next-wave", delay);
                schedule(CrawlerLogic::runWave, delay);

                datas.each(PlayerData::afterWave);

                // Remove all BaseShields since they are too OP
                world.tiles.eachTile(tile -> {
                    if (tile.build instanceof BaseShieldBuild build && build.radius() >= 1f) {
                        build.kill();
                    }
                });
            }

            datas.each(data -> Call.setHudText(data.player.con, format("ui.money", data, data.money)));
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length > 1 && parseInt(args[1]) <= 0) {
                bundled(player, "upgrade.invalid-amount");
                return;
            }

            var type = with(costs.keys()).find(unitType -> unitType.name.equalsIgnoreCase(args[0]));
            if (type == null) {
                bundled(player, "upgrade.unit-not-found");
                return;
            }

            int amount = args.length > 1 ? parseInt(args[1]) : 1;
            if (state.rules.defaultTeam.data().countType(type) + amount > unitCap) {
                bundled(player, "upgrade.too-many-units");
                return;
            }

            var data = PlayerData.getData(player.uuid());
            if (data.money < costs.get(type) * amount) {
                bundled(player, "upgrade.not-enough-money", costs.get(type) * amount, data.money);
                return;
            }

            player.unit(data.applyUnit(type.spawn(player.x + Mathf.range(tilesize), player.y + Mathf.range(tilesize))));

            for (int i = 1; i < amount; i++)
                data.applyUnit(type.spawn(player.x + Mathf.range(tilesize), player.y + Mathf.range(tilesize)));

            data.money -= costs.get(type) * amount;
            bundled(player, "upgrade.success", amount, type.name);
        });

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            var data = PlayerData.getData(player.uuid());
            var upgrades = new StringBuilder();

            var integer = new AtomicInteger();
            costs.each((type, cost) -> {
                upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(data.money >= cost ? "[lime]" : "[scarlet]").append(cost).append("[])\n");
                if (integer.incrementAndGet() % 25 == 0) {
                    Call.infoMessage(player.con, format("upgrades", data, upgrades));
                    upgrades.setLength(0);
                }
            });
        });

        handler.<Player>register("info", "Show info about the Crawler Arena gamemode", (args, player) -> bundled(player, "info", bossWave));
    }
}