package me.legault.letitrain;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.MaterialDefinition;

public class Rain implements CommandExecutor{
	
	public static HashMap<Entity, Boolean> thrownedItems = new HashMap<Entity, Boolean>();
	public static HashMap<Integer, Integer> runningTasks = new HashMap<Integer, Integer>();
	
	public boolean onCommand(final CommandSender sender, Command cmd, String label,  String[] args){
		boolean isOnFire = false;
		int amount = 0, radius = 0, time = 0;
		String targetName = null;
		Location targetLocation = null;
		EntityType obj = null;
		PotionType potion = null;
		ItemStack item = null;
		
		// Check permissions
		if (!sender.hasPermission("LetItRain.rain"))
			return true;
		
		// Firerain command
		if (label.equalsIgnoreCase("firerain"))
			isOnFire = true;
		
		// Give command usage if no params specified
		if (args == null || args.length < 4) {
			displayHelp(label, sender);
			return true;
		}
		
		// Help
		if (args[0].equalsIgnoreCase("help")){
			displayHelp(label, sender);
			return true;
		}
		
		// Check if the user tries to add/remove a command
		if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete"))
			return addRemoveCoordinates(sender, args);
		
		obj = findEntity(args[0]);

		// Parse potions
		if(LetItRain.rainPotions && (args[0].startsWith("potion:") || args[0].startsWith("potions:"))){
			if(args[0].startsWith("potion:"))
				potion = findPotion(args[0].substring("potion:".length()));
			else
				potion = findPotion(args[0].substring("potions:".length()));
		}

		// Parse other entity names
		if (potion == null && LetItRain.rainBlocks)
			item = getItem(args[0]);

		// Validate entities/potions
		if (obj == null && item == null && potion == null){
			Resources.privateMsg(sender, "Please enter a valid entity/material id or name");
			if (!LetItRain.rainBlocks)
				Resources.privateMsg(sender, "Blocks have been disabled ");
			return true;
		}

		// Parse named target
		for (Coordinate f: LetItRain.coordinates) {
			//Check if the target is a named location from the config
			if (f.hasName(args[1])) {
				targetName = f.name;
				World w = LetItRain.server.getWorld(f.world);
				targetLocation = new Location(w, f.x, f.y, f.z);
				break;
			}
		}

		// Parse player
		if (targetName == null) {
			Player target = Resources.isPlayer(args[1]);
			if (target != null) {
				targetName = target.getDisplayName();
				targetLocation = target.getLocation();
			} else {
				Resources.privateMsg(sender, "Invalid player/place specified");
				return true;
			}
		}

		// Parse amount/time
		String sAmt = null, sTime = null;
		if (args[2].contains("/")) {
			// This is amount+time
			sAmt = args[2].split("/")[0];
			sTime = args[2].split("/")[1];
			if (sTime.endsWith("s")) {
				sTime = sTime.substring(0, sTime.length() - 1);
			}
		}
		else if (args[2].endsWith("s")) {
			// This is a time only (x secs) not an amount
			sTime = args[2].substring(0, args[2].length() - 1);
		} else {
			// Amount only
			sAmt = args[2];
		}
		
		try {
			if (sAmt != null)
				amount = Integer.parseInt(sAmt);
		} catch(NumberFormatException e) {
			amount = -1;
		}

		try {
			if (sTime != null)
				time = Integer.parseInt(sTime);
		} catch(NumberFormatException e) {
			time = -1;
		}

		try {
			radius = Integer.parseInt(args[3]);
		} catch(NumberFormatException e) {
			radius = -1;
		}
		
		//Impossible parameters
		if (time == -1) {
			Resources.privateMsg(sender, "Time parameter of \"" + sTime + "\" is not valid.");
			return true;
		}
		if (amount == -1) {
			Resources.privateMsg(sender, "Amount parameter of \"" + sAmt + "\" is not valid.");
			return true;
		}
		if (radius == -1) {
			Resources.privateMsg(sender, "Radius parameter of \"" + args[3] + "\" is not valid.");
			return true;
		}
		
		//Max radius
		if(radius > LetItRain.maxRadius){
			Resources.privateMsg(sender, "The maximum radius is " + LetItRain.maxRadius);
			return true;
		}
		
		//Returns false if console forgot name as parameter
		if(targetName == null && isNotPlayer(sender))
			return true;
		
		//Defaults
		if (targetName == null){
			targetName = ((Player) sender).getDisplayName();
			targetLocation = ((Player) sender).getLocation();
		}
		
		//Test whether the animal is blacklisted
		if (obj != null){
			try{
				if (LetItRain.config.getBoolean("LetItRain.rain.blacklist." + obj.getEntityClass().getSimpleName())){
					Resources.privateMsg(sender, "The entity you chose has been blacklisted");
					return true;
				}
			}catch(Exception e){
				Resources.privateMsg(sender, "An unknow exception has occured with your config file. Please try again.");
				return true;
			}
		}
		
		if ((item != null) && ((!LetItRain.rainLava && item.getType() == Material.LAVA) || (!LetItRain.rainWater && item.getType() == Material.WATER))) {
			// WATER or LAVA
			Resources.privateMsg(sender, "Do not use water or lava! Bad things happen...");
			/*
			World w = targetLocation.getWorld();
			for(int i = -radius; i < radius; i++){
				double boundary = Math.sqrt(Math.pow(radius, 2) - Math.pow(i, 2));
				for(int j = -(int)boundary; j < boundary; j++) {
					Location l = new Location(targetLocation.getWorld(), targetLocation.getX() + i, targetLocation.getY() + 50, targetLocation.getZ() + j);
					w.getBlockAt(l).setType(item.getType());
				}
			}
			*/
		} else if (time > 0) {
			// TIME based spawning (100 per second for X seconds)
			Random rdm = new Random();
			final long startTime = System.currentTimeMillis();
			final int myTaskIdentifier = rdm.nextInt();
			final PotionType fPotion = potion;
			final Location fLocation = targetLocation;
			final ItemStack fItem = item;
			final int fRadius = radius;
			if (amount == 0) {
				amount = 50;	// Default batch amount
			} else {
				amount = (amount / time);	// Calculate batch amount
			}
			final int fAmount = amount;
			final int fTime = time;
			final EntityType fObj = obj;
			final boolean fIsOnFire = isOnFire;
			
			int id = LetItRain.server.getScheduler().scheduleSyncRepeatingTask(LetItRain.plugin, new Runnable() {
				@Override
				public void run() {
					long timeRemaining = (startTime + (fTime * 1000)) - System.currentTimeMillis();

					boolean result = spawnEntities(fLocation, fObj, sender, fItem, fPotion, fAmount, fRadius, fIsOnFire); 
					if ((!result) || (timeRemaining < 1000)) { 
						StopScheduler(myTaskIdentifier);
					}
				}
				
			}, 0L,  20); // There are 20 server ticks per second
			runningTasks.put(myTaskIdentifier, id);
		}else{
			// ALL other drops
			boolean res = spawnEntities(targetLocation, obj, sender, item, potion, amount, radius, isOnFire);
			if(!res)
				return true;
		}
		
		String name = "";
		
		if (obj != null)
			name = obj.getEntityClass().getSimpleName();
		else if(potion != null)
			name = potion.name() + " potion";
		else
			name = item.getType().name();

		name = name.replaceAll("_", " ").toLowerCase();
		
		if(amount > 1)
			name = toPlural(name);

		displayMsg(targetName, name, isOnFire);
		
		return true;
	}
	
	private static boolean spawnEntities(Location location, EntityType obj, CommandSender sender, ItemStack item, 
			PotionType potionType, int amount, int radius, boolean isOnFire){
		
		Location newLoc;
		Random rdm = new Random();
		
		try{
			//Spawn entity
			for (int i = 0; i < amount; i++){
				newLoc = location.clone();
				newLoc.setX(location.getX()+(double)rdm.nextInt(radius*2)-(double)radius);
				newLoc.setY(location.getY()+(double)rdm.nextInt(250)+100.0);
				newLoc.setZ(location.getZ()+(double)rdm.nextInt(radius*2)-(double)radius);
				
				if (obj != null){
					Entity creature = location.getWorld().spawn(newLoc, obj.getEntityClass());
					thrownedItems.put(creature, isOnFire);
					
					if (creature instanceof Fireball)
						((Fireball)creature).setDirection(new Vector(0, -1, 0));
					if (creature instanceof ExperienceOrb)
						((ExperienceOrb) creature).setExperience(1000 + (int)rdm.nextFloat()*300);
					if (creature instanceof TNTPrimed)
						((TNTPrimed) creature).setFuseTicks(150);
					if (isOnFire)
						creature.setFireTicks(1000 + (int)rdm.nextFloat()*300);
				} else {
					if (potionType != null) {
						item = new Potion(potionType).toItemStack(1);
					}
					location.getWorld().dropItem(newLoc, item);
				}
			}
		}catch(Exception e){
			Resources.privateMsg(sender, "This entity or world is invalid");
			return false;
		}
		return true;
	}
	
	private EntityType findEntity(String token){
		
		token = toSingular(token);
		
		for(EntityType o: EntityType.values()){
			String name = o.getName() == null ? "": o.getName();
			String simpleName = o.getEntityClass() == null || o.getEntityClass().getSimpleName() == null ? "": o.getEntityClass().getSimpleName();
			
			if(toSingular(simpleName).equalsIgnoreCase(token) ||
					toSingular(name).equalsIgnoreCase(token))
					return o;
		}
		return null;
	}
	
	private ItemStack getItem(String search)
	{
		String itemname = search.split(":")[0];
		MaterialDefinition def = null;
		short data = 0;
		
		if(search.contains(":")) {
			String dpart = search.split(":")[1];
			try {
				data = Short.parseShort(dpart);
				if(data < 0)
					throw new IllegalArgumentException("Data value for " + itemname + " cannot be less than 0");
			}
			catch(NumberFormatException e) {
				throw new IllegalArgumentException("Data value after " + itemname);
			}
		}

		def = getMaterial(itemname);
		if (def == null) return null;
		
		def = new MaterialDefinition(def.getMaterial(), data);

		ItemStack item = def.asItemStack(1);
		return item;
	}
	
	@SuppressWarnings( "deprecation" )
    private MaterialDefinition getMaterial(String name)
	{
		// Bukkit name
		Material mat = Material.getMaterial(name.toUpperCase());
		if (mat != null)
			return new MaterialDefinition(mat, (short)0);
		
		// Id
		try
		{
			short id = Short.parseShort(name);
			mat = Material.getMaterial(id);
		}
		catch(NumberFormatException e)
		{
		}
		
		if(mat != null)
			return new MaterialDefinition(mat, (short)0);

		// ItemDB
		return Lookup.findItemByName(name);
	}
	
	private PotionType findPotion(String token){
		
		token = token.replaceAll("[(potion|instant)_ ]", "").toLowerCase();
		
		for(PotionType o: PotionType.values())
			if(o.name().replaceAll("[(POTION|INSTANT)_ ]", "").equalsIgnoreCase(token))
				return o;
		return null;
	}
	
	private static void StopScheduler(int id){
		Integer i = runningTasks.remove(id);
		if(i != null)
			LetItRain.server.getScheduler().cancelTask(i);
	}

	/**
	 * Display help
	 */
	private void displayHelp(String label, CommandSender sender){
		Resources.privateMsg(sender, "/" + label + " <entity> <player|place> <amount/time> <radius>");
	}
	
	/**
	 * Substitute [player] and [animal] and display the msg
	 */
	private void displayMsg(String target, String animal, boolean isOnFire){
		String msg;
		if (isOnFire)
			msg = LetItRain.dFirerainMsg;
		else
			msg = LetItRain.dRainMsg;
		
		msg = msg.replaceAll(Pattern.quote("[entity]"), animal.toLowerCase());
		msg = msg.replaceAll(Pattern.quote("[player]"), target);
		
		if(!msg.isEmpty())
			Resources.broadcast(msg);
	}
	
	/**
	 * Grammar: returns the singular lower case version of a word
	 */
	private static String toSingular(String word){

		word = word.toLowerCase();
		
		if(!word.equals("zombies") || !word.equals("slimes")){

			if(word.matches(".*ives$"))
				return word.substring(0, word.length() - 3) + "fe";
			if(word.matches(".*ves$"))
				return word.substring(0, word.length() - 3) + "f";
			if(word.matches(".*men$"))
				return word.substring(0, word.length() - 3) + "man";
			if(word.matches(".*ies$"))
				return word.substring(0, word.length() - 3) + "y";
			if(word.matches(".*es$"))
				return word.substring(0, word.length() - 2);
			
		}
		if(word.matches(".*[^s]s$"))
			return word.substring(0, word.length() - 1);
		return word;
	}
	
	/**
	 * Grammar: returns the plural lower case version of a word
	 */
	private static String toPlural(String word){
		word = word.toLowerCase();
		
		if(word.equals("lava") || word.equals("water") || word.equals("wool") || word.endsWith("grass") || word.endsWith("glass"))
			return word;

		if(word.matches(".*[sxz(ch)(sh)]$"))
			return word + "es";
		if(word.matches(".*o$"))
			return word + "es";
		if(word.matches(".*f$"))
			return word.substring(0, word.length() - 1) + "ves";
		if(word.matches(".*man$"))
			return word.substring(0, word.length() - 3) + "men";
		if(word.matches(".*ife$"))
			return word.substring(0, word.length() - 3) + "ives";
		if(word.matches(".*y$"))
			return word.substring(0, word.length() - 1) + "ies";
		return word + "s";
	}
	
	private static boolean addRemoveCoordinates(CommandSender sender, String[] args){
		File coordFile = new File("plugins" + File.separator + "LetItRain" + File.separator + "coordinates.yml");
		FileConfiguration coords = YamlConfiguration.loadConfiguration(coordFile);
		
		if(!sender.hasPermission("LetItRain.rain.coordinates")){
			Resources.privateMsg(sender, "You do not have permission to execute this command");
			return true;
		}
		
		if(args.length != 2){
			Resources.privateMsg(sender, "/rain add <location_name>");
			return true;
		}
		if(isNotPlayer(sender))
			return true;
		
		Player p = (Player) sender;
		Location l = p.getLocation();
		
		if(args[0].equals("add")){
			coords.set("LetItRain." + p.getWorld().getName() + "." + args[1], l.getX() + " " + l.getY() + " " + l.getZ());
			if(!LetItRain.coordinates.add(new Coordinate(args[1], p.getWorld().getName(), l.getX(), l.getY(), l.getZ())))
				Resources.privateMsg(sender, "The command has failed. It is likely that a location with the same name already exists");
			else
				Resources.privateMsg(sender, "The coordinate has been added");
		}else{
			if(coords.get("LetItRain." + p.getWorld().getName() + "." + args[1]) != null){
				coords.set("LetItRain." + p.getWorld().getName() + "." + args[1], null);
				Resources.privateMsg(sender, "The coordinate has been removed");
			}else
				Resources.privateMsg(sender, "The coordinate does not exist");
		}
		try {
			coords.save(coordFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private static boolean isNotPlayer(CommandSender sender){
		if (!(sender instanceof Player)){
			Resources.privateMsg(sender, "You cannot use this command from the console without specifying a player name");
			return true;
		}
		return false;
	}
}
