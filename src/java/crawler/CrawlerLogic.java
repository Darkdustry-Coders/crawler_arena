package crawler;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap.Entry;
import arc.struct.OrderedMap;
import arc.util.Timer;
import crawler.ai.ReinforcementAI;
import crawler.boss.BossBullets;
import crawler.boss.BulletSpawnAbility;
import crawler.boss.GroupSpawnAbility;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.game.EventType.GameOverEvent;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;

import static crawler.Bundle.bundled;
import static crawler.Bundle.sendToChat;
import static crawler.CrawlerVars.*;
import static crawler.Main.*;
import static crawler.PlayerData.datas;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class CrawlerLogic {

    public static void load() {
        rules.canGameOver = false;
        rules.waveTimer = false;
        rules.waves = true;
        rules.waitEnemies = true;
        rules.unitCap = unitCap;
        rules.modeName = "Crawler Arena";
    }

    public static void play() {
        state.wave = 0;
        state.rules = rules;
        Call.setRules(rules);

        rules.defaultTeam.cores().each(Building::kill);
        statScaling = 1f;

        BossBullets.bullets.clear(); // it can kill everyone after new game
        datas.clear(); // recreate PlayerData
        Timer.schedule(() -> {
            Groups.player.each(CrawlerLogic::join);
            firstWaveLaunched = false;
        }, 1f);
    }

    public static void gameOver() {
        String msg = state.wave > bossWave ? "events.victory" : "events.lose";
        PlayerData.each(data -> Call.infoMessage(data.player.con, Bundle.get(msg, data.locale)));

        Team team = state.wave > bossWave ? rules.defaultTeam : rules.waveTeam;
        BossBullets.timer(0f, 0f, (x, y) -> Events.fire(new GameOverEvent(team)));

        Call.hideHudText();

        for (int i = 0; i < world.width() * world.height() / 1000; i++) // boom!
            BossBullets.atomic(Mathf.random(world.unitWidth()), Mathf.random(world.unitHeight()));
    }

    public static void runWave() {
        state.wave++;
        statScaling += state.wave / statDiv;

        if (state.wave == bossWave) {
            spawnBoss();
            return; // during the boss battle do not spawn small enemies
        }

        if (state.wave > bossWave) {
            gameOver();
            return; // it is the end
        }

        int totalEnemies = Mathf.ceil(Mathf.pow(crawlersExpBase, 1f + state.wave * crawlersRamp + Mathf.pow(state.wave, 2f) * extraCrawlersRamp) * Groups.player.size() * crawlersMultiplier);
        int spreadX = world.width() / 2 - 20, spreadY = world.height() / 2 - 20;

        for (Entry<UnitType, Integer> entry : enemyCuts) {
            int typeCount = totalEnemies / entry.value;
            totalEnemies -= typeCount;

            for (int i = 0; i < Math.min(typeCount, maxUnits); i++) spawnEnemy(entry.key, spreadX, spreadY);
        }

        for (int i = 0; i < Math.min(totalEnemies, maxUnits); i++) spawnEnemy(UnitTypes.crawler, spreadX, spreadY);

        isWaveGoing = true;
    }

    public static void spawnEnemy(UnitType type, int spreadX, int spreadY) {
        Tile tile = spawnTile(spreadX, spreadY);
        Unit unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());

        unit.maxHealth(unit.maxHealth * statScaling / 5);
        unit.health(unit.maxHealth);
    }

    public static void spawnBoss() {
        PlayerData.each(data -> Call.announce(data.player.con, Bundle.get("events.boss", data.locale)));

        BossBullets.timer(world.width() * 4f, world.height() * 4f, (x, y) -> {
            BossBullets.impact(x, y); // some cool effects
            Unit boss = UnitTypes.eclipse.spawn(state.rules.waveTeam, x, y);

            // increasing armor to keep the bar boss working
            boss.armor(statScaling * Groups.player.size() * 24000f);
            boss.damageMultiplier = statScaling * 8f;

            boss.apply(StatusEffects.boss);

            boss.abilities.add(new GroupSpawnAbility(UnitTypes.flare, 5, -64f, 64f));
            boss.abilities.add(new GroupSpawnAbility(UnitTypes.flare, 5, 64f, 64f));
            boss.abilities.add(new GroupSpawnAbility(UnitTypes.zenith, 3, 0, -96f));

            boss.abilities.add(new BulletSpawnAbility(BossBullets::toxomount));
            boss.abilities.add(new BulletSpawnAbility(BossBullets::corvuslaser, 1800f));
            boss.abilities.add(new BulletSpawnAbility(BossBullets::fusetitanium));
            boss.abilities.add(new BulletSpawnAbility(BossBullets::fusethorium));
            boss.abilities.add(new BulletSpawnAbility(BossBullets::arclight, 300f));
            boss.abilities.add(new BulletSpawnAbility(BossBullets::atomic));

            isWaveGoing = true;
        });
    }

    public static void spawnReinforcement() {
        sendToChat("events.aid");

        for (int i = 0; i < state.wave; i++) {
            Unit unit = UnitTypes.mega.spawn(Team.derelict, Mathf.random(40f), world.unitHeight() / 2f + Mathf.range(120));
            unit.controller(new ReinforcementAI());
            unit.maxHealth(Float.MAX_VALUE);
            unit.health(unit.maxHealth);

            OrderedMap<Block, Integer> amount = Mathf.chance(.05f) ? aidBlocksRare : aidBlocks;
            Block block = amount.keys().toSeq().random();

            Payloadc pay = (Payloadc) unit; // add blocks to unit payload component
            for (int j = 0; j < amount.get(block); j++)
                pay.addPayload(new BuildPayload(block, state.rules.defaultTeam));
        }
    }

    public static Tile spawnTile(int spreadX, int spreadY) {
        spreadX = Math.max(spreadX, 20);
        spreadY = Math.max(spreadY, 20);
        return switch (Mathf.random(0, 3)) {
            case 0 -> world.tile(world.width() - 4, world.height() / 2 + Mathf.range(spreadY));
            case 1 -> world.tile(world.width() / 2 + Mathf.range(spreadX), world.height() - 4);
            case 2 -> world.tile(4, world.height() / 2 + Mathf.range(spreadY));
            case 3 -> world.tile(world.width() / 2 + Mathf.range(spreadX), 4);
            default -> null; // Because java sucks
        };
    }

    public static void join(Player player) {
        String uuid = player.uuid();
        if (datas.containsKey(uuid)) {
            datas.get(uuid).handlePlayerJoin(player);
            bundled(player, "events.join.already-played");
        } else {
            datas.put(uuid, new PlayerData(player));
            bundled(player, "events.join.welcome");
        }
    }
}
