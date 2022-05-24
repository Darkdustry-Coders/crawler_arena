package crawler;

import arc.math.Mathf;
import arc.struct.OrderedMap;
import arc.struct.ObjectMap.Entry;
import arc.util.Timer;
import crawler.boss.BossBullets;
import crawler.boss.BulletSpawnAbility;
import mindustry.content.StatusEffects;
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
        rules.waitEnemies = true;
        rules.unitCap = unitCap;
        rules.modeName = "Crawler Arena";
    }

    public static void play() {
        state.wave = 0;
        state.rules = rules;
        Call.setRules(rules);

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

        if(state.wave % bossWave == 0) {
            spawnBoss();
            return; // during the boss battle do not spawn small enemies
        }

        int totalEnemies = Math.max((int) Mathf.pow(enemiesBase, 1f + state.wave * enemiesRamp + Mathf.pow(state.wave, 2f) * extraEnemiesRamp) * Groups.player.size(), 1);
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
        PlayerData.each(data -> Call.announce(data.player.con, Bundle.get("events.boss", data.locale)));

        BossBullets.timer(world.width() * 4f, world.height() * 4f, (x, y) -> {
            BossBullets.impact(x, y); // some cool effects
            Unit boss = UnitTypes.eclipse.spawn(state.rules.waveTeam, x, y);

            boss.controller(new ArenaAI()); // increasing armor to keep the bar boss working
            boss.armor(statScaling * Groups.player.size() * 30000f);
            boss.damageMultiplier = statScaling * 10f;

            boss.apply(StatusEffects.boss);
            boss.apply(StatusEffects.overclock);

            boss.abilities.add(new UnitSpawnAbility(UnitTypes.horizon, 180f, -16f, 16f));
            boss.abilities.add(new UnitSpawnAbility(UnitTypes.horizon, 180f, 16f, 16f));
            boss.abilities.add(new UnitSpawnAbility(UnitTypes.zenith, 240f, 0, -32f));

            boss.abilities.add(new BulletSpawnAbility((x1, y1) -> BossBullets.timer(x1, y1, BossBullets::toxomount)));
            boss.abilities.add(new BulletSpawnAbility((x1, y1) -> BossBullets.timer(x1, y1, BossBullets::fusetitanium)));
            boss.abilities.add(new BulletSpawnAbility((x1, y1) -> BossBullets.timer(x1, y1, BossBullets::fusethorium)));
            boss.abilities.add(new BulletSpawnAbility((x1, y1) -> BossBullets.arclight(x1, y1), 600f, 300f));
            boss.abilities.add(new BulletSpawnAbility((x1, y1) -> BossBullets.atomic(x1, y1)));
        });
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
