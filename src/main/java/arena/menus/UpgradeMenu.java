package arena.menus;

import arc.util.Structs;
import arena.PlayerData;
import mindustry.core.UI;
import mindustry.gen.Player;
import mindustry.type.UnitType;
import useful.*;
import useful.State.StateKey;
import useful.menu.Menu;
import useful.menu.Menu.MenuView;
import useful.menu.Menu.MenuView.OptionData;

import static mindustry.Vars.*;

public class UpgradeMenu {

    public static final Menu
            unitMenu = new Menu(),
            amountMenu = new Menu();

    public static final StateKey<PlayerData> DATA = new StateKey<>("data");
    public static final StateKey<UnitCost> UNIT = new StateKey<>("unit");

    public static void load() {
        unitMenu.transform(DATA, (menu, data) -> {
            menu.title("upgrade.unit.title");
            menu.content("ui.money", data.money);

            menu.options(3, UnitCost.values()).row();
            menu.option("ui.close");
        });

        amountMenu.transform(DATA, (menu, data) -> {
            menu.title("upgrade.amount.title");
            menu.content("ui.money", data.money);

            menu.options(3, UnitAmount.values()).row();

            menu.option("ui.back", Action.back());
            menu.option("ui.close");
        });
    }

    public static void show(Player player, PlayerData data) {
        unitMenu.show(player, DATA, data);
    }

    public enum UnitCost implements OptionData {
        dagger(25),
        flare(75),
        nova(100),
        mace(200),
        stell(200),
        merui(225),
        elude(250),
        atrax(250),
        horizon(250),
        pulsar(300),
        retusa(400),
        locus(400),
        avert(450),
        cleroi(500),
        fortress(500),
        minke(750),
        oxynoe(850),
        risso(1000),
        mega(1500),
        spiroct(1500),
        quasar(2000),
        precept(2250),
        zenith(2500),
        anthicus(2500),
        obviate(2750),
        cyerce(5000),
        bryde(5000),
        crawler(7500),
        scepter(10000),
        vela(15000),
        vanquish(17500),
        antumbra(18000),
        quell(23500),
        quad(25000),
        arkyid(25000),
        tecta(25000),
        aegires(30000),
        sei(75000),
        poly(100000),
        eclipse(175000),
        disrupt(225000),
        conquer(250000),
        corvus(250000),
        reign(250000),
        oct(250000),
        collaris(300000),
        toxopid(325000),
        navanax(350000),
        omura(1500000),
        mono(3500000);

        public final UnitType type;
        public final long cost;

        UnitCost(long cost) {
            this.type = content.unit(name());
            this.cost = cost;
        }

        public static UnitCost find(String input) {
            return Structs.find(values(), unit -> unit.name().equalsIgnoreCase(input));
        }

        @Override
        public void option(MenuView menu) {
            menu.option("upgrade.unit.button", Action.openWith(amountMenu, UNIT, this), type.name, type.emoji(), UI.formatAmount(cost));
        }
    }

    public enum UnitAmount implements OptionData {
        one(1),
        two(2),
        five(5),
        ten(10),
        fifteen(15),
        twenty(20);

        public final int amount;

        UnitAmount(int amount) {
            this.amount = amount;
        }

        public boolean tooManyUnits(MenuView menu) {
            return state.rules.defaultTeam.data().countType(menu.state.get(UNIT).type) > state.rules.unitCap - amount;
        }

        public boolean notEnoughMoney(MenuView menu) {
            return menu.state.get(DATA).money < menu.state.get(UNIT).cost * amount;
        }

        @Override
        public void option(MenuView menu) {
            menu.option("upgrade.amount.button", view -> {
                var data = view.state.get(DATA);
                var unit = view.state.get(UNIT);

                if (tooManyUnits(view)) {
                    Bundle.announce(view.player, "upgrade.too-many-units", state.rules.defaultTeam.data().countType(unit.type));
                    return;
                }

                if (notEnoughMoney(view)) {
                    Bundle.announce(view.player, "upgrade.not-enough-money", UI.formatAmount(data.money), UI.formatAmount(unit.cost * amount));
                    return;
                }

                for (int i = 0; i < amount; i++)
                    data.applyUnit(unit.type.spawn(player.x, player.y));

                data.money -= unit.cost * amount;
                Bundle.announce(view.player, "upgrade.success", amount, unit.type.emoji(), unit.type.name);
            }, tooManyUnits(menu) || notEnoughMoney(menu) ? "scarlet" : "lime", amount, menu.state.get(UNIT).type.emoji(), UI.formatAmount(menu.state.get(UNIT).cost * amount));
        }
    }
}