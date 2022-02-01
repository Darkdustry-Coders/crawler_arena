package crawler_arena;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import arc.util.pooling.Pools;
import mindustry.ai.types.FlyingAI;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.Units;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.entities.bullet.SapBulletType;
import mindustry.entities.units.StatusEntry;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;

import java.lang.reflect.Field;

import static crawler_arena.CVars.*;
import static mindustry.Vars.*;

public class CrawlerArenaMod extends Plugin {

    public static boolean gameIsOver = true, waveIsOver = false, firstWaveLaunched = false;

    public static int worldWidth, worldHeight, worldCenterX, worldCenterY, wave = 1;
    public static float statScaling = 1f;

    public static ObjectIntMap<String> money = new ObjectIntMap<>();
    public static ObjectMap<String, UnitType> units = new ObjectMap<>();
    public static ObjectIntMap<String> unitIDs = new ObjectIntMap<>();

    public static long timer = Time.millis();

    @Override
    public void init() {
        content.units().each(u -> u.defaultController = FlyingAI::new);
        UnitTypes.crawler.defaultController = ArenaAI::new;
        UnitTypes.atrax.defaultController = ArenaAI::new;
        UnitTypes.spiroct.defaultController = ArenaAI::new;
        UnitTypes.arkyid.defaultController = ArenaAI::new;
        UnitTypes.toxopid.defaultController = ArenaAI::new;

        UnitTypes.poly.defaultController = SwarmAI::new;
        UnitTypes.mega.defaultController = ReinforcementAI::new;

        UnitTypes.scepter.defaultController = ArenaAI::new;
        UnitTypes.reign.defaultController = ArenaAI::new;

        UnitTypes.risso.flying = true;
        UnitTypes.minke.flying = true;
        UnitTypes.bryde.flying = true;
        UnitTypes.sei.flying = true;
        UnitTypes.omura.flying = true;

        UnitTypes.retusa.flying = true;
        UnitTypes.oxynoe.flying = true;
        UnitTypes.cyerce.flying = true;
        UnitTypes.aegires.flying = true;
        UnitTypes.navanax.flying = true;

        UnitTypes.crawler.maxRange = 8000f;
        UnitTypes.atrax.maxRange = 8000f;
        UnitTypes.spiroct.maxRange = 8000f;
        UnitTypes.arkyid.maxRange = 8000f;
        UnitTypes.toxopid.maxRange = 8000f;
        UnitTypes.reign.maxRange = 8000f;

        UnitTypes.poly.maxRange = 2000f;

        UnitTypes.reign.speed = 2.5f;

        UnitTypes.poly.abilities.add(new UnitSpawnAbility(UnitTypes.poly, 480f, 0f, -32f));
        UnitTypes.poly.health = 125f;
        UnitTypes.poly.speed = 1.5f;

        UnitTypes.arkyid.weapons.each(w -> {
            if (w.bullet instanceof SapBulletType sap) sap.sapStrength = 0f;
        });

        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> state.rules.defaultTeam.cores().each(Building::kill));

            Core.app.post(() -> {
                state.rules.canGameOver = false;
                state.rules.waveTimer = false;
                state.rules.waves = true;
                state.rules.unitCap = unitCap;
                state.rules.modeName = "Arena";
                Call.setRules(state.rules);
                newGame();
            });

            worldWidth = world.width() * tilesize;
            worldHeight = world.height() * tilesize;
            worldCenterX = worldWidth / 2;
            worldCenterY = worldHeight / 2;
            firstWaveLaunched = false;
            waveIsOver = true;
            timer = Time.millis();
        });

        Events.on(PlayerJoin.class, event -> {
            if (!money.containsKey(event.player.uuid()) || !units.containsKey(event.player.uuid())) {
                Bundle.bundled(event.player, "events.join.welcome");
                money.put(event.player.uuid(), (int) (money.get(event.player.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                units.put(event.player.uuid(), UnitTypes.dagger);
                respawnPlayer(event.player);
            } else {
                Bundle.bundled(event.player, "events.join.already-played");
                if (unitIDs.containsKey(event.player.uuid())) {
                    Unit swapTo = Groups.unit.getByID(unitIDs.get(event.player.uuid()));
                    if (swapTo != null) {
                        if (swapTo.getPlayer() != null && unitIDs.containsKey(swapTo.getPlayer().uuid())) {
                            Player intruder = swapTo.getPlayer();
                            Unit swapIntruderTo = Groups.unit.getByID(unitIDs.get(intruder.uuid()));
                            if (swapIntruderTo != null) {
                                intruder.unit(swapIntruderTo);
                            } else {
                                intruder.clearUnit();
                            }
                        }
                        Timer.schedule(() -> event.player.unit(swapTo), 1f);
                    }
                } else {
                    respawnPlayer(event.player);
                }
            }
        });

        Events.run(Trigger.update, () -> {
            if (gameIsOver) return;

            if (!Groups.unit.contains(u -> u.team == state.rules.defaultTeam)) {
                gameIsOver = true;
                if (wave > bossWave) {
                    Bundle.sendToChat("events.gameover.win");
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.defaultTeam)), 2f);
                } else {
                    Bundle.sendToChat("events.gameover.lose");
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.waveTeam)), 2f);
                }
                return;
            }

            Groups.player.each(p -> Call.setHudText(p.con, Bundle.format("labels.money", Bundle.findLocale(p), money.get(p.uuid()))));

            if (Mathf.chance(1f * tipChance * Time.delta)) Bundle.sendToChat("events.tip.info");
            if (Mathf.chance(1f * tipChance * Time.delta)) Bundle.sendToChat("events.tip.upgrades");

            if (!Groups.unit.contains(u -> u.team == state.rules.waveTeam) && !waveIsOver) {
                if (wave < reinforcementMinWave || wave % reinforcementSpacing != 0) {
                    Bundle.sendToChat("events.wave", (int) waveDelay);
                    Timer.schedule(this::nextWave, waveDelay);
                } else {
                    Bundle.sendToChat("events.next-wave", (int) (Math.min(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp, reinforcementWaveDelayMax)));
                    Timer.schedule(this::spawnReinforcements, 2.5f);
                    Timer.schedule(this::nextWave, Math.min(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp, reinforcementWaveDelayMax));
                }
                Groups.player.each(p -> {
                    respawnPlayer(p);
                    money.put(p.uuid(), (int) (money.get(p.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                });
                waveIsOver = true;
            }
            if (!waveIsOver) {
                enemyTypes.each(type -> type.speed += enemySpeedBoost * Time.delta * statScaling);
            }
        });

        netServer.admins.addActionFilter(action -> action.type != Administration.ActionType.breakBlock && action.type != Administration.ActionType.placeBlock);

        Log.info("Crawler Arena loaded.");
    }

    public void newGame() {
        if (firstWaveLaunched) return;
        if (Groups.player.size() == 0) {
            Timer.schedule(this::newGame, 5f);
            gameIsOver = true;
            return;
        }

        reset();
        Timer.schedule(() -> gameIsOver = false, 5f);

        Bundle.sendToChat("events.first-wave", (int) firstWaveDelay);
        Timer.schedule(this::nextWave, firstWaveDelay);
        firstWaveLaunched = true;
        waveIsOver = true;
    }

    public void spawnReinforcements() {
        Bundle.sendToChat("events.aid");
        Seq<Unit> megas = new Seq<>();
        ObjectMap<Block, Integer> blocks = new ObjectMap<>();
        int megasFactor = (int) Math.min(wave * reinforcementScaling * statScaling, reinforcementMax);

        for (int i = 0; i < megasFactor; i += reinforcementFactor) {
            Unit u = UnitTypes.mega.spawn(reinforcementTeam, 32, worldCenterY + Mathf.random(-80, 80));
            u.health = Float.MAX_VALUE;
            megas.add(u);
        }

        for (int i = 0; i < megas.size; i++) {
            Block block;
            boolean rare = false;
            if (Mathf.chance(rareAidChance / aidBlockAmounts.size)) {
                block = Seq.with(rareAidBlockAmounts.keys()).random();
                rare = true;
            } else {
                block = Seq.with(aidBlockAmounts.keys()).random();
            }

            if (guaranteedAirdrops.contains(block) || Mathf.chance(blockDropChance)) {
                int blockAmount = rare ? rareAidBlockAmounts.get(block) : aidBlockAmounts.get(block);
                int range = 10, x = 0, y = 0, j = 0;
                IntSeq valids = new IntSeq();

                while ((j < maxAirdropSearches && valids.size < blockAmount) || world.tile(x, y) == null) {
                    x = world.width() / 2 + Mathf.random(-range, range);
                    y = world.height() / 2 + Mathf.random(-range, range);
                    boolean valid = true;
                    for (int xi = x - (block.size - 1) / 2; xi <= x + block.size / 2; xi++) {
                        for (int yi = y - (block.size - 1) / 2; yi <= y + block.size / 2; yi++) {
                            if (world.build(xi, yi) != null || valids.contains(Point2.pack(xi, yi))) {
                                valid = false;
                                break;
                            }
                        }
                        if (!valid) break;
                    }
                    valid = valid && !Units.anyEntities(x * tilesize + block.offset - block.size * tilesize / 2f, y * tilesize + block.offset - block.size * tilesize / 2f, block.size * tilesize, block.size * tilesize);
                    if (valid) valids.add(Point2.pack(x, y));
                    range++;
                    j++;
                }

                valids.each(v -> {
                    Point2 unpacked = Point2.unpack(v);
                    float xf = unpacked.x * tilesize;
                    float yf = unpacked.y * tilesize;
                    Call.effect(Fx.blockCrash, xf, yf, 0, Color.white, block);
                    Time.run(100f, () -> {
                        Call.soundAt(Sounds.explosionbig, xf, yf, 1, 1);
                        Call.effect(Fx.spawnShockwave, xf, yf, block.size * 60f, Color.white);
                        world.tileWorld(xf, yf).setNet(block, state.rules.defaultTeam, 0);
                    });
                });
                blocks.put(block, 0);
            } else {
                blocks.put(block, rare ? rareAidBlockAmounts.get(block) : aidBlockAmounts.get(block));
            }
        }

        blocks.each((block, amount) -> {
            for (int i = 0; i < amount; i++) {
                if (megas.get(megas.size - 1) instanceof Payloadc pay)
                    pay.addPayload(new BuildPayload(block, state.rules.defaultTeam));
            }
            megas.remove(megas.size - 1);
        });
    }

    public void respawnPlayer(Player p) {
        if (p.dead() || p.unit().id != unitIDs.get(p.uuid())) {
            Unit oldUnit = Groups.unit.getByID(unitIDs.get(p.uuid()));
            if (oldUnit != null && oldUnit != p.unit()) oldUnit.kill();

            Tile tile = world.tile(worldCenterX / 8 + Mathf.random(-3, 3), worldCenterY / 8 + Mathf.random(-3, 3));
            UnitType type = units.get(p.uuid(), () -> UnitTypes.dagger);

            if (!type.flying && tile.solid()) {
                tile.removeNet();
            }

            Unit unit = type.spawn(tile.worldx(), tile.worldy());
            setUnit(unit);
            p.unit(unit);
            unitIDs.put(p.uuid(), unit.id);
            return;
        }

        if (p.unit().health < p.unit().maxHealth) {
            p.unit().heal();
            Bundle.bundled(p, "events.heal", Pal.heal);
        }
    }


    public void applyStatus(Unit unit, float duration, int amount, StatusEffect... effects) {
        Seq<StatusEntry> entries = new Seq<>();
        for (int i = 0; i < amount; i++) {
            for (StatusEffect effect : effects) {
                StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
                entry.set(effect, duration);
                entries.add(entry);
            }
        }

        for (Field field : unit.getClass().getFields()) {
            if (field.getName().equals("statuses")) {
                try {
                    ((Seq<StatusEntry>) field.get(unit)).addAll(entries);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void applyStatus(Unit unit, float duration, StatusEffect... effects) {
        applyStatus(unit, duration, 1, effects);
    }

    public void spawnEnemy(UnitType unit, int spX, int spY) {
        int sX = 32;
        int sY = 32;
        switch (Mathf.random(0, 3)) {
            case 0 -> {
                sX = worldWidth - 32;
                sY = worldCenterY + Mathf.random(-spY, spY);
            }
            case 1 -> {
                sX = worldCenterX + Mathf.random(-spX, spX);
                sY = worldHeight - 32;
            }
            case 2 -> sY = worldCenterY + Mathf.random(-spY, spY);
            case 3 -> sX = worldCenterX + Mathf.random(-spX, spX);
        }

        Unit u = unit.spawn(state.rules.waveTeam, sX, sY);
        u.armor = 0f;
        u.maxHealth *= statScaling * healthMultiplierBase;
        u.health = u.maxHealth;

        if (unit == UnitTypes.reign) {
            u.apply(StatusEffects.boss);
            if (Groups.player.size() > bossT1Cap) {
                u.apply(StatusEffects.overclock);
            }
            if (Groups.player.size() > bossT2Cap) {
                u.apply(StatusEffects.overdrive);
            }
            if (Groups.player.size() > bossT3Cap) {
                applyStatus(u, Float.MAX_VALUE, StatusEffects.overdrive, StatusEffects.overclock);
            }
            float totalHealth = 0f;
            for (Unit un : Groups.unit) {
                totalHealth += un.maxHealth * (un.team == state.rules.defaultTeam ? 1 : 0);
            }
            if (totalHealth >= bossBuffThreshold) {
                applyStatus(u, Float.MAX_VALUE, (int) totalHealth / bossBuffThreshold, StatusEffects.overdrive, StatusEffects.overclock);
            }
            u.maxHealth *= bossHealthMultiplier * Mathf.sqrt(Groups.player.size());
            u.health = u.maxHealth;
            u.abilities.add(new UnitSpawnAbility(UnitTypes.scepter, bossScepterDelayBase / Groups.player.size(), 0, -32));
        }
    }

    public void nextWave() {
        wave++;
        state.wave = wave;
        statScaling = 1f + wave * statScalingNormal;
        Timer.schedule(() -> waveIsOver = false, 1f);

        int crawlers = Mathf.ceil(Mathf.pow(crawlersExpBase, 1f + wave * crawlersRamp + Mathf.pow(wave, 2f) * extraCrawlersRamp) * Groups.player.size() * crawlersMultiplier);

        if (wave == bossWave - 5) Bundle.sendToChat("events.good-game");
        else if (wave == bossWave - 3) Bundle.sendToChat("events.what-so-long");
        else if (wave == bossWave - 1) Bundle.sendToChat("events.why-alive");
        else if (wave == bossWave) {
            Bundle.sendToChat("events.boss");
            spawnEnemy(UnitTypes.reign, 32, 32);
            return;
        } else if (wave == bossWave + 1) {
            Bundle.sendToChat("events.victory", Time.timeSinceMillis(timer));
        } else if (wave > bossWave + 1) {
            gameIsOver = true;
            Bundle.sendToChat("events.gameover.win");
            Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.defaultTeam)), 2f);
            return;
        }

        if (crawlers > crawlersCeiling) {
            crawlers = crawlersCeiling;
        }

        UnitTypes.crawler.health += crawlerHealthRamp * wave * statScaling;
        UnitTypes.crawler.speed += crawlerSpeedRamp * wave * statScaling;

        int spreadX = Math.max(worldCenterX - 160 - wave * 10, 160);
        int spreadY = Math.max(worldCenterY - 160 - wave * 10, 160);

        ObjectIntMap<UnitType> typeCounts = new ObjectIntMap<>();
        int totalTarget = maxUnits - keepCrawlers;
        for (UnitType type : enemyTypes) {
            int typeCount = Math.min(crawlers / enemyCrawlerCuts.get(type), totalTarget / 2);
            totalTarget -= typeCount;
            typeCounts.put(type, typeCount);
            crawlers -= typeCount * enemyCrawlerCuts.get(type) / 2;
            type.speed = defaultEnemySpeeds.get(type, 1f);
        }
        crawlers = Math.min(crawlers, keepCrawlers);

        typeCounts.forEach(entry -> spawnEnemies(entry.key, entry.value, spreadX, spreadY));
        spawnEnemies(UnitTypes.crawler, crawlers, spreadX, spreadY);
    }

    public void spawnEnemies(UnitType unit, int amount, int spX, int spY) {
        for (int i = 0; i < amount; i++) spawnEnemy(unit, spX, spY);
    }

    public void setUnit(Unit unit, boolean ultraEligible) {
        if (unit.type == UnitTypes.crawler) {
            unit.maxHealth = playerCrawlerHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerCrawlerArmor;
            unit.abilities.add(new UnitSpawnAbility(UnitTypes.crawler, playerCrawlerCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        } else if (unit.type == UnitTypes.mono) {
            unit.maxHealth = playerMonoHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerMonoArmor;
            unit.abilities.add(new UnitSpawnAbility(playerMonoSpawnTypes.random(), playerMonoCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        } else if (unit.type == UnitTypes.poly) {
            unit.maxHealth = playerPolyHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerPolyArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            unit.abilities.each(ability -> {
                if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = playerPolyCooldown;
            });
        } else if (unit.type == UnitTypes.omura) {
            unit.maxHealth = playerOmuraHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerOmuraArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            unit.abilities.each(ability -> {
                if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = playerOmuraCooldown;
            });
        } else if (ultraEligible && unit.type == UnitTypes.dagger && Mathf.chance(ultraDaggerChance)) {
            unit.maxHealth = ultraDaggerHealth;
            unit.health = unit.maxHealth;
            unit.armor = ultraDaggerArmor;
            unit.abilities.add(new UnitSpawnAbility(UnitTypes.dagger, ultraDaggerCooldown, 0f, -1f));
            applyStatus(unit, Float.MAX_VALUE, 3, StatusEffects.overclock, StatusEffects.overdrive, StatusEffects.boss);
        }
        unit.controller(new FlyingAI());
    }

    public void setUnit(Unit unit) {
        setUnit(unit, false);
    }

    public void reset() {
        wave = 1;
        state.wave = 1;
        statScaling = 1f;
        UnitTypes.crawler.speed = crawlerSpeedBase;
        UnitTypes.crawler.health = crawlerHealthBase;
        money.clear();
        units.clear();
        unitIDs.clear();
        Groups.player.each(p -> {
            money.put(p.uuid(), 0);
            units.put(p.uuid(), UnitTypes.dagger);
            Timer.schedule(() -> respawnPlayer(p), 1f);
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length == 2 && !Strings.canParsePositiveInt(args[1])) {
                Bundle.bundled(player, "exceptions.invalid-amount");
                return;
            }

            int amount = args.length == 2 ? Strings.parseInt(args[1]) : 1;
            UnitType newUnitType = Seq.with(unitCosts.keys()).find(u -> u.name.equalsIgnoreCase(args[0]));

            if (amount < 1) {
                Bundle.bundled(player, "exceptions.invalid-amount");
                return;
            }

            if (newUnitType == null) {
                Bundle.bundled(player, "commands.upgrade.unit-not-found");
                return;
            }

            if (Groups.unit.count(u -> u.type == newUnitType && u.team == state.rules.defaultTeam) > unitCap - amount) {
                Bundle.bundled(player, "commands.upgrade.too-many-units");
                return;
            }

            if (money.get(player.uuid()) >= unitCosts.get(newUnitType) * amount) {
                if (!player.dead() && player.unit().type == newUnitType || amount > 1) {
                    for (int i = 0; i < amount; i++) {
                        Unit newUnit = newUnitType.spawn(player.x + Mathf.random(), player.y + Mathf.random());
                        setUnit(newUnit);
                    }
                    money.put(player.uuid(), money.get(player.uuid()) - unitCosts.get(newUnitType) * amount);
                    Bundle.bundled(player, "commands.upgrade.already");
                    return;
                }

                Unit newUnit = newUnitType.spawn(player.x, player.y);
                setUnit(newUnit, true);
                player.unit(newUnit);
                money.put(player.uuid(), money.get(player.uuid()) - unitCosts.get(newUnitType));
                units.put(player.uuid(), newUnitType);
                unitIDs.put(player.uuid(), newUnit.id);
                Bundle.bundled(player, "commands.upgrade.success");

            } else Bundle.bundled(player, "commands.upgrade.not-enough-money");
        });

        handler.<Player>register("information", "Show info about the Crawler Arena gamemode.", (args, player) -> Bundle.bundled(player, "commands.information"));

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            StringBuilder upgrades = new StringBuilder(Bundle.format("commands.upgrades.header", Bundle.findLocale(player)));
            IntSeq sortedUnitCosts = unitCosts.values().toArray();
            sortedUnitCosts.sort();
            ObjectIntMap<UnitType> unitCostsCopy = new ObjectIntMap<>();
            unitCostsCopy.putAll(unitCosts);
            sortedUnitCosts.each(cost -> {
                UnitType type = unitCostsCopy.findKey(cost);
                upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(cost).append(")\n");
                unitCostsCopy.remove(type);
            });
            player.sendMessage(upgrades.toString());
        });
    }

    public void registerServerCommands(CommandHandler handler) {
        handler.register("kill", "Kill all enemies in the current wave.", args -> Groups.unit.each(u -> u.team == state.rules.waveTeam, Unitc::kill));
    }
}
