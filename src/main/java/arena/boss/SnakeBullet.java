package arena.boss;

import arc.audio.Sound;
import arc.util.Tmp;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Call;
import mindustry.gen.Unit;

import static arc.math.Mathf.*;
import static mindustry.Vars.state;
import static mindustry.content.UnitTypes.mono;

public class SnakeBullet extends BossBullet {

    public BulletType bullet;
    public Sound sound;

    public float speed;
    public float rotateSpeed;
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

        var target = Units.closestTarget(state.rules.waveTeam, x, y, 999999f, unit -> unit.type == mono); // priority target - mono
        if (target == null)
            target = Units.closestTarget(state.rules.waveTeam, x, y, 999999f); // if there is no mono on the map, attack everyone
        if (target == null) return;

        Tmp.v1.set(target).sub(this); // find the direction to the nearest enemy...
        if (Tmp.v1.len() > speed) Tmp.v1.setLength(speed);

        float rad = rotation * degRad;
        Tmp.v2.set(cos(rad), sin(rad));
        Tmp.v2.rotateTo(Tmp.v1.angle(), rotateSpeed); // ...and turn to it by rotateSpeed degrees

        rotation = Tmp.v2.angle(); // move to the nearest enemy
        add(Tmp.v2.setLength(Tmp.v1.len()));

        if (target instanceof Unit unit) unit.apply(StatusEffects.electrified, 1f);
    }
}