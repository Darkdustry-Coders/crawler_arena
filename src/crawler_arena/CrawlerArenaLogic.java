package crawler_arena;

import arc.Events;
import mindustry.core.Logic;
import mindustry.game.EventType.WaveEvent;

import static crawler_arena.CrawlerArenaMod.*;
import static crawler_arena.CrawlerVars.bossWave;
import static crawler_arena.CrawlerVars.statScalingNormal;
import static mindustry.Vars.state;

public class CrawlerArenaLogic extends Logic {

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
            return;
        }

        spawnEnemies();

        Events.fire(new WaveEvent());
    }

    public void spawnEnemies() {
        // TODO
    }

    public void spawnBoss() {
        // TODO
    }
}
