package crawler.boss;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Timer;
import mindustry.content.Fx;
import mindustry.gen.Call;

public class AtomicBullet extends BossBullet {

    public float deg;
    public float dst;

    public AtomicBullet(float x, float y) {
        super(x, y, 1);

        for (int i = 0; i < 3; i++)
            BossBullets.timer(x + Mathf.range(150f), y + Mathf.range(150f), BossBullets::thorium);
        for (float i = 0; i < 3; i += .05f) Timer.schedule(this::inst, i);
    }

    public void inst() {
        dst += 5f; // increase scope
        float dx = x + Mathf.cosDeg(deg += 24f) * dst;
        float dy = y + Mathf.sinDeg(deg) * dst;
        Call.effect(Fx.instShoot, dx, dy, deg, Color.white);
    }
}