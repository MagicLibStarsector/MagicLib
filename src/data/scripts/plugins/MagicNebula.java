package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

// test to make 3d nebula
/**
 * @deprecated Please replace `data.scripts` with `org.magiclib`.
 */
public class MagicNebula extends BaseEveryFrameCombatPlugin  {
    
    @Override
    public void advance(float amount, List<InputEventAPI> events) { 
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.getTotalElapsedTime(false)==0){
            for(float i = -engine.getMapWidth()/100; i<engine.getMapWidth()/100; i++){
                for(float o = -engine.getMapHeight()/100; o<engine.getMapHeight()/100; o++){
//                    engine.addHitParticle(new Vector2f(i*50,o*50), new Vector2f(), 5, 5, Color.CYAN);
                    if(
                            Math.random()>0.5 && 
                            engine.getNebula().locationHasNebula(i*50, o*50)){
                        engine.addNebulaParticle(
                                new Vector2f(i*50,o*50), 
                                new Vector2f(),
                                MathUtils.getRandomNumberInRange(50, 400), 1,
                                0, 1, 9999999,
                                new Color(
                                        MathUtils.getRandomNumberInRange(5, 10),
                                        MathUtils.getRandomNumberInRange(5, 10),
                                        MathUtils.getRandomNumberInRange(10, 15),
                                        MathUtils.getRandomNumberInRange(150, 200)
                                )
                        );
                    }
                }
            }  
        }
    }
}
