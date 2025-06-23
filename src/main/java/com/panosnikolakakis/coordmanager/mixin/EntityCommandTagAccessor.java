package com.panosnikolakakis.coordmanager.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(Entity.class)
public interface EntityCommandTagAccessor {
	@Invoker("getCommandTags")
	Set<String> coordmanager$getCommandTags();
}