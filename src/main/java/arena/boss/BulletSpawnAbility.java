package arena.boss;

import arc.func.*;
import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

import static arc.math.Mathf.range;

public class BulletSpawnAbility extends Ability {

    public final Cons<Unit> bullet;

    public float time;
    public float delay;

    public BulletSpawnAbility(Floatc2 bullet) {
        this(bullet, 900f);
    }

    public BulletSpawnAbility(Floatc2 bullet, float delay) {
        this.bullet = unit -> bullet.get(unit.x, unit.y);
        this.time = Time.time + range(delay, delay * 2f);
        this.delay = delay;
    }

    @Override
    public void update(Unit unit) {
        if (time > Time.time) return;

        time = Time.time + delay;
        bullet.get(unit);
    }
}