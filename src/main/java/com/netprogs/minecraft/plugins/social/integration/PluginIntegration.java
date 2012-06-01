package com.netprogs.minecraft.plugins.social.integration;

import org.bukkit.plugin.Plugin;

/*
 * Copyright (C) 2012 Scott Milne
 * 
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

public abstract class PluginIntegration {

    public abstract void initialize(Plugin plugin);

    public boolean isEnabled() {

        // If the plug-in is dependent, we should add it to the "Depend" list in plugin.yml
        // That will cause the loader to throw an exception if they do not have it installed.

        // Check to see if it was initialized, then check to see if it was enabled.
        if (isPluginLoaded()) {
            return isPluginEnabled();
        }

        // the component was not installed
        return false;
    }

    protected abstract boolean isPluginEnabled();

    protected abstract boolean isPluginLoaded();
}
