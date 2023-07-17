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

    public float deg;
    public float step;
    public int amount;
    public float speed;
    public float rotateSpeed;

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

        deg -= rotateSpeed;
        if (lifetime % amount == 0) Call.soundAt(sound, x, y, 0.8f, 1f);
        for (int i = 0; i < amount; i++)
            bullet.createNet(state.rules.waveTeam, x, y, deg + i * step, bullet.damage, 1f, 1f);

        var target = Units.closestTarget(state.rules.waveTeam, x, y, 999999f);
        if (target == null) return;

        Tmp.v1.set(target).sub(this);
        if (Tmp.v1.len() > speed) Tmp.v1.setLength(speed);
        add(Tmp.v1); // move to the nearest enemy
    }
}