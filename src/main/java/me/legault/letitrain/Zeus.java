package me.legault.letitrain;

import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Zeus implements CommandExecutor{
	
	public Zeus(LetItRain plugin){}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label,  String[] args){
		
		Player player = null;
		if (!(sender instanceof Player))
			Resources.privateMsg(sender, "Only a player can execute this command");
		else{
			player = (Player) sender;
			//Permissions
			if (!sender.hasPermission("LetItRain.zeus"))
				return true;
			if(args.length > 0){
				switch (args[0]){
					case "set":
						LetItRain.config.zeusMaterial = player.getEquipment().getItemInMainHand().getType();
				}
			}
			PlayerInventory inv = player.getInventory();
			ItemStack item = new ItemStack(LetItRain.config.zeusMaterial);
			if (!inv.contains(LetItRain.config.zeusMaterial) && inv.firstEmpty() != -1){
				inv.addItem(item);
				String outputMsg = LetItRain.config.dZeusMsg;
				outputMsg = outputMsg.replaceAll(Pattern.quote("[player]"), player.getName());
				if(!outputMsg.isEmpty()){
					Resources.broadcast(outputMsg);
				}
			}
		}
		return true;
	}
}
