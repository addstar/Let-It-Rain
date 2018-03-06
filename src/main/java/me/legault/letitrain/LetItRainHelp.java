package me.legault.letitrain;

import java.util.ArrayList;
import java.util.Collections;

import net.md_5.bungee.api.ChatColor;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LetItRainHelp implements CommandExecutor{
	
	public LetItRainHelp(LetItRain plugin){}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label,  String[] args) {
		if ((args.length > 0) && (args[0].equals("effects"))) {
			// Command: /letitrain effects
			sender.sendMessage(ChatColor.GREEN + "Possible particle effect names:");
			ArrayList<String> effects = new ArrayList<>();
			for (Particle e : org.bukkit.Particle.values()) {
				effects.add(e.toString());
			}
			Collections.sort(effects);
			sender.sendMessage(ChatColor.AQUA + StringUtils.join(effects, ", "));
			return true;
		}
		if (args.length == 0){
			// Command: /letitrain
			Resources.privateMsg(sender, "________________ " + LetItRain.plugin.getDescription().getFullName() + " ________________");
			Resources.privateMsg(sender, "/rain <entity|item|hand> <amount|duration|amountPerSec/duration> <radius> <player|location name> [effect]");
			Resources.privateMsg(sender, "/firerain - Drop burning entities");
			Resources.privateMsg(sender, "/effectrain <entity|item|hand> <amount|duration|amountPerSec/duration> <radius> <player|location name> [effect]");
			Resources.privateMsg(sender, "/letitrain effects - See valid effect names");
			Resources.privateMsg(sender, "/letitrain reload - Reload the config");
			Resources.privateMsg(sender, "/rain lightning <amount|duration> <radius> <player|location name> - Spark lightning");
			Resources.privateMsg(sender, "/rain lightningexplode <amount|duration> <radius> <player|location name> - Spark lightning that explodes and breaks blocks");
			Resources.privateMsg(sender, "/rain add <location name> - Add a named location");
			Resources.privateMsg(sender, "/rain delete <location name> - Remove a named location");
			Resources.privateMsg(sender, "/zeus - Give nether star; right click for lightning (destructive, breaks blocks)");
			Resources.privateMsg(sender, "/strike <player> - Kill the player");
			Resources.privateMsg(sender, "/launcher");
			Resources.privateMsg(sender, "/slaughter <radius> <x> <y> <z> <world> - Kill mobs around player or at world location");
			Resources.privateMsg(sender, "/removeItems <radius> <x> <y> <z> <world> - Remove items around player or at world location");
			return true;
		}
		
		if (args.length == 1){
			// Command: /letitrain reload
			if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("LetItRain.reload")){
				LetItRain.config.reloadConfig();
				Resources.privateMsg(sender, "LetItRain Reloaded!");
				return true;
			}			
		}					
		return true;
	}

}
