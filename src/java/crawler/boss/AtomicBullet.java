package crawler.boss;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Timer;
import mindustry.content.Fx;
import mindustry.gen.Call;

public class AtomicBullet extends BossBullet {

    public float deg;
    public float dst = 180f;

    public AtomicBullet(float x, float y) {
        super(x, y, 1);

        for (float i = 0; i < 3; i += .05f) Timer.schedule(this::inst, i);
        BossBullets.timer(x, y, BossBullets::thorium);
    }

    public void inst() {
        float dx = x + Mathf.cosDeg(deg += 24f) * dst++;
        float dy = y + Mathf.sinDeg(deg) * dst++;
        Call.effect(Fx.instShoot, dx, dy, deg, Color.white);
    }
}
