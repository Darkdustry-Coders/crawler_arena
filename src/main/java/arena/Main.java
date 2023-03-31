package arena;

import arc.Events;
import arc.util.*;
import arena.ai.EnemyAI;
import arena.boss.BossBullets;
import arena.menus.UpgradeMenu;
import mindustry.content.UnitTypes;
import mindustry.core.UI;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import useful.*;

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
        Formatter.setFormatter(Bundle::format);

        CrawlerVars.load();
        UpgradeMenu.load();

        content.units().each(type -> type.naval, type -> type.flying = true);
        content.units().each(type -> type.payloadCapacity = 36f * tilePayload);

        netServer.admins.addActionFilter(action -> action.type != ActionType.breakBlock && action.type != ActionType.placeBlock);

        UnitTypes.crawler.aiController = EnemyAI::new;
        UnitTypes.atrax.aiController = EnemyAI::new;
        UnitTypes.spiroct.aiController = EnemyAI::new;
        UnitTypes.arkyid.aiController = EnemyAI::new;
        UnitTypes.toxopid.aiController = EnemyAI::new;

        UI.thousands = "k";
        UI.millions = "m";
        UI.billions = "b";

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

                datas.eachValue(PlayerData::afterWave);
            }

            datas.eachValue(data -> Bundle.setHud(data.player, "ui.money", data.money));
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "Upgrade your unit.", (args, player) -> {
            var data = datas.get(player.uuid());
            UpgradeMenu.showUpgradeMenu(player, data);
        });
    }
}