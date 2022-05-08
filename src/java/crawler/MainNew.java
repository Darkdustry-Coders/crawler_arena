package crawler;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Timer;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.ai.types.FlyingAI;
import mindustry.game.Rules;
import mindustry.game.EventType.*;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.WaterMovec;

import static arc.Core.*;
import static mindustry.Vars.*;
import static crawler.Bundle.*;
import static crawler.CrawlerVarsNew.*;

public class MainNew extends Plugin {

    public static final Rules rules = new Rules();

    public static boolean isWaveGoing;
    public static float statScaling;

    @Override
    public void init() {
        CrawlerVarsNew.load();
        CrawlerLogicNew.load();

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        content.units().each(unit -> unit instanceof WaterMovec, unit -> unit.flying = true);
        content.units().each(type -> {
            type.payloadCapacity = 6f * 6f * tilePayload;
            type.defaultController = FlyingAI::new;
        });

        Events.on(WorldLoadEvent.class, event -> app.post(CrawlerLogicNew::play));
        Events.on(PlayerJoin.class, event -> CrawlerLogicNew.join(event.player));

        Timer.schedule(() -> sendToChat("events.tip.info"), 90f, 180f);
        Timer.schedule(() -> sendToChat("events.tip.upgrades"), 180f, 180f);

        Timer.schedule(() -> {
            if (state.gameOver || PlayerData.datas.isEmpty()) return;

            if (rules.defaultTeam.data().unitCount == 0) {
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
                    CrawlerLogicNew.spawnReinforcement();
                    delay += helpExtraTime; // megas need time to deliver help
                }

                sendToChat(state.wave == 1 ? "events.first-wave" : "events.wave", delay);
                Timer.schedule(CrawlerLogicNew::runWave, delay);
                PlayerData.each(PlayerData::update);
            }

            PlayerData.each(data -> Call.setHudText(data.player.con, format("ui.money", data.locale, data.money)));
        }, 0f, 1f);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("info", "Show info about the Crawler Arena gamemode.", (args, player) -> bundled(player, "commands.information"));

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            PlayerData data = PlayerData.datas.get(player.uuid());
            StringBuilder upgrades = new StringBuilder(format("commands.upgrades.header", data.locale));
            costs.each((type, cost) -> upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(data.money < cost ? "[scarlet]" : "[lime]").append(cost).append("[])\n"));
            player.sendMessage(upgrades.toString());
        });
    }
}
