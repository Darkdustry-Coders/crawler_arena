package arena;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.*;
import arena.ai.BossAI;
import arena.ai.ReinforcementAI;
import arena.boss.BossBullets;
import arena.boss.BulletSpawnAbility;
import arena.boss.GroupSpawnAbility;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.ctype.MappableContent;
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
        datas.filter((uuid, data) -> data.player.con.isConnected());
        datas.eachValue(PlayerData::reset);

        applyRules(state.rules);

        state.rules.defaultTeam.cores().each(Building::kill);
        bullets.clear(); // it can kill everyone after new game

        firstWaveLaunched = false;

        state.wave = 0;
        statScaling = 1f;
    }

    public static void gameOver(boolean win) {
        datas.eachValue(data -> Call.infoMessage(data.player.con, Bundle.get(win ? "events.victory" : "events.lose", data)));
        Call.hideHudText();

        BossBullets.timer(0f, 0f, (x, y) -> Events.fire(new GameOverEvent(win ? state.rules.defaultTeam : state.rules.waveTeam)));

        for (int i = 0; i < world.width() * world.height() / 2400; i++) // Boom Boom Bakudan!
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

            boss.armor(statScaling * 48000f);
            boss.damageMultiplier(statScaling * 48f);

            boss.apply(StatusEffects.overclock, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.overdrive, Float.POSITIVE_INFINITY);
            boss.apply(StatusEffects.boss);

            var abilities = Seq.with(boss.abilities);

            abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 6, -64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 6, 64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.antumbra, 3, 0, -96f));

            abilities.add(new GroupSpawnAbility(UnitTypes.quell, 3, -96f, 96f));
            abilities.add(new GroupSpawnAbility(UnitTypes.quell, 3, 96f, 96f));
            abilities.add(new GroupSpawnAbility(UnitTypes.disrupt, 1, 0, -128f));

            abilities.add(new BulletSpawnAbility(BossBullets::toxopidMount));
            abilities.add(new BulletSpawnAbility(BossBullets::corvusLaser, 1800f));
            abilities.add(new BulletSpawnAbility(BossBullets::titaniumFuse));
            abilities.add(new BulletSpawnAbility(BossBullets::thoriumFuse));
            abilities.add(new BulletSpawnAbility(BossBullets::arcLight, 300f));
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

            var block = Seq.with(reinforcement.keys()).random();

            for (int j = 0; j < reinforcement.get(block); j++)
                payloadc.addPayload(new BuildPayload(block, state.rules.defaultTeam));
        }
    }

    public static void join(Player player) {
        var data = datas.get(player.uuid());
        if (data != null) {
            data.handlePlayerJoin(player);
            Bundle.send(player, "events.join.already-played");
        } else {
            datas.put(player.uuid(), new PlayerData(player));
            Bundle.send(player, "events.join.welcome");
        }
    }

    public static char icon(MappableContent content) {
        return Reflect.get(Iconc.class, Strings.kebabToCamel(content.getContentType().name() + "-" + content.name));
    }
}
