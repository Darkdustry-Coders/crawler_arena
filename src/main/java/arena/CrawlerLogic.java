package arena;

import arc.Events;
import arena.ai.BossAI;
import arena.ai.ReinforcementAI;
import arena.boss.*;
import mindustry.content.StatusEffects;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.blocks.payloads.BuildPayload;

import static arc.math.Mathf.*;
import static arc.struct.Seq.with;
import static arc.util.Timer.schedule;
import static arena.CrawlerVars.*;
import static arena.Main.*;
import static arena.PlayerData.datas;
import static arena.boss.BossBullets.bullets;
import static mindustry.Vars.*;
import static mindustry.content.UnitTypes.*;
import static useful.Bundle.*;

public class CrawlerLogic {

    public static void applyRules(Rules rules) {
        rules.canGameOver = false;
        rules.waveTimer = false;
        rules.waves = true;
        rules.waitEnemies = true;
        rules.unitCap = unitCap;
        rules.modeName = "Crawler Arena";
        rules.env = defaultEnv;

        rules.weather.clear();
        rules.hiddenBuildItems.clear();
    }

    public static void play() {
        applyRules(state.rules);

        state.wave = 0;
        state.rules.defaultTeam.cores().each(Building::kill);
        statScaling = 1f;

        bullets.clear(); // it can kill everyone after new game
    }

    public static void startGame() {
        datas.filter(data -> data.player.con.isConnected());
        datas.each(PlayerData::reset);

        firstWaveLaunched = false;
    }

    public static void gameOver(boolean win) {
        datas.each(data -> Call.infoMessage(data.player.con, get(win ? "events.victory" : "events.lose", data)));
        Call.hideHudText();

        BossBullets.timer(0f, 0f, (x, y) -> Events.fire(new GameOverEvent(win ? state.rules.defaultTeam : state.rules.waveTeam)));

        for (int i = 0; i < world.width() * world.height() / 2400; i++) // boom!
            BossBullets.atomic(random(world.unitWidth()), random(world.unitHeight()));
    }

    public static void runWave() {
        state.wave++;
        statScaling += state.wave / statDiv;

        if (state.wave >= bossWave) spawnBoss(); // during the boss battle do not spawn small enemies
        else {
            int totalEnemies = ceil(pow(crawlersExpBase, state.wave * crawlersRamp));

            for (var entry : enemyCuts) {
                int typeCount = totalEnemies / entry.value;
                totalEnemies -= typeCount;

                for (int i = 0; i < Math.min(typeCount, maxUnits); i++)
                    spawnEnemy(entry.key);
            }

            waveLaunched = true;
        }
    }

    public static void spawnEnemy(UnitType type) {
        var tile = with(
                world.tiles.getc(tilesize, random(world.height())),
                world.tiles.getc(random(world.width()), tilesize),
                world.tiles.getc(world.width() - tilesize, random(world.height())),
                world.tiles.getc(random(world.width()), world.height() - tilesize)
        ).random();

        var unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());

        unit.health = unit.maxHealth *= statScaling / 5;
    }

    public static void spawnBoss() {
        announce("events.boss");

        BossBullets.timer(world.width() * 4f, world.height() * 4f, (x, y) -> {
            BossBullets.impact(x, y); // some cool effects
            var boss = eclipse.spawn(state.rules.waveTeam, x, y);
            boss.controller(new BossAI());

            // increasing armor to keep the bar boss working
            boss.armor(statScaling * Groups.player.size() * 32000f);
            boss.damageMultiplier(statScaling * 32f);

            boss.apply(StatusEffects.overclock, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.overdrive, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.boss);

            var abilities = with(boss.abilities);

            abilities.add(new GroupSpawnAbility(flare, 5, -64f, 64f));
            abilities.add(new GroupSpawnAbility(flare, 5, 64f, 64f));
            abilities.add(new GroupSpawnAbility(zenith, 3, 0, -96f));

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
        schedule(() -> announce("events.aid"), 3f);

        for (int i = 0; i < state.wave; i++) {
            var unit = mega.spawn(Team.derelict, random(40), world.unitHeight() / 2f + range(120f));
            unit.controller(new ReinforcementAI());
            unit.health = unit.maxHealth = Float.MAX_VALUE;

            var block = with(aidBlocks.keys()).random();

            if (unit instanceof Payloadc payloadc)
                for (int j = 0; j < aidBlocks.get(block); j++)
                    payloadc.addPayload(new BuildPayload(block, state.rules.defaultTeam));
        }
    }

    public static void join(Player player) {
        var data = PlayerData.getData(player.uuid());
        if (data != null) {
            data.handlePlayerJoin(player);
            bundled(player, "events.join.already-played");
        } else {
            datas.add(new PlayerData(player));
            bundled(player, "events.join.welcome");
        }
    }
}