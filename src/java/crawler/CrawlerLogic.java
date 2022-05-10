package crawler;

import arc.math.Mathf;
import arc.struct.OrderedMap;
import arc.struct.ObjectMap.Entry;
import arc.util.Timer;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Payloadc;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;

import static crawler.Main.*;
import static crawler.PlayerData.*;
import static crawler.Bundle.*;
import static crawler.CrawlerVars.*;
import static mindustry.Vars.*;

public class CrawlerLogic {

    public static void load() {
        rules.canGameOver = false;
        rules.waveTimer = false;
        rules.waves = true;
        rules.unitCap = unitCap;
        rules.modeName = "Crawler Arena";
    }

    public static void play() {
        Call.setRules(rules);
        state.rules = rules;

        rules.defaultTeam.cores().each(Building::kill);
        isWaveGoing = true;
        statScaling = 1f;

        datas.clear(); // recreate PlayerData
        Timer.schedule(() -> Groups.player.each(CrawlerLogic::join), 1f);
    }

    public static void runWave() {
        isWaveGoing = true;
        state.wave++;
        statScaling += state.wave / statDiv;

        if(state.wave == bossWave) {
            spawnBoss();
            return; // during the boss battle do not spawn small enemies
        }

        int totalEnemies = (int) Mathf.pow(enemiesBase, 1f + state.wave * enemiesRamp + Mathf.pow(state.wave, 2f) * extraEnemiesRamp) * Groups.player.size();
        int spreadX = world.width() / 2 - 20 - state.wave, spreadY = world.height() / 2 - 20 - state.wave;

        for (Entry<UnitType, Integer> entry : enemy) {
            int typeCount = totalEnemies / entry.value;
            totalEnemies -= typeCount;

            for (int i = 0; i < Math.min(typeCount, maxUnits); i++) spawnEnemy(entry.key, spreadX, spreadY);
        }
    }

    public static void spawnEnemy(UnitType type, int spreadX, int spreadY) {
        Tile tile = spawnTile(spreadX, spreadY);
        Unit unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());

        unit.controller(new ArenaAI());
        unit.maxHealth(unit.maxHealth * statScaling / 5);
        unit.health(unit.maxHealth);
    }

    public static void spawnBoss() {
        Tile tile = spawnTile(20, 20);
        Unit boss = UnitTypes.reign.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());

        boss.controller(new ArenaAI());
        boss.maxHealth(boss.maxHealth * Groups.player.size() * 6f);
        boss.health(boss.maxHealth);
        boss.abilities.add(new UnitSpawnAbility(UnitTypes.scepter, 300f, -32f, -32f));
    }

    public static void spawnReinforcement() {
        sendToChat("events.aid");

        for (int i = 0; i < state.wave; i++) {
            Unit unit = UnitTypes.mega.spawn(Team.blue, 0, world.unitHeight() / 2f + Mathf.range(120));
            unit.controller(new ReinforcementAI());
            unit.maxHealth(Float.MAX_VALUE);
            unit.health(unit.maxHealth);

            OrderedMap<Block, Integer> amount = Mathf.chance(.05d) ? aidBlocksRare : aidBlocks;
            Block block = amount.keys().toSeq().random();

            Payloadc pay = (Payloadc) unit; // add blocks to unit payload component
            for (int j = 0; j < amount.get(block); j++) pay.addPayload(new BuildPayload(block, state.rules.defaultTeam));
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
            default -> null;
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
