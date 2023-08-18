package org.magiclib.achievements;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class FancyEffects implements CustomUIPanelPlugin {
    PositionAPI position;
    MagicAchievement achievement;
    float widthHeight;
    MagicAchievementParticleScript particleScript = new MagicAchievementParticleScript();

    public FancyEffects(PositionAPI position, float widthHeight, MagicAchievement achievement) {
        this.position = position;
        this.achievement = achievement;
        this.widthHeight = widthHeight;

        // Start with effects showing, rather than none and having them appear over time.
        for (int i = 0; i < 50; i++) {
            Vector2f renderCenter = new Vector2f(position.getX() + (widthHeight / 8), position.getY() + (widthHeight / 2));

            particleScript.render(
                    new Rectangle((int) renderCenter.getX(), (int) renderCenter.getY(), (int) widthHeight, (int) widthHeight),
                    achievement,
                    true);
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
        Vector2f renderCenter = new Vector2f(position.getX() + (widthHeight / 8), position.getY() + (widthHeight / 2));

        particleScript.render(new Rectangle((int) renderCenter.getX(), (int) renderCenter.getY(), (int) widthHeight, (int) widthHeight), achievement);
    }

    @Override
    public void advance(float amount) {
        particleScript.advance(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
