package arena.boss;

import arc.func.Cons;
import arc.math.Angles;
import arc.util.Time;
import arena.ai.BossAI;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

import static arc.math.Mathf.*;

public class GroupSpawnAbility extends Ability {

    public final Cons<Unit> spawn;

    public float time;
    public float delay;

    public GroupSpawnAbility(UnitType unit, int amount, float x, float y) {
        this(unit, amount, x, y, 900f);
    }

    public GroupSpawnAbility(UnitType type, int amount, float x, float y, float delay) {
        this.spawn = unit -> {
            float cx = unit.x + Angles.trnsx(unit.rotation, x, y), cy = unit.y + Angles.trnsy(unit.rotation, x, y);
            for (float deg = 0; deg < 360f; deg += 360f / amount)
                type.spawn(unit.team, cx + cosDeg(deg) * type.hitSize, cy + sinDeg(deg) * type.hitSize).controller(new BossAI());
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

    @Override
    public void death(Unit unit) {
        unit.team.data().units.each(Unit::kill);
    }
}