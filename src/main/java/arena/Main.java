package arena;

import arc.Events;
import arc.util.*;
import arena.ai.EnemyAI;
import arena.boss.BossBullets;
import arena.menus.UpgradeMenu;
import arena.menus.UpgradeMenu.UnitCost;
import mindustry.content.UnitTypes;
import mindustry.core.UI;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import useful.Bundle;

import static arena.CrawlerVars.*;
import static arena.PlayerData.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static boolean waveLaunched, firstWaveLaunched;
    public static float statScaling;

    @Override
    public void init() {
        Bundle.load(getClass());

        CrawlerVars.load();
        UpgradeMenu.load();

        content.units().each(type -> type.naval, type -> type.flying = true);
        content.units().each(type -> {
            type.payloadCapacity = 36f * tilePayload;
            type.targetGround = type.targetAir = true;
        });

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

                CrawlerLogic.gameOver(state.rules.waveTeam);
                return;
            }

            if (state.rules.waveTeam.data().unitCount == 0 && waveLaunched) {
                waveLaunched = false;

                if (state.wave >= bossWave) {
                    CrawlerLogic.gameOver(state.rules.defaultTeam);
                    return;
                }

                int delay = waveDelay + additionalDelay * state.wave;
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
        handler.<Player>register("upgrade", "[type] [amount]", "Upgrade your unit.", (args, player) -> {
            var data = datas.get(player.uuid());
            if (args.length == 0) {
                UpgradeMenu.show(player, data);
                return;
            }

            var unit = UnitCost.find(args[0]);
            if (unit == null) {
                Bundle.announce(player, "upgrade.unit-not-found");
                return;
            }

            int amount = args.length > 1 ? Strings.parseInt(args[1]) : 1;
            if (amount <= 0) {
                Bundle.announce(player, "upgrade.invalid-amount");
                return;
            }

            if (state.rules.defaultTeam.data().countType(unit.type) > state.rules.unitCap - amount) {
                Bundle.announce(player, "upgrade.too-many-units", state.rules.defaultTeam.data().countType(unit.type));
                return;
            }

            if (data.money < unit.cost * amount) {
                Bundle.announce(player, "upgrade.not-enough-money", UI.formatAmount(data.money), UI.formatAmount(unit.cost * amount));
                return;
            }

            for (int i = 0; i < amount; i++)
                data.applyUnit(unit.type.spawn(player.x, player.y));

            data.money -= unit.cost * amount;
            Bundle.announce(player, "upgrade.success", amount, unit.type.emoji(), unit.type.name);
        });
    }
}