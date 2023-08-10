package arena.boss;

import arc.audio.Sound;
import arc.math.Mathf;
import arc.util.Tmp;
import mindustry.content.StatusEffects;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Call;
import mindustry.gen.Statusc;

import static mindustry.Vars.state;

public class SnakeBullet extends BossBullet {

    public final BulletType bullet;
    public final Sound sound;

    public final float speed;
    public final float rotateSpeed;

    public float rotation;

    public SnakeBullet(float x, float y, int lifetime, float speed, float rotateSpeed, BulletType bullet, Sound sound) {
        super(x, y, lifetime);

        this.speed = speed;
        this.rotateSpeed = rotateSpeed;

        this.bullet = bullet;
        this.sound = sound;
    }

    @Override
    public void update() {
        super.update();

        if (lifetime % 2 == 0) Call.soundAt(sound, x, y, 0.8f, 1f);
        bullet.createNet(state.rules.waveTeam, x, y, rotation + 195, bullet.damage, 1f, 1f);
        bullet.createNet(state.rules.waveTeam, x, y, rotation - 195, bullet.damage, 1f, 1f);

        var target = Units.closestTarget(state.rules.waveTeam, x, y, Float.MAX_VALUE);
        if (target == null) return;

        Tmp.v1.set(target).sub(this); // find the direction to the closest enemy...
        Tmp.v1.limit(speed);

        Tmp.v2.set(Mathf.cosDeg(rotation), Mathf.sinDeg(rotation));
        Tmp.v2.rotateTo(Tmp.v1.angle(), rotateSpeed); // ...and turn to it by rotateSpeed degrees

        rotation = Tmp.v2.angle();
        add(Tmp.v2.setLength(Tmp.v1.len())); // move to the closest enemy

        if (target instanceof Statusc statusc)
            statusc.apply(StatusEffects.electrified, 60f);
    }
}