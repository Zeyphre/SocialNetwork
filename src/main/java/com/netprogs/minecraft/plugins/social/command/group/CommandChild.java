package com.netprogs.minecraft.plugins.social.command.group;

import java.util.logging.Logger;

import com.netprogs.minecraft.plugins.social.SocialPerson;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.command.exception.ArgumentsMissingException;
import com.netprogs.minecraft.plugins.social.command.exception.PlayerNotInNetworkException;
import com.netprogs.minecraft.plugins.social.command.help.HelpMessage;
import com.netprogs.minecraft.plugins.social.command.help.HelpSegment;
import com.netprogs.minecraft.plugins.social.command.util.MessageUtil;
import com.netprogs.minecraft.plugins.social.config.PluginConfig;
import com.netprogs.minecraft.plugins.social.config.resources.ResourcesConfig;
import com.netprogs.minecraft.plugins.social.config.settings.group.ChildSettings;
import com.netprogs.minecraft.plugins.social.integration.VaultIntegration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/*
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
 * 
 * Copyright (C) 2012 Scott Milne
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

public class CommandChild extends GroupCommand<ChildSettings> {

    private final Logger logger = Logger.getLogger("Minecraft");

    public CommandChild() {
        super(SocialNetworkCommandType.child);
    }

    /**
     * Checks to see if playerPerson is allowed to send a request to receiverPerson.
     * Called from the sender of the request to see if they are allowed to send a request for this group.
     * @param playerPerson The person sending the request.
     * @param receiverPerson The person who is receiving the request.
     * @return
     */
    @Override
    protected boolean allowSendRequest(SocialPerson playerPerson, SocialPerson receiverPerson) {

        // A person can only be a child of one other person.

        // Check to see if the sender can afford the cost.
        boolean success = checkCommandCost(playerPerson);
        if (!success) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they've reached their maximum limit
        ChildSettings settings = getCommandSettings();
        if (settings.getMaximumChildren() > 0 && playerPerson.getNumberChildren() >= settings.getMaximumChildren()) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.maximum.sender",
                    ChatColor.RED);
            return false;
        }

        // We want to make sure the receiverPerson isn't already a child of someone
        if (receiverPerson.getChildOf() != null) {
            MessageUtil.sendMessage(playerPerson, "social." + getCommandType() + ".cannotSendRequest.sender",
                    ChatColor.RED);
            return false;
        }

        return true;
    }

    /**
     * Checks to see if acceptPerson is still able to accept a request from the senderPerson.
     * Called when the request is received by the other person to determine if they are allowed to accept a request.
     * @param acceptPerson The person who is trying to accept the request.
     * @param senderPerson The person who sent them the request.
     * @return
     */
    @Override
    protected boolean allowAcceptRequest(SocialPerson acceptPerson, SocialPerson senderPerson) {

        // Check to see if the player can afford the cost.
        boolean success = checkCommandCost(acceptPerson);
        if (!success) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.price.sender",
                    ChatColor.GOLD);
            return false;
        }

        // check to see if they've reached their maximum limit
        ChildSettings settings = getCommandSettings();
        if (settings.getMaximumChildren() > 0 && acceptPerson.getNumberChildren() >= settings.getMaximumChildren()) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.maximum.sender",
                    ChatColor.RED);
            return false;
        }

        // We want to make sure the acceptPerson isn't already a child of someone
        if (acceptPerson.getChildOf() != null) {
            MessageUtil.sendMessage(acceptPerson, "social." + getCommandType() + ".cannotAcceptRequest.sender",
                    ChatColor.RED);
            return false;
        }

        return true;
    }

    /**
     * Checks to see if inGroupPerson is within playerPerson's group already.
     * Called from the sender of the request to make sure the receiver isn't already in the group.
     * @param playerPerson
     * @param inGroupPerson
     * @return
     */
    @Override
    protected boolean personInGroup(SocialPerson playerPerson, SocialPerson inGroupPerson) {

        // check to see if they are already a child of yours
        return playerPerson.isParentOf(inGroupPerson.getName());
    }

    /**
     * Adds the addPerson to the playerPerson's group.
     * @param playerPerson
     * @param addPerson
     */
    @Override
    protected void addPersonToGroup(SocialPerson playerPerson, SocialPerson addPerson) {

        // add the child
        playerPerson.addChild(addPerson.getName());

        // add the parent to the childOf
        addPerson.createChildOf(playerPerson.getName());

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);

        // charge the user
        Player player = Bukkit.getServer().getPlayer(playerPerson.getName());
        if (player != null) {
            VaultIntegration.getInstance().processCommandPurchase(player, getCommandSettings().getPerUseCost());
        }
    }

    /**
     * Removes the removePerson from the playerPerson's group.
     * @param playerPerson
     * @param removePerson
     */
    @Override
    protected void removePersonFromGroup(SocialPerson playerPerson, SocialPerson removePerson) {

        // remove the child (removePerson) from the parents (playerPerson) list
        playerPerson.removeChild(removePerson.getName());

        // clear the childOf value for removePerson making them have no parent
        removePerson.breakChildOf();

        // check for a permissions update
        checkForPermissionsUpdate(playerPerson);
    }

    /**
     * Handles the actual accept.
     * @param receiverPerson The person who is accepting the request.
     * @param senderPerson The person who sent the request.
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    @Override
    protected void handleAccept(SocialPerson receiverPerson, SocialPerson senderPerson)
            throws ArgumentsMissingException, PlayerNotInNetworkException {

        // add the child (receiverPerson) to the parent (senderPerson)
        addPersonToGroup(senderPerson, receiverPerson);
    }

    /**
     * Handles the actual remove. This is called by the parent in this case to remove a child.
     * @param playerPerson The person who requested the removal.
     * @param removePerson The person who is going to be removed.
     * @throws ArgumentsMissingException
     * @throws PlayerNotInNetworkException
     */
    @Override
    protected void handleRemove(SocialPerson playerPerson, SocialPerson removePerson) throws ArgumentsMissingException,
            PlayerNotInNetworkException {

        // if the playerPerson is a child, remove them from their parents list and reset their childOf setting
        if (playerPerson.getChildOf() != null && playerPerson.getChildOf().equalsIgnoreCase(removePerson.getName())) {

            // remove the child from the parents list
            removePersonFromGroup(removePerson, playerPerson);

            // clear the childOf
            playerPerson.breakChildOf();

        } else if (playerPerson.getChildOf() == null) {

            // if the playerPerson is the parent (childOf null), then remove the child from their list and reset childOf

            // remove the child from the parents list
            removePersonFromGroup(playerPerson, removePerson);

            // clear the childOf
            removePerson.breakChildOf();
        }
    }

    /**
     * Handles the listing of a persons group.
     * @param player The bukkit player instance.
     * @param playerPerson The person who sent the request.
     */
    @Override
    protected void handleList(Player player, SocialPerson playerPerson) {

        // send back our member list
        displayGroupList(player, playerPerson.getChildren());
    }

    @Override
    public HelpSegment help() {

        HelpSegment helpSegment = new HelpSegment(getCommandType());

        ResourcesConfig config = PluginConfig.getInstance().getConfig(ResourcesConfig.class);

        HelpMessage mainCommand = new HelpMessage();
        mainCommand.setCommand(getCommandType().toString());
        mainCommand.setArguments("request <player>");
        mainCommand.setDescription(config.getResource("social.child.help.request"));
        helpSegment.addEntry(mainCommand);

        HelpMessage acceptCommand = new HelpMessage();
        acceptCommand.setCommand(getCommandType().toString());
        acceptCommand.setArguments("accept <player>");
        acceptCommand.setDescription(config.getResource("social.child.help.accept"));
        helpSegment.addEntry(acceptCommand);

        HelpMessage rejectCommand = new HelpMessage();
        rejectCommand.setCommand(getCommandType().toString());
        rejectCommand.setArguments("reject <player>");
        rejectCommand.setDescription(config.getResource("social.child.help.reject"));
        helpSegment.addEntry(rejectCommand);

        HelpMessage ignoreCommand = new HelpMessage();
        ignoreCommand.setCommand(getCommandType().toString());
        ignoreCommand.setArguments("ignore <player>");
        ignoreCommand.setDescription(config.getResource("social.child.help.ignore"));
        helpSegment.addEntry(ignoreCommand);

        HelpMessage removeCommand = new HelpMessage();
        removeCommand.setCommand(getCommandType().toString());
        removeCommand.setArguments("remove <player>");
        removeCommand.setDescription(config.getResource("social.child.help.remove"));
        helpSegment.addEntry(removeCommand);

        HelpMessage listCommand = new HelpMessage();
        listCommand.setCommand(getCommandType().toString());
        listCommand.setArguments("list");
        listCommand.setDescription(config.getResource("social.child.help.list"));
        helpSegment.addEntry(listCommand);

        return helpSegment;
    }
}
