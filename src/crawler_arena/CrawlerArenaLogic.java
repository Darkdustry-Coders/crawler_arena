package crawler_arena;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import arc.util.Timer;
import mindustry.content.UnitTypes;
import mindustry.core.Logic;
import mindustry.game.EventType.WaveEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Tile;

import static crawler_arena.CrawlerArenaMod.*;
import static crawler_arena.CrawlerVars.*;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class CrawlerArenaLogic extends Logic {

    // TODO а надо ли это?

    @Override
    public void skipWave() {
        runWave();
    }

    @Override
    public void runWave() {
        state.wave++;
        state.wavetime = state.rules.waveSpacing;

        statScaling = 1f + state.wave * statScalingNormal;

        if (state.wave == bossWave) {
            spawnBoss();
        } else {
            spawnEnemies();
        }

        Events.fire(new WaveEvent());
        Timer.schedule(() -> isWaveGoing = true, 1f);
    }

    public void spawnEnemies() {
        int totalEnemies = Math.min(Mathf.ceil(Mathf.pow(enemiesExpBase, 1f + wave * enemiesRamp + Mathf.pow(wave, 2f) * extraEnemiesRamp) * Groups.player.size() * enemiesMultiplier), enemiesCeiling);
        int totalTarget = maxUnits - keepCrawlers;

        int spreadX = Math.max(world.unitWidth() / 2 - 160 - state.wave * 10, 160);
        int spreadY = Math.max(world.unitHeight() / 2 - 160 - state.wave * 10, 160);

        ObjectMap<UnitType, Integer> enemies = new ObjectMap<>();
        for (Entry<UnitType, Integer> enemy : enemyCuts) {
            int typeCount = Math.min(totalEnemies / enemy.value, totalTarget / 2);
            totalTarget -= typeCount;
            totalEnemies -= typeCount * enemy.value / 2;

            enemies.put(enemy.key, typeCount);
        }

        enemies.put(UnitTypes.crawler, Math.min(totalEnemies, keepCrawlers));
        enemies.each((type, count) -> spawnEnemyGroup(type, count, spreadX, spreadY));
    }

    public void spawnBoss() {

    }

    public void spawnEnemyGroup(UnitType type, int count, int spreadX, int spreadY) {
        for (int i = 0; i < count; i++) spawnEnemy(type, spreadX, spreadY);
    }

    public void spawnEnemy(UnitType type, int spreadX, int spreadY) {
        Tile tile = getRandomSpawnTile(spreadX, spreadY);
        if (tile == null) return;

        Unit unit = type.spawn(state.rules.waveTeam, tile.worldx(), tile.worldy());
        unit.maxHealth *= statScaling * healthMultiplierBase;
        unit.health = unit.maxHealth;
    }

    public Tile getRandomSpawnTile(int spreadX, int spreadY) {
        return switch (Mathf.random(0, 3)) {
            case 0 -> world.tileWorld(world.unitWidth() - 32, world.unitHeight() / 2f + Mathf.range(spreadY));
            case 1 -> world.tileWorld(world.unitWidth() / 2f + Mathf.range(spreadX), world.unitHeight() - 32);
            case 2 -> world.tileWorld(32, world.unitHeight() / 2f + Mathf.range(spreadY));
            case 3 -> world.tileWorld(world.unitWidth() / 2f + Mathf.range(spreadX), 32);
            default -> null;
        };
    }
}
