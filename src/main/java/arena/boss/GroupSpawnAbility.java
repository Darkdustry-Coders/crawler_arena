package arena.boss;

import arc.func.Cons;
import arc.math.Angles;
import arc.util.Time;
import arena.ai.BossAI;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

import static arc.math.Mathf.*;
import static mindustry.Vars.state;

public class GroupSpawnAbility extends Ability {

    public Cons<Unit> spawn;

    public float time;
    public float delay;

    public GroupSpawnAbility(UnitType unit, int amount, float x, float y) {
        this(unit, amount, x, y, 1800f);
    }

    public GroupSpawnAbility(UnitType type, int amount, float x, float y, float delay) {
        this.spawn = unit -> {
            float sx = unit.x + Angles.trnsx(unit.rotation, y, x), sy = unit.y + Angles.trnsy(unit.rotation, y, x);
            for (float deg = 0; deg < 360f; deg += 360f / amount) {
                float dx = sx + cosDeg(deg) * type.hitSize;
                float dy = sy + sinDeg(deg) * type.hitSize;
                var spawned = type.spawn(state.rules.waveTeam, dx, dy);
                spawned.controller(new BossAI());
            }
        };
        this.time = Time.time + range(delay, 2 * delay);
        this.delay = delay;
    }

    @Override
    public void update(Unit unit) {
        if (time > Time.time) return;
        time = Time.time + delay;
        spawn.get(unit);
    }
}