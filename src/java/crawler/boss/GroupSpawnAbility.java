package crawler.boss;

import arc.func.Cons;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class GroupSpawnAbility extends Ability {

    public Cons<Unit> spawn;

    public float time;
    public float delay;

    public GroupSpawnAbility(UnitType unit, int amount, float x, float y) {
        this(unit, amount, x, y, 2400f);
    }

    public GroupSpawnAbility(UnitType unit, int amount, float x, float y, float delay) {
        this.spawn = u -> {
            float sx = u.x + Angles.trnsx(u.rotation, y, x), sy = u.y + Angles.trnsy(u.rotation, y, x);
            for (float deg = 0; deg < 360f; deg += 360f / amount) {
                float dx = sx + Mathf.cosDeg(deg) * unit.hitSize;
                float dy = sy + Mathf.sinDeg(deg) * unit.hitSize;
                Call.effect(Fx.mineHuge, dx, dy, 0f, Team.crux.color);
                unit.spawn(Team.crux, dx, dy);
            }
        };
        this.time = Time.time + Mathf.range(delay, 2 * delay);
        this.delay = delay;
    }

    @Override
    public void update(Unit unit) {
        if (time > Time.time) return;
        time = Time.time + delay;
        spawn.get(unit);
    }
}
