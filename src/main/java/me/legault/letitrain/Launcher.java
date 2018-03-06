package me.legault.letitrain;

/**
 * @title GrenadeLaucher
 * @author Bathlamos, Maty241
 * @version 1.0
 * 
 * CraftBukkit 1.0.1+
 */

import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Launcher implements CommandExecutor{

	public Configuration config;

	public Launcher(){
		this.config = LetItRain.config;
	}
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label,  String[] args){
		
		Player player = null;
		if (!(sender instanceof Player))
			Resources.privateMsg(sender, "Only a player can execute this command");
		else{
			//Permissions
			if (!sender.hasPermission("LetItRain.launcher"))
				return true;
			
			
			player = (Player)sender;
			PlayerInventory inventory = player.getInventory();
			
			inventory.addItem(new ItemStack(config.launcherMaterial));
			
			String outputMsg = config.dGrenadeMsg;
			outputMsg = outputMsg.replaceAll(Pattern.quote("[player]"), player.getName());
			if(!outputMsg.isEmpty())
				Resources.broadcast(outputMsg);
		}
		
		return true;
		
	}

}
