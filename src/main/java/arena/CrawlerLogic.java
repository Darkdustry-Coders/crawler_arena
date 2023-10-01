package arena;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Timer;
import arena.ai.*;
import arena.boss.*;
import mindustry.content.*;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.blocks.payloads.BuildPayload;
import useful.Bundle;

import static arc.Core.*;
import static arena.CrawlerVars.*;
import static arena.Main.*;
import static arena.PlayerData.*;
import static arena.boss.BossBullets.*;
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
        rules.env = defaultEnv;

        rules.modeName = "Crawler Arena";

        rules.weather.clear();
        rules.hiddenBuildItems.clear();
    }

    public static void play() {
        app.post(() -> {
            datas.filter((uuid, data) -> data.player.con.isConnected());
            datas.eachValue(PlayerData::reset);
        });

        applyRules(state.rules);

        bullets.clear(); // it can kill someone after a new game
        state.rules.defaultTeam.cores().each(Building::kill);

        firstWaveLaunched = false;

        state.wave = 0;
        statScaling = 1f;
    }

    public static void gameOver(Team winner) {
        bullets.clear(); // it can kill someone
        state.gameOver = true;

        Call.hideHudText();
        BossBullets.timer(0f, 0f, (x, y) -> Events.fire(new GameOverEvent(winner)));

        for (int i = 0; i < world.width() * world.height() / 3600; i++) // Boom Boom Bakudan!
            BossBullets.atomic(Mathf.random(world.unitWidth()), Mathf.random(world.unitHeight()));
    }

    public static void runWave() {
        state.wave++;
        statScaling += state.wave / statDiv;

        if (state.wave >= bossWave) {
            spawnBoss(); // during the boss battle do not spawn small enemies
            return;
        }

        shifts.each((type, shift) -> {
            int amount = Mathf.floor(maxUnits * Mathf.pow(Mathf.sin((state.wave - shift) / 8f), 3) + 1.5f);
            for (int i = 0; i < amount; i++)
                spawnEnemy(type);
        });

        waveLaunched = true;
    }

    public static void spawnEnemy(UnitType type) {
        boolean half = Mathf.randomBoolean();
        boolean side = Mathf.randomBoolean();

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

            boss.armor(statScaling * 96000f);
            boss.damageMultiplier(statScaling * 96f);

            boss.apply(StatusEffects.fast, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.overclock, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.overdrive, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.shielded, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.boss);

            var abilities = Seq.with(boss.abilities);

            abilities.add(new GroupSpawnAbility(content.unit("scathe-missile"), 1, -32f, 0f, 300f));
            abilities.add(new GroupSpawnAbility(content.unit("scathe-missile"), 1, 32f, 0f, 300f));

            abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 6, -64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 6, 64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.antumbra, 3, 0, -96f));

            abilities.add(new GroupSpawnAbility(UnitTypes.quell, 3, -96f, 96f));
            abilities.add(new GroupSpawnAbility(UnitTypes.quell, 3, 96f, 96f));
            abilities.add(new GroupSpawnAbility(UnitTypes.disrupt, 1, 0, -128f));

            abilities.add(new BulletSpawnAbility(BossBullets::toxopidMount));
            abilities.add(new BulletSpawnAbility(BossBullets::corvusLaser, 1800f));
            abilities.add(new BulletSpawnAbility(BossBullets::arcLightning, 300f));
            abilities.add(new BulletSpawnAbility(BossBullets::titaniumFuse));
            abilities.add(new BulletSpawnAbility(BossBullets::thoriumFuse));
            abilities.add(new BulletSpawnAbility(BossBullets::sublimateFlame));
            abilities.add(new BulletSpawnAbility(BossBullets::atomic));

            boss.abilities(abilities.toArray());

            waveLaunched = true;
        });
    }

    public static void spawnReinforcement() {
        Timer.schedule(() -> Bundle.announce("events.aid"), 3f);

        for (int i = 0; i <= state.wave; i++) {
            var unit = UnitTypes.mega.spawn(Team.derelict, Mathf.random(120f), world.unitHeight() / 2f + Mathf.range(120f));
            if (!(unit instanceof Payloadc payloadc)) return; // Just in case

            unit.health(Float.MAX_VALUE);
            unit.maxHealth(Float.MAX_VALUE);
            unit.controller(new ReinforcementAI());

            // Первые две меги доставляют гарантированные блоки
            var reinforcement = i >= 2 ?
                    commonReinforcement :
                    guaranteedReinforcement;

            var block = reinforcement.orderedKeys().random();
            int amount = reinforcement.get(block);

            for (int j = 0; j < amount; j++)
                payloadc.addPayload(new BuildPayload(block, state.rules.defaultTeam));
        }
    }

    public static void join(Player player) {
        var data = datas.get(player.uuid());
        if (data == null) {
            datas.put(player.uuid(), new PlayerData(player));
            Bundle.send(player, "events.join.welcome");
        } else {
            data.join(player);
            Bundle.send(player, "events.join.already-played");
        }
    }
}