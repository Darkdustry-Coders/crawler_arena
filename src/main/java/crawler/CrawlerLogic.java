package crawler;

import arc.Events;
import arc.math.Mathf;
import arc.util.Timer;
import crawler.ai.ReinforcementAI;
import crawler.boss.*;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;

import static arc.struct.Seq.with;
import static crawler.Bundle.*;
import static crawler.CrawlerVars.*;
import static crawler.Main.*;
import static crawler.PlayerData.datas;
import static crawler.boss.BossBullets.bullets;
import static mindustry.Vars.*;

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

    public static void gameOver() {
        datas.each(data -> Call.infoMessage(data.player.con, Bundle.get(state.wave > bossWave ? "events.victory" : "events.lose", data.locale)));

        BossBullets.timer(0f, 0f, (x, y) -> Events.fire(new GameOverEvent(state.wave > bossWave ? state.rules.defaultTeam : state.rules.waveTeam)));

        Call.hideHudText();

        for (int i = 0; i < world.width() * world.height() / 1000; i++) // boom!
            BossBullets.atomic(Mathf.random(world.unitWidth()), Mathf.random(world.unitHeight()));
    }

    public static void runWave() {
        state.wave++;
        statScaling += state.wave / statDiv;

        if (state.wave == bossWave) spawnBoss(); // during the boss battle do not spawn small enemies
        else if (state.wave > bossWave) gameOver(); // it is the end
        else {
            int totalEnemies = Mathf.ceil(Mathf.pow(crawlersExpBase, 1f + state.wave * crawlersRamp + Mathf.pow(state.wave, 2f) * extraCrawlersRamp) * Groups.player.size() * crawlersMultiplier);
            int spreadX = world.width() / 2 - 20, spreadY = world.height() / 2 - 20;

            for (var entry : enemyCuts) {
                int typeCount = totalEnemies / entry.value;
                totalEnemies -= typeCount;

                for (int i = 0; i < Math.min(typeCount, maxUnits); i++) spawnEnemy(entry.key, spreadX, spreadY);
            }

            for (int i = 0; i < Math.min(totalEnemies, maxUnits); i++) spawnEnemy(UnitTypes.crawler, spreadX, spreadY);

            isWaveGoing = true;
        }
    }

    public static void spawnEnemy(UnitType type, int spreadX, int spreadY) {
        var tile = spawnTile(spreadX, spreadY);
        var unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());

        unit.health = unit.maxHealth = unit.maxHealth * statScaling / 5;
    }

    public static void spawnBoss() {
        announce("events.boss");

        BossBullets.timer(world.width() * 4f, world.height() * 4f, (x, y) -> {
            BossBullets.impact(x, y); // some cool effects
            var boss = UnitTypes.eclipse.spawn(state.rules.waveTeam, x, y);

            // increasing armor to keep the bar boss working
            boss.armor(statScaling * Groups.player.size() * 24000f);
            boss.damageMultiplier = statScaling * 8f;

            boss.apply(StatusEffects.boss);

            var abilities = with(boss.abilities);

            abilities.add(new GroupSpawnAbility(UnitTypes.flare, 5, -64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.flare, 5, 64f, 64f));
            abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 3, 0, -96f));

            abilities.add(new BulletSpawnAbility(BossBullets::toxopidMount));
            abilities.add(new BulletSpawnAbility(BossBullets::corvusLaser, 1800f));
            abilities.add(new BulletSpawnAbility(BossBullets::fuseTitanium));
            abilities.add(new BulletSpawnAbility(BossBullets::fuseThorium));
            abilities.add(new BulletSpawnAbility(BossBullets::arcLight, 300f));
            abilities.add(new BulletSpawnAbility(BossBullets::atomic));

            boss.abilities(abilities.toArray());

            isWaveGoing = true;
        });
    }

    public static void spawnReinforcement() {
        Timer.schedule(() -> announce("events.aid"), 3f);

        for (int i = 0; i < state.wave; i++) {
            var unit = UnitTypes.mega.spawn(Team.derelict, Mathf.random(40f), world.unitHeight() / 2f + Mathf.range(120));
            unit.controller(new ReinforcementAI());
            unit.health = unit.maxHealth = Float.MAX_VALUE;

            var block = with(aidBlocks.keys()).random();

            var pay = (Payloadc) unit; // add blocks to unit payload component
            for (int j = 0; j < aidBlocks.get(block); j++)
                pay.addPayload(new BuildPayload(block, state.rules.defaultTeam));
        }
    }

    public static Tile spawnTile(int spreadX, int spreadY) {
        spreadX = Math.max(spreadX, 20);
        spreadY = Math.max(spreadY, 20);
        return switch (Mathf.random(3)) {
            case 0 -> world.tile(world.width() - tilesize, world.height() / 2 + Mathf.range(spreadY));
            case 1 -> world.tile(world.width() / 2 + Mathf.range(spreadX), world.height() - tilesize);
            case 2 -> world.tile(tilesize, world.height() / 2 + Mathf.range(spreadY));
            case 3 -> world.tile(world.width() / 2 + Mathf.range(spreadX), tilesize);
            default -> null;
        };
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