package crawler;

import arc.math.Mathf;
import arc.struct.ObjectMap.Entry;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Tile;

import static crawler.MainNew.*;
import static crawler.PlayerData.*;
import static crawler.Bundle.*;
import static crawler.CrawlerVarsNew.*;
import static mindustry.Vars.*;

public class CrawlerLogicNew {

    public static void load() {
        rules.canGameOver = false;
        rules.waveTimer = false;
        rules.waves = true;
        rules.unitCap = 96;
        rules.modeName = "Crawler Arena";
    }

    public static void play() {
        Call.setRules(rules);
        state.rules = rules;

        rules.defaultTeam.cores().each(Building::kill);
        statScaling = 1f;
        isWaveGoing = true;

        datas.clear(); // recreate PlayerData
        Groups.player.each(CrawlerLogicNew::join);
    }

    public static void runWave() {
        isWaveGoing = true;
        state.wave++;

        int totalEnemies = (int) Mathf.pow(enemiesBase, 1f + state.wave * enemiesRamp + Mathf.pow(state.wave, 2f) * extraEnemiesRamp) * Groups.player.size();
        int spreadX = world.width() / 2 - 20 - state.wave, spreadY = world.height() / 2 - 20 - state.wave;

        for (Entry<UnitType, Integer> entry : enemy) {
            int typeCount = totalEnemies / entry.value;
            totalEnemies -= typeCount;

            for (int i = 0; i < typeCount; i++) spawnEnemy(entry.key, spreadX, spreadY);
        }
    }

    public static void spawnEnemy(UnitType type, int spreadX, int spreadY) {
        Tile tile = switch (Mathf.random(0, 3)) {
            case 0 -> world.tile(world.width() - 4, world.height() / 2 + Mathf.range(spreadY));
            case 1 -> world.tile(world.width() / 2 + Mathf.range(spreadX), world.height() - 4);
            case 2 -> world.tile(4, world.height() / 2 + Mathf.range(spreadY));
            case 3 -> world.tile(world.width() / 2 + Mathf.range(spreadX), 4);
            default -> null;
        };

        Unit unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());
        unit.controller(new ArenaAI());
        unit.maxHealth(unit.maxHealth * statScaling / 5);
        unit.health(unit.maxHealth);
    }

    public static void spawnReinforcement() {
        // there is nothing yet
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

    public static int waveMoney() { // what the
        return (int) Mathf.pow(moneyBase, 1f + state.wave * moneyRamp + Mathf.pow(state.wave, 2) * extraMoneyRamp) * 4;
    }
}
