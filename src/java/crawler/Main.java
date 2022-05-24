package crawler;

import arc.Events;
import arc.math.Mathf;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Timer;
import crawler.boss.BossBullets;
import mindustry.ai.types.FlyingAI;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Rules;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.gen.WaterMovec;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.type.UnitType;

import static arc.Core.app;
import static crawler.Bundle.*;
import static crawler.CrawlerVars.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final Rules rules = new Rules();

    public static boolean isWaveGoing;
    public static float statScaling;

    @Override
    public void init() {
        CrawlerVars.load();
        CrawlerLogic.load();

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        content.units().each(unit -> unit.constructor.get() instanceof WaterMovec, unit -> unit.flying = true);
        content.units().each(type -> {
            type.payloadCapacity = 6f * 6f * tilePayload;
            type.maxRange = Float.MAX_VALUE;
            type.defaultController = FlyingAI::new;
        });

        UnitTypes.poly.defaultController = SwarmAI::new;
        UnitTypes.poly.health = 125f;
        UnitTypes.poly.speed = 1.5f;

        Events.on(WorldLoadEvent.class, event -> app.post(CrawlerLogic::play));
        Events.on(PlayerJoin.class, event -> CrawlerLogic.join(event.player));

        Timer.schedule(() -> sendToChat("events.tip.info"), 90f, 180f);
        Timer.schedule(() -> sendToChat("events.tip.upgrades"), 180f, 180f);

        Timer.schedule(BossBullets::update, 0f, .1f);
        Timer.schedule(() -> {
            if (state.gameOver || Groups.player.isEmpty()) return;

            if (rules.defaultTeam.data().unitCount == 0 && state.wave > 0) {
                isWaveGoing = false;

                if (state.wave > bossWave) {
                    sendToChat("events.gameover.win");
                    Events.fire(new GameOverEvent(state.rules.defaultTeam));
                } else {
                    sendToChat("events.gameover.lose");
                    Events.fire(new GameOverEvent(state.rules.waveTeam));
                }

                Call.hideHudText();
                return;
            } else if (rules.waveTeam.data().unitCount == 0 && isWaveGoing) {
                isWaveGoing = false;

                int delay = waveDelay;
                if (state.wave > helpMinWave && state.wave % helpSpacing == 0) {
                    CrawlerLogic.spawnReinforcement();
                    delay += helpExtraTime; // megas need time to deliver blocks
                }

                sendToChat(state.wave == 0 ? "events.first-wave" : "events.next-wave", delay);
                Timer.schedule(CrawlerLogic::runWave, delay);
                PlayerData.each(PlayerData::afterWave);
            }

            PlayerData.each(data -> Call.setHudText(data.player.con, format("ui.money", data.locale, data.money)));
        }, 0f, 1f);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length == 2 && Strings.parseInt(args[1]) < 0) {
                bundled(player, "commands.upgrade.invalid-amount");
                return;
            }

            UnitType type = costs.keys().toSeq().find(u -> u.name.equalsIgnoreCase(args[0]));
            if (type == null) {
                bundled(player, "commands.upgrade.unit-not-found");
                return;
            }

            int amount = args.length == 2 ? Strings.parseInt(args[1]) : 1;
            if (rules.defaultTeam.data().countType(type) + amount > unitCap) {
                bundled(player, "commands.upgrade.too-many-units");
                return;
            }

            PlayerData data = PlayerData.datas.get(player.uuid());
            if (data.money < costs.get(type) * amount) {
                bundled(player, "commands.upgrade.not-enough-money", costs.get(type) * amount, data.money);
                return;
            }

            for (int i = 0; i < amount; i++) {
                Unit unit = type.spawn(player.x + Mathf.range(8f), player.y + Mathf.range(8f));
                data.applyUnit(unit);
            }

            data.money -= costs.get(type) * amount;
            bundled(player, "commands.upgrade.success", amount, type.name);
        });

        handler.<Player>register("info", "Show info about the Crawler Arena gamemode.", (args, player) -> bundled(player, "commands.information"));

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            PlayerData data = PlayerData.datas.get(player.uuid());
            StringBuilder upgrades = new StringBuilder(format("commands.upgrades.header", data.locale));
            costs.each((type, cost) -> upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(data.money < cost ? "[scarlet]" : "[lime]").append(cost).append("[])\n"));
            player.sendMessage(upgrades.toString());
        });

        handler.<Player>register("spawn1", "", (args, player) -> {
            BossBullets.timer(player.x + Mathf.range(200f), player.y + Mathf.range(200f), BossBullets::toxomount);
        });

        handler.<Player>register("spawn2", "", (args, player) -> {
            BossBullets.timer(player.x + Mathf.range(200f), player.y + Mathf.range(200f), BossBullets::fusetitanium);
        });

        handler.<Player>register("spawn3", "", (args, player) -> {
            BossBullets.timer(player.x + Mathf.range(200f), player.y + Mathf.range(200f), BossBullets::fusethorium);
        });

        handler.<Player>register("spawn4", "", (args, player) -> {
            BossBullets.atomic(player.x + Mathf.range(200f), player.y + Mathf.range(200f));
        });
    }
}
