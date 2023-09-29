package arena.boss;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Timer;
import mindustry.content.Fx;
import mindustry.gen.Call;

public class AtomicBullet extends BossBullet {
    public float rotation;
    public float distance;

    public AtomicBullet(float x, float y) {
        super(x, y, 1);

        for (int i = 0; i < 3; i++)
            BossBullets.timer(x + Mathf.range(150f), y + Mathf.range(150f), BossBullets::thorium);

        for (float i = 0; i < 3; i += .05f)
            Timer.schedule(this::inst, i);
    }

    public void inst() {
        rotation += 24f;
        distance += 5f; // increase scope

        Call.effect(Fx.instShoot, x + Mathf.cosDeg(rotation) * distance, y + Mathf.sinDeg(rotation) * distance, rotation, Color.white);
    }
}