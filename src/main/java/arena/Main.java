package arena;

import arc.Events;
import arc.math.Mathf;
import arc.util.*;
import arena.ai.EnemyAI;
import arena.boss.BossBullets;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import useful.Bundle;

import static arena.CrawlerVars.*;
import static arena.PlayerData.datas;
import static arena.boss.BossBullets.bullets;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static boolean waveLaunched, firstWaveLaunched;
    public static float statScaling;

    @Override
    public void init() {
        Bundle.load(Main.class);
        CrawlerVars.load();

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        content.units().each(type -> type.naval, type -> type.flying = true);
        content.units().each(type -> type.payloadCapacity = 36f * tilePayload);

        UnitTypes.crawler.aiController = EnemyAI::new;
        UnitTypes.atrax.aiController = EnemyAI::new;
        UnitTypes.spiroct.aiController = EnemyAI::new;
        UnitTypes.arkyid.aiController = EnemyAI::new;
        UnitTypes.toxopid.aiController = EnemyAI::new;

        Events.on(PlayEvent.class, event -> CrawlerLogic.play());
        Events.on(PlayerJoin.class, event -> CrawlerLogic.join(event.player));

        Timer.schedule(() -> Bundle.sendToChat("events.tip.info"), 120f, 240f);
        Timer.schedule(() -> Bundle.sendToChat("events.tip.upgrades"), 240f, 240f);

        Timer.schedule(BossBullets::update, 0f, .1f);

        Events.run(Trigger.update, () -> {
            if (state.gameOver || Groups.player.isEmpty()) return;

            if (!firstWaveLaunched) {
                firstWaveLaunched = true;

                Bundle.announce("events.first-wave", firstWaveDelay);
                Time.run(firstWaveDelay * 60f, CrawlerLogic::runWave);
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

                float delay = waveDelay + additionalDelay * state.wave;
                if (state.wave >= helpMinWave && state.wave % helpSpacing == 0) {
                    CrawlerLogic.spawnReinforcement();
                    delay += helpExtraTime; // Aid package needs time to deliver blocks
                }

                Bundle.announce("events.next-wave", delay);
                Time.run(delay * 60f, CrawlerLogic::runWave);

                datas.each(PlayerData::afterWave);
            }

            datas.each(data -> Bundle.setHud(data.player, "ui.money", data.money));
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length > 1 && Strings.parseInt(args[1]) <= 0) {
                Bundle.bundled(player, "upgrade.invalid-amount");
                return;
            }

            var type = content.units().find(unit -> unitCosts.containsKey(unit) && unit.name.equalsIgnoreCase(args[0]));
            if (type == null) {
                Bundle.bundled(player, "upgrade.unit-not-found");
                return;
            }

            int amount = args.length > 1 ? Strings.parseInt(args[1]) : 1;
            if (state.rules.defaultTeam.data().countType(type) > unitCap - amount) {
                Bundle.bundled(player, "upgrade.too-many-units");
                return;
            }

            var data = PlayerData.getData(player);
            if (data.money < unitCosts.get(type) * amount) {
                Bundle.bundled(player, "upgrade.not-enough-money", unitCosts.get(type) * amount, data.money);
                return;
            }

            data.controlUnit(data.applyUnit(type.spawn(player.x, player.y)));
            for (int i = 1; i < amount; i++)
                data.applyUnit(type.spawn(player.x, player.y));

            data.money -= unitCosts.get(type) * amount;
            Bundle.bundled(player, "upgrade.success", amount, type.name);
        });

        handler.<Player>register("upgrades", "[page]", "Show units you can upgrade to.", (args, player) -> {
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1, pages = Mathf.ceil(unitCosts.size / (float) unitsPerPage);
            if (page > pages || page <= 0) {
                Bundle.bundled(player, "upgrades.invalid-page", pages);
                return;
            }

            var data = PlayerData.getData(player);
            var builder = new StringBuilder();

            for (int i = unitsPerPage * (page - 1); i < Math.min(unitsPerPage * page, unitCosts.size); i++) {
                var type = unitCosts.orderedKeys().get(i);
                builder.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(data.money >= unitCosts.get(type) ? "[lime]" : "[scarlet]").append(unitCosts.get(type)).append("[])\n");
            }

            Call.infoMessage(player.con, Bundle.format("upgrades", data, page, builder.toString()));
        });

        handler.<Player>register("info", "Show info about the Crawler Arena gamemode", (args, player) -> Bundle.bundled(player, "info", bossWave));
    }
}