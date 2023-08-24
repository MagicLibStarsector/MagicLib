package org.magiclib.achievements;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

class MagicAchievementIntelParticleEffect implements CustomUIPanelPlugin {
    PositionAPI position;
    MagicAchievement achievement;
    float widthHeight;
    MagicAchievementParticleScript particleScript = new MagicAchievementParticleScript();
    Vector2f anchor = new Vector2f(0, 0);
    boolean initialRun = true;

    public MagicAchievementIntelParticleEffect(PositionAPI position, float widthHeight, MagicAchievement achievement) {
        this.position = position;
        this.achievement = achievement;
        this.widthHeight = widthHeight;

        // Start with effects showing, rather than none and having them appear over time.
        for (int i = 0; i < 40; i++) {
            render(1f);
        }

        initialRun = false;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {

    }

    @Override
    public void render(float alphaMult) {
        int edgePadding = 5; // shrink box a little to be closer to center
        Vector2f renderCenter = new Vector2f(
                (widthHeight / 8) + ((float) edgePadding / 2),
                (widthHeight / 2) + ((float) edgePadding / 2));

        particleScript.render(
                new Rectangle((int) widthHeight - edgePadding, (int) widthHeight - edgePadding),
                anchor.translate(renderCenter.getX(), renderCenter.getY()),
                achievement,
                initialRun);
    }

    @Override
    public void advance(float amount) {
        anchor.set(position.getX(), position.getY());
        particleScript.advance(amount);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }
}
