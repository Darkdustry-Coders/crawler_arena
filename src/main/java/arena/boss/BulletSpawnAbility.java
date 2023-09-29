package arena.boss;

import arc.func.Floatc2;
import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

public class BulletSpawnAbility extends Ability {
    public final Floatc2 bullet;
    public final float delay;

    public float timer;

    public BulletSpawnAbility(Floatc2 bullet) {
        this(bullet, 600f);
    }

    public BulletSpawnAbility(Floatc2 bullet, float delay) {
        this.bullet = bullet;
        this.delay = delay;
    }

    @Override
    public void update(Unit parent) {
        timer += Time.delta;

        if (timer >= delay) {
            bullet.get(parent.x, parent.y);
            timer = 0f;
        }
    }
}