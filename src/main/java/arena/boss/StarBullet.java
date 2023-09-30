package arena.boss;

import arc.audio.Sound;
import arc.util.Tmp;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Call;

import static mindustry.Vars.*;

public class StarBullet extends BossBullet {
    public final BulletType bullet;
    public final Sound sound;

    public final float step;
    public final int amount;
    public final float speed;
    public final float rotateSpeed;

    public float rotation;

    public StarBullet(float x, float y, int lifetime, int amount, float speed, BulletType bullet, Sound sound) {
        super(x, y, lifetime);

        this.step = 360f / amount;
        this.amount = amount;
        this.speed = speed;
        this.rotateSpeed = speed / amount;

        this.bullet = bullet;
        this.sound = sound;
    }

    @Override
    public void update() {
        super.update();
        rotation -= rotateSpeed;

        if (lifetime % amount == 0) Call.soundAt(sound, x, y, 0.8f, 1f);
        for (int i = 0; i < amount; i++)
            bullet.createNet(state.rules.waveTeam, x, y, rotation + i * step, bullet.damage, 1f, 1f);

        var target = Units.closestTarget(state.rules.waveTeam, x, y, Float.MAX_VALUE);
        if (target == null) return;

        add(Tmp.v1.set(target).sub(this).limit(speed)); // move to the nearest enemy
    }
}