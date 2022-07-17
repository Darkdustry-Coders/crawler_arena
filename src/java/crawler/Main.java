package crawler;

import arc.Events;
import arc.math.Mathf;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Structs;
import arc.util.Timer;
import crawler.ai.CrawlerAI;
import crawler.ai.DefaultAI;
import crawler.boss.BossBullets;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Rules;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.type.UnitType;

import java.util.Locale;

import static arc.Core.app;
import static crawler.CrawlerVars.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final Rules rules = new Rules();

    public static boolean isWaveGoing, firstWaveLaunched;
    public static float statScaling;

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(Bundle.format(key, findLocale(player), values));
    }

    public static void sendToChat(String key, Object... values) {
        Groups.player.each(player -> bundled(player, key, values));
    }

    public static Locale findLocale(Player player) {
        Locale locale = Structs.find(Bundle.supportedLocales, l -> player.locale.equals(l.toString()) || player.locale.startsWith(l.toString()));
        return locale != null ? locale : Bundle.defaultLocale;
    }

    @Override
    public void init() {
        Bundle.load();
        CrawlerLogic.load();
        CrawlerVars.load();

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        content.units().each(type -> type.constructor.get() instanceof WaterMovec, type -> type.flying = true);
        content.units().each(type -> {
            type.payloadCapacity = 6f * 6f * tilePayload;
            type.controller = unit -> new DefaultAI();
        });

        UnitTypes.crawler.controller = unit -> new CrawlerAI();

        Events.on(WorldLoadEvent.class, event -> app.post(CrawlerLogic::play));
        Events.on(PlayerJoin.class, event -> CrawlerLogic.join(event.player));

        Timer.schedule(() -> sendToChat("events.tip.info"), 120f, 240f);
        Timer.schedule(() -> sendToChat("events.tip.upgrades"), 240f, 240f);

        Timer.schedule(BossBullets::update, 0f, .1f);

        Events.run(Trigger.update, () -> {
            if (state.gameOver || Groups.player.isEmpty()) return;

            if (!firstWaveLaunched) { // It is really needed, trust me, I'm not a schizoid
                firstWaveLaunched = true;

                sendToChat("events.first-wave", firstWaveDelay);
                Timer.schedule(CrawlerLogic::runWave, firstWaveDelay);
                return;
            }

            if (rules.defaultTeam.data().unitCount == 0 && isWaveGoing) {
                isWaveGoing = false;

                CrawlerLogic.gameOver();
                return;
            }

            if (rules.waveTeam.data().unitCount == 0 && isWaveGoing) {
                isWaveGoing = false;

                // it can kill somebody
                BossBullets.bullets.clear();

                int delay = waveDelay;
                if (state.wave >= helpMinWave && state.wave % helpSpacing == 0) {
                    CrawlerLogic.spawnReinforcement();
                    delay += helpExtraTime; // megas need time to deliver blocks
                }

                sendToChat("events.next-wave", delay);
                Timer.schedule(CrawlerLogic::runWave, delay);
                PlayerData.each(PlayerData::afterWave);
            }

            PlayerData.each(data -> Call.setHudText(data.player.con, Bundle.format("ui.money", data.locale, data.money)));
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length == 2 && Strings.parseInt(args[1]) <= 0) {
                bundled(player, "upgrade.invalid-amount");
                return;
            }

            UnitType type = costs.keys().toSeq().find(u -> u.name.equalsIgnoreCase(args[0]));
            if (type == null) {
                bundled(player, "upgrade.unit-not-found");
                return;
            }

            int amount = args.length == 2 ? Strings.parseInt(args[1]) : 1;
            if (rules.defaultTeam.data().countType(type) + amount > unitCap) {
                bundled(player, "upgrade.too-many-units");
                return;
            }

            PlayerData data = PlayerData.datas.get(player.uuid());
            if (data.money < costs.get(type) * amount) {
                bundled(player, "upgrade.not-enough-money", costs.get(type) * amount, data.money);
                return;
            }

            for (int i = 0; i < amount; i++) {
                Unit unit = type.spawn(player.x + Mathf.range(8f), player.y + Mathf.range(8f));
                data.applyUnit(unit);
            }

            data.money -= costs.get(type) * amount;
            bundled(player, "upgrade.success", amount, type.name);
        });

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            PlayerData data = PlayerData.datas.get(player.uuid());
            StringBuilder upgrades = new StringBuilder(Bundle.format("upgrades", data.locale));
            costs.each((type, cost) -> upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(data.money < cost ? "[scarlet]" : "[lime]").append(cost).append("[])\n"));
            player.sendMessage(upgrades.toString());
        });

        handler.<Player>register("info", "Show info about the Crawler Arena gamemode", (args, player) -> bundled(player, "info", bossWave));
    }
}
