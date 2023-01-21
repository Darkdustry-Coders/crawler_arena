package arena;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Timer;
import arena.ai.BossAI;
import arena.ai.ReinforcementAI;
import arena.boss.*;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.blocks.payloads.BuildPayload;
import useful.Bundle;

import static arena.CrawlerVars.*;
import static arena.Main.*;
import static arena.PlayerData.datas;
import static arena.boss.BossBullets.bullets;
import static mindustry.Vars.*;

public class CrawlerLogic {

    public static void applyRules(Rules rules) {
        rules.waveTimer = false;
        rules.waves = true;
        rules.waitEnemies = true;
        rules.canGameOver = false;
        rules.possessionAllowed = true;
        rules.ghostBlocks = false;

        rules.unitCap = unitCap;
        rules.modeName = "Crawler Arena";
        rules.env = defaultEnv;

        rules.weather.clear();
        rules.hiddenBuildItems.clear();
    }

    public static void play() {
        datas.filter(data -> data.player.con.isConnected());
        datas.each(PlayerData::reset);

        applyRules(state.rules);

        state.rules.defaultTeam.cores().each(Building::kill);
        bullets.clear(); // it can kill everyone after new game

        firstWaveLaunched = false;

        state.wave = 0;
        statScaling = 1f;
    }

    public static void gameOver(boolean win) {
        datas.each(data -> Call.infoMessage(data.player.con, Bundle.get(win ? "events.victory" : "events.lose", data)));
        Call.hideHudText();

        BossBullets.timer(0f, 0f, (x, y) -> Events.fire(new GameOverEvent(win ? state.rules.defaultTeam : state.rules.waveTeam)));

        for (int i = 0; i < world.width() * world.height() / 2400; i++) // boom!
            BossBullets.atomic(Mathf.random(world.unitWidth()), Mathf.random(world.unitHeight()));
    }

    public static void runWave() {
        state.wave++;
        statScaling += state.wave / statDiv;

        if (state.wave >= bossWave) {
            spawnBoss(); // during the boss battle do not spawn small enemies
            return;
        }

        int totalEnemies = Mathf.ceil(Mathf.pow(crawlersExpBase, state.wave * crawlersRamp));

        for (var entry : enemyCuts) {
            int typeCount = totalEnemies / entry.value;
            totalEnemies -= typeCount;

            for (int i = 0; i < Math.min(typeCount, maxUnits); i++)
                spawnEnemy(entry.key);
        }

        waveLaunched = true;
    }

    public static void spawnEnemy(UnitType type) {
        boolean half = Mathf.chance(.5f);
        boolean side = Mathf.chance(.5f);

        var tile = world.tile(
                half ? (side ? tilesize : world.width() - tilesize) : Mathf.random(tilesize, world.width() - tilesize),
                half ? Mathf.random(tilesize, world.height() - tilesize) : (side ? world.height() - tilesize : tilesize)
        );

        var unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());
        unit.health = unit.maxHealth *= statScaling / 5;
    }

    public static void spawnBoss() {
        Bundle.announce("events.boss");

        BossBullets.timer(world.width() * 4f, world.height() * 4f, (x, y) -> {
            BossBullets.impact(x, y); // some cool effects

            var boss = UnitTypes.eclipse.spawn(state.rules.waveTeam, x, y);
            boss.controller(new BossAI());

            // increasing armor to keep the bar boss working
            boss.armor(statScaling * Groups.player.size() * 48000f);
            boss.damageMultiplier(statScaling * 48f);

            boss.apply(StatusEffects.overclock, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.overdrive, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.boss);

            var abilities = Seq.with(boss.abilities);

            abilities.add(new GroupSpawnAbility(UnitTypes.flare, 5, -64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.flare, 5, 64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 3, 0, -96f));

            abilities.add(new GroupSpawnAbility(UnitTypes.quell, 3, -96f, 96f));
            abilities.add(new GroupSpawnAbility(UnitTypes.quell, 3, 96f, 96f));
            abilities.add(new GroupSpawnAbility(UnitTypes.disrupt, 1, 0, -128f));

            abilities.add(new BulletSpawnAbility(BossBullets::toxopidMount));
            abilities.add(new BulletSpawnAbility(BossBullets::corvusLaser, 1800f));
            abilities.add(new BulletSpawnAbility(BossBullets::fuseTitanium));
            abilities.add(new BulletSpawnAbility(BossBullets::fuseThorium));
            abilities.add(new BulletSpawnAbility(BossBullets::arcLight, 300f));
            abilities.add(new BulletSpawnAbility(BossBullets::atomic));

            boss.abilities(abilities.toArray());

            waveLaunched = true;
        });
    }

    public static void spawnReinforcement() {
        Timer.schedule(() -> Bundle.announce("events.aid"), 3f);

        for (int i = 0; i <= state.wave; i++) {
            var unit = UnitTypes.mega.spawn(Team.derelict, Mathf.random(40f), world.unitHeight() / 2f + Mathf.range(120f));
            unit.controller(new ReinforcementAI());
            unit.health = unit.maxHealth = Float.MAX_VALUE;

            var block = Seq.with(aidBlocks.keys()).random();

            if (unit instanceof Payloadc payloadc)
                for (int j = 0; j < aidBlocks.get(block); j++)
                    payloadc.addPayload(new BuildPayload(block, state.rules.defaultTeam));
        }
    }

    public static void join(Player player) {
        var data = PlayerData.getData(player);
        if (data != null) {
            data.handlePlayerJoin(player);
            Bundle.bundled(player, "events.join.already-played");
        } else {
            datas.add(new PlayerData(player));
            Bundle.bundled(player, "events.join.welcome");
        }
    }
}