package org.magiclib.graphics;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.WarpingSpriteRendererUtil.MutatingValue;

/**
 * Warping sprite utility for tiled sprites.
 * <p>
 * For rendering sprites that wiggle a bit like hyperspace clouds.
 * Based on WarpingSpriteRendererUtil, but adjusted to allow for tiled sprites.
 * Example use:
 * <pre>
 *     MagicTiledSpriteSampler mySampler = new MagicTiledSpriteSampler("hyperspace", "deep_hyperspace", 4, 4);
 *     SpriteAPI mySprite = mySampler.getSprite();
 *     WarpingTiledSpriteRendererUtil myRenderer = new WarpingTiledSpriteRendererUtil(
 *         mySprite, 4, 4, mySprite.getWidth() * 0.1, mySprite.getWidth() * 0.2, 1
 *     );
 * </pre>
 *
 * @author Alex originally.
 * @author Toaster minor modifications.
 */
@SuppressWarnings("unused")
public class WarpingTiledSpriteRendererUtil {
    /**
     * Shivering sprite vertex
     * <p>
     * The bits that wiggle around.
     */
    public static class WarpingVertex {
        public MutatingValue theta;
        public MutatingValue radius;

        public WarpingVertex() {
            theta = new MutatingValue(
                    -360f * ((float) Math.random() * 30f + 1f),
                    360f * ((float) Math.random() * 30f + 1f),
                    30f + 70f * (float) Math.random()
            );
            radius = new MutatingValue(
                    0,
                    10f + 15f * (float) Math.random(),
                    3f + 7f * (float) Math.random()
            );
        }

        public void advance(float amount) {
            theta.advance(amount);
            radius.advance(amount);
        }

        Object writeReplace() {
            theta.setMax((int) theta.getMax());
            theta.setMin((int) theta.getMin());
            theta.setRate((int) theta.getRate());
            theta.setValue((int) theta.getValue());

            radius.setMax((int) radius.getMax());
            radius.setMin((int) radius.getMin());
            radius.setRate((int) radius.getRate());
            radius.setValue((int) radius.getValue());
            return this;
        }
    }

    protected int verticesWide, verticesTall;
    protected WarpingVertex[][] vertices;
    protected SpriteAPI sprite;
    protected boolean mirror = false;

    /**
     * Constructor for the warping sprite.
     *
     * @param sprite        The sprite to render.
     * @param verticesWide  How many moving points across the sprite.
     * @param verticesTall  How many moving points up the sprite.
     * @param minWarpRadius The minimum radius each point moves through.
     * @param maxWarpRadius The maximum radius each point moves through.
     * @param warpRateMult  The rate at which those points move.
     */
    public WarpingTiledSpriteRendererUtil(
            SpriteAPI sprite, int verticesWide, int verticesTall,
            float minWarpRadius, float maxWarpRadius, float warpRateMult
    ) {
        this.sprite = sprite;
        this.verticesWide = verticesWide;
        this.verticesTall = verticesTall;

        vertices = new WarpingVertex[verticesWide][verticesTall];
        for (int i = 0; i < verticesWide; i++) {
            for (int j = 0; j < verticesTall; j++) {
                vertices[i][j] = new WarpingVertex();

                vertices[i][j].radius.set(minWarpRadius, maxWarpRadius);
                vertices[i][j].radius.rate *= warpRateMult;
                vertices[i][j].theta.rate *= warpRateMult;
            }
        }
    }

    /**
     * Only works once, if the original mult was 1f - original rate values are not retained.
     *
     * @param mult
     */
    public void setWarpRateMult(float mult) {
        for (int i = 0; i < verticesWide; i++) {
            for (int j = 0; j < verticesTall; j++) {
                vertices[i][j].radius.rate *= mult;
                vertices[i][j].theta.rate *= mult;
            }
        }
    }

    public void advance(float amount) {
        for (int i = 0; i < verticesWide; i++) {
            for (int j = 0; j < verticesTall; j++) {
                vertices[i][j].advance(amount);
            }
        }
    }

    public void renderAtCenter(float x, float y) {
        float w = sprite.getWidth();
        float h = sprite.getHeight();

        x -= w / 2f;
        y -= h / 2f;

        sprite.bindTexture();
        GL11.glPushMatrix();

        Color color = sprite.getColor();
        GL11.glColor4ub(
                (byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) (color.getAlpha() * sprite.getAlphaMult())
        );

        // translate to the right location and prepare to draw
        GL11.glTranslatef(x, y, 0);

        float centerX = sprite.getCenterX();
        float centerY = sprite.getCenterY();
        float angle = sprite.getAngle();
        // translate to center, rotate, translate back
        if (centerX != -1 && centerY != -1) {
            GL11.glTranslatef(w / 2, h / 2, 0);
            GL11.glRotatef(angle, 0, 0, 1);
            GL11.glTranslatef(-centerX, -centerY, 0);
        } else {
            GL11.glTranslatef(w / 2, h / 2, 0);
            GL11.glRotatef(angle, 0, 0, 1);
            GL11.glTranslatef(-w / 2, -h / 2, 0);
        }

        int blendSrc = sprite.getBlendSrc();
        int blendDest = sprite.getBlendDest();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(blendSrc, blendDest);

        float tex_width = sprite.getTexWidth() - 0.001f;
        float tex_height = sprite.getTexWidth() - 0.001f;

        float tex_x = sprite.getTexX();
        float tex_y = sprite.getTexY();

        float cw = w / (float) (verticesWide - 1);
        float ch = h / (float) (verticesTall - 1);
        float ctw = tex_width / (float) (verticesWide - 1);
        float cth = tex_height / (float) (verticesTall - 1);

        for (float i = 0; i < verticesWide - 1; i++) {
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            {
                for (float j = 0; j < verticesTall; j++) {
                    float x1 = cw * i;
                    float y1 = ch * j;
                    float x2 = cw * (i + 1f);
                    float y2 = ch * j;

                    float tx1 = tex_x + ctw * i;
                    float ty1 = tex_y + cth * j;
                    float tx2 = tex_x + ctw * (i + 1f);
                    float ty2 = tex_y + cth * j;

                    if (mirror) {
                        tx1 = tex_width - tx1;
                        tx2 = tex_height - tx2;
                    }

                    float theta1 = (float) Math.toRadians(vertices[(int) i][(int) j].theta.getValue());
                    float radius1 = vertices[(int) i][(int) j].radius.getValue();
                    float sin1 = (float) Math.sin(theta1);
                    float cos1 = (float) Math.cos(theta1);

                    x1 += cos1 * radius1;
                    y1 += sin1 * radius1;

                    float theta2 = (float) Math.toRadians(vertices[(int) i + 1][(int) j].theta.getValue());
                    float radius2 = vertices[(int) i + 1][(int) j].radius.getValue();
                    float sin2 = (float) Math.sin(theta2);
                    float cos2 = (float) Math.cos(theta2);

                    x2 += cos2 * radius2;
                    y2 += sin2 * radius2;

                    GL11.glTexCoord2f(tx1, ty1);
                    GL11.glVertex2f(x1, y1);

                    GL11.glTexCoord2f(tx2, ty2);
                    GL11.glVertex2f(x2, y2);
                }
            }
            GL11.glEnd();

        }

        GL11.glPopMatrix();
    }

    public int getVerticesWide() {
        return this.verticesWide;
    }

    public int getVerticesTall() {
        return this.verticesTall;
    }

    public SpriteAPI getSprite() {
        return this.sprite;
    }

    public boolean isMirror() {
        return this.mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

}















