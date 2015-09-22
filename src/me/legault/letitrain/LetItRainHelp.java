package me.legault.letitrain;

import java.util.ArrayList;
import java.util.Collections;

import net.md_5.bungee.api.ChatColor;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import au.com.addstar.monolith.ParticleEffect;

public class LetItRainHelp implements CommandExecutor{
	
	public LetItRainHelp(LetItRain plugin){}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label,  String[] args) {
		if ((args.length > 0) && (args[0].equals("effects"))) {
			sender.sendMessage(ChatColor.GREEN + "Possible particle effect names:");
			ArrayList<String> effects = new ArrayList<String>();
			for (ParticleEffect e : ParticleEffect.values()) {
				effects.add(e.toString());
			}
			Collections.sort(effects);
			sender.sendMessage(ChatColor.AQUA + StringUtils.join(effects, ", "));
		} else {
			Resources.privateMsg(sender, "________________ Let It Rain " + Resources.getPluginVersion() + " ________________");
			Resources.privateMsg(sender, "/rain <creature> <amount/duration> <radius> <player/coordinate name>");
			Resources.privateMsg(sender, "/firerain <creature> <amount/duration> <radius> <player/coordinate name>");
			Resources.privateMsg(sender, "/effectrain <creature> <amount/duration> <radius> <player/coordinate name> [effect]");
			Resources.privateMsg(sender, "/letitrain effects");
			Resources.privateMsg(sender, "/rain add <coordinate name>");
			Resources.privateMsg(sender, "/rain remove <coordinate name>");
			Resources.privateMsg(sender, "/zeus");
			Resources.privateMsg(sender, "/strike <player>");
			Resources.privateMsg(sender, "/launcher");
			Resources.privateMsg(sender, "/slaughter <radius>");
			Resources.privateMsg(sender, "/removeItems <radius>");
		}
		return true;
	}

}
