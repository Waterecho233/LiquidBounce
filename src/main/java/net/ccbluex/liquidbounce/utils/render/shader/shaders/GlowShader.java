/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders;

import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader;

import static org.lwjgl.opengl.GL20.*;

public final class GlowShader extends FramebufferShader {

    public static final GlowShader GLOW_SHADER = new GlowShader();

    public GlowShader() {
        super("glow.frag");
    }

    @Override
    public void setupUniforms() {
        setupUniform("texture");
        setupUniform("texelSize");
        setupUniform("color");
        setupUniform("fade");
        setupUniform("radius");
        setupUniform("targetAlpha");
    }

    @Override
    public void updateUniforms() {
        glUniform1i(getUniform("texture"), 0);
        glUniform2f(getUniform("texelSize"), 1F / mc.displayWidth * renderScale, 1F / mc.displayHeight * renderScale);
        glUniform3f(getUniform("color"), red, green, blue);
        glUniform1f(getUniform("fade"), fade);
        glUniform1i(getUniform("radius"), radius);
        glUniform1f(getUniform("targetAlpha"), targetAlpha);
    }
}
