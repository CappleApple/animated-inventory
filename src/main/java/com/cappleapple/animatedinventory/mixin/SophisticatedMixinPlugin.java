package com.cappleapple.animatedinventory.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SophisticatedMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String mixinPackage) { }
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            org.spongepowered.asm.service.MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            return true;
        } catch (ClassNotFoundException | java.io.IOException unavailable) { return false; }
    }
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    public List<String> getMixins() { return null; }
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
