package arena.boss;

import arc.func.Cons;
import arc.func.Cons2;
import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

import static arc.math.Mathf.range;

public class BulletSpawnAbility extends Ability {

    public Cons<Unit> bullet;

    public float time;
    public float delay;

    public BulletSpawnAbility(Cons2<Float, Float> bullet) {
        this(bullet, 1200f);
    }

    public BulletSpawnAbility(Cons2<Float, Float> bullet, float delay) {
        this.bullet = unit -> bullet.get(unit.x, unit.y);
        this.time = Time.time + range(delay, 2 * delay);
        this.delay = delay;
    }

    @Override
    public void update(Unit unit) {
        if (time > Time.time) return;
        time = Time.time + delay;
        bullet.get(unit);
    }
}