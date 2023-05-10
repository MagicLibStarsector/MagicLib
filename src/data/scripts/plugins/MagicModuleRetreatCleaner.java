package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.combat.CombatFleetManager.O0;

import java.util.List;

// Works around battle not ending if ship with modules retreats
@Deprecated
public class MagicModuleRetreatCleaner extends BaseEveryFrameCombatPlugin {

    public static final String CUSTOM_DATA_KEY = "shared_module_retreat_cleaner_plugin";

    protected IntervalUtil interval = new IntervalUtil(0.4f, 0.6f);
    protected CombatEngineAPI engine;
    protected boolean running = true;    // turn off if another such plugin is running

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!running) return;
        interval.advance(amount);
        if (interval.intervalElapsed()) {
            validateDeployedShips(FleetSide.PLAYER);
            validateDeployedShips(FleetSide.ENEMY);
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        if (engine.getCustomData().containsKey(CUSTOM_DATA_KEY)) {
            Global.getLogger(this.getClass()).info("Another module retreat cleaner already running, suspending plugin");
            running = false;
        } else {
            engine.getCustomData().put(CUSTOM_DATA_KEY, this);
        }
    }

    public void validateDeployedShips(FleetSide side) {
        if (engine == null) return;

        CombatFleetManager manager = (CombatFleetManager) engine.getFleetManager(side);
        boolean anyNonModule = false;

        if (manager.getDeployed().isEmpty())
            return;

        for (O0 deployed : manager.getDeployed()) {
            if (!deployed.isStationModule()) {
                anyNonModule = true;
                break;
            }
        }
        if (!anyNonModule) {
            Global.getLogger(this.getClass()).info("Clearing orphaned modules from "
                    + "deployed list for side " + side.toString() + ": " + manager.getDeployed().size());
            manager.getDeployed().clear();
        }
    }

    /* Debugging methods */

//    // runcode data.scripts.everyframe.SWP_ModuleRetreatCleaner.queryBattleState()
//    public static void queryBattleState() {
//        CombatEngine engine = (CombatEngine)Global.getCombatEngine();
//        
//        Console.showMessage("Player fleet deployed ships:");
//        CombatFleetManager man = engine.getPlayerFleetManager();
//        printShips(man.getDeployed());
//        Console.showMessage("Enemy fleet deployed ships:");
//        man = engine.getEnemyFleetManager();
//        printShips(man.getDeployed());
//    }
//    
//    public static void printShips(Set<O0> ships) {
//        for (O0 ship : ships) {
//            String name = ship.getMember().getShipName();
//            String name2 = ship.getShip().getName();
//            String hullId = ship.getMember().getHullId();
//            
//            Console.showMessage("  Ship " + name + " (hull " + hullId + "): " + ship.isStationModule());
//        }
//    }
//    
//    // runcode data.scripts.everyframe.SWP_ModuleRetreatCleaner.purge()
//    public static void purge() {
//        CombatEngine engine = (CombatEngine)Global.getCombatEngine();
//        
//        //engine.getPlayerFleetManager().getDeployed().clear();
//        engine.getEnemyFleetManager().getDeployed().clear();
//    }
}
