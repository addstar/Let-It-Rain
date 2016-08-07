package me.legault.letitrain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import au.com.addstar.monolith.StringTranslator;
import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.monolith.lookup.MaterialDefinition;

@SuppressWarnings("deprecation")
public class Rain implements CommandExecutor, TabCompleter{
	
	public static HashMap<Entity, Boolean> thrownedItems = new HashMap<Entity, Boolean>();
	public static HashMap<Integer, Integer> runningTasks = new HashMap<Integer, Integer>();
	public static AtomicInteger taskIdentifier = new AtomicInteger(Integer.MIN_VALUE);
	public static Random randomGen = new Random();

	public boolean onCommand(final CommandSender sender, Command cmd, String label,  String[] args){
		boolean isAmountInit = false;
		boolean isTime = false;
		boolean isOnFire = false;
		boolean useParticleEffects = false;
		int amount = LetItRain.dAmount;		// Number of items to drop (default 500); Or, if isTime is true, the length, in seconds, to drop items
		int amountPerSecond = 100;			// Number of items per second to drop; only valid when isTime is true
		int radius = LetItRain.dRadius;		// Radius to drop items; default 30
		String targetName = null;
		Location targetLocation = null;
		EntityType entityToDrop = null;
		PotionType potion = null;
		boolean isLightning = false;
		ItemStack stack = new ItemStack(Material.AIR);
		Particle effectType = Particle.FIREWORKS_SPARK;

		final int TIME_DELAY_SECONDS = 7;

		// Check permissions
		if (!sender.isOp() && !sender.hasPermission("LetItRain.rain"))
			return true;

		// rain | firerain | effectrain syntax:
		// rain <entity|item|hand> <player|coord name> <amount|duration|AmountPerSecond/duration> <radius>
		// effectrain <entity|item|hand> <player|coord name> <amount|duration|AmountPerSecond/duration> <radius> [effect]
		//
		// Examples:
		//   rain diamond 5s 20						// Drop 100 diamonds/second for 5 seconds around the current player, radius 20
		//   rain diamond playerName 5s 20			// Drop 100 diamonds/second for 5 seconds around the specified player, radius 20
		//   rain hand playerName 5s 20				// Drop 100 per second of the item in your hand for 5 seconds around the specified player, radius 20
		//   rain hand playerName 64/30 20			// Drop 64 per second of the item in your hand for 30 seconds around the specified player, radius 20
		//   rain hand playerName 64/30s 20			// Drop 64 per second of the item in your hand for 30 seconds around the specified player, radius 20
		//   rain potion:FIRE_RESISTANCE 15 20  	// Drop 15 fire resistance potions around the current player, radius 20

		//   rain lightning		            		// Spark lightning 10 times (default) around the current player (2 per second), radius 30
		//   rain lightning 4 20		            // Spark lightning 4 times around the current player (2 per second), radius 20
		//   rain lightning 5s 20		            // Spark lightning around the current player for 5 seconds (1 per second), radius 20
		//   rain lightning playerName 5s 10		// Spark lightning around the named player for 5 seconds, radius 10
		//   rain zeus playerName 5s 10				// Same as previous command

		//   effectrain diamond playerName 5s 20 explosion_normal		// Use effect explosion_norml when dropping items
		//   effectrain diamond namedLocation 5s 20 explosion_normal

		Logger log = LetItRain.plugin.getLogger();

		// FireRain command
		if (label.equalsIgnoreCase("firerain")) {
			isOnFire = true;
		}

		// EffectRain command
		if (label.equalsIgnoreCase("effectrain")) {
			useParticleEffects = true;
			if (args.length > 4) {
				try {
					effectType = Particle.valueOf(args[4].toUpperCase());
					// Blank out the argument so that it is not parsed in the for loop below
					args[4] = "";
				}
				catch (Exception e) {
					Resources.privateMsg(sender, ChatColor.RED + "Unknown particle effect type \"" + args[4].toUpperCase() + "\".");
					Resources.privateMsg(sender, ChatColor.DARK_AQUA + "See available effects with: /letitrain effects");
					return true;
				}
			}
		}

		// Give command usage if no params specified
		if (args == null || args.length == 0) {
			displayHelp(label, sender, useParticleEffects);
			return true;
		}
		
		// Help
		if (args[0].equalsIgnoreCase("help")){
			displayHelp(label, sender, useParticleEffects);
			return true;
		}
		
		// Check if the user tries to add/remove a command
		if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete"))
			return addRemoveCoordinates(sender, args);

		if (args[0].equalsIgnoreCase("lightning") || args[0].equalsIgnoreCase("zeus")){
			if (!sender.isOp() && !sender.hasPermission("LetItRain.rain.lightning")){
				return true;
			}
			isLightning = true;
			amount = LetItRain.defLightAmount;
		} else {

			// Parse potions
			if(LetItRain.rainPotions && (args[0].startsWith("potion:") || args[0].startsWith("potions:"))){
				if(args[0].startsWith("potion:"))
					potion = findPotion(args[0].substring("potion:".length()));
				else
					potion = findPotion(args[0].substring("potions:".length()));
			} else {
				// Parse the first argument to see if it is a mob or entity
				// entityToDrop will be null if it is not a recognized entity
				entityToDrop = findEntity(args[0]);
			}
		}

		if (!isLightning && potion == null && entityToDrop == null && LetItRain.rainBlocks){
			if (args[0].equalsIgnoreCase("hand") && sender instanceof Player && ((Player)sender).getItemInHand() != null && !((Player)sender).getItemInHand().getType().equals(Material.AIR)){
				stack = ((Player)sender).getItemInHand();
			} else {

				// Method 1: check Material enum
				if (false) {
					Material mat = findMaterial(args[0]);

					if (mat == null || mat == Material.AIR) {
						Resources.privateMsg(sender, ChatColor.RED + "Item to drop is not a recognized material: " + args[0]);
						return true;
					}
					stack = new ItemStack(mat);

					if (args[0].contains(":")) {
						stack = new ItemStack(findMaterial(args[0]), 1, (short) Integer.parseInt(args[0].split(":")[1]));
						//mat = findMaterial(args[0].split(":")[0]);
						//matID = Integer.parseInt(args[0].split(":")[1]);
					}
				} else {
					// Method 2: use Monolith
					MaterialDefinition itemDef = getItem(args[0]);
					if (itemDef == null) {
						Resources.privateMsg(sender, ChatColor.RED + "Item to drop is not a recognized material: " + args[0]);
						return true;
					}

					stack = itemDef.asItemStack(1);
				}
			}						
		}		

		// Validate entities/potions
		if (entityToDrop == null && stack.getType().equals(Material.AIR) && potion == null && !isLightning){
			Resources.privateMsg(sender, "Please enter a valid entity/material id or name");
			if (!LetItRain.rainBlocks)
				Resources.privateMsg(sender, "Blocks have been disabled ");
			return true;
		}

		//Parse remaining arguments
		int recognizedParams = 0;
		for (int i = 1; i < args.length; i++){

			if (Strings.isNullOrEmpty(args[i])) {
				recognizedParams++;
				continue;
			}

			//Parse player/target
			if (targetName == null){
				
				//Find programmable target
				Coordinate c = null;
				for(Coordinate f: LetItRain.coordinates) {
					if (f.hasName(args[i])) {
						c = f;
						break;
					}
				}

				if(c != null){
					targetName = args[i];
					World w = LetItRain.server.getWorld(c.world);
					targetLocation = new Location(w, c.x, c.y, c.z);
				}else{
					//Find player
					Player target = Resources.isPlayer(args[i]);
					if(target != null){
						targetName = target.getDisplayName();
						targetLocation = target.getLocation();
					}
				}
				if (targetName != null)
					recognizedParams++;
			}
			
			//Help
			if (args[i].equalsIgnoreCase("help")){
				displayHelp(label, sender, true);
				return true;
			}
			
			// Parse amount and radius
			try{
				int slashIndex = args[i].indexOf("/");
				if (slashIndex > 0) {
					// Argument specifies quantity per second and time (in seconds)
					isTime = true;
					amountPerSecond = Integer.parseInt(args[i].substring(0, slashIndex));
					String durationSeconds = args[i].substring(slashIndex+1);
					if (durationSeconds.endsWith("s")) {
						durationSeconds = durationSeconds.substring(0, durationSeconds.length() - 1);
					}
					amount = Integer.parseInt(durationSeconds);

					recognizedParams++;
					isAmountInit = true;

				} else {

					if (args[i].endsWith("s") && !isAmountInit) {
						isTime = true;
						args[i] = args[i].substring(0, args[i].length() - 1);
					}
					int holder = Integer.parseInt(args[i]);
					recognizedParams++;
					if (!isAmountInit) {
						// If isTime is true, amount tracks the length, in seconds, to rain items
						// Otherwise, amount tracks the number of items to drop
						amount = holder;
						isAmountInit = true;
					} else {
						radius = holder;
					}
				}
			} catch(NumberFormatException e){}
		}
		
		//Parameter not recognized
		if (recognizedParams < args.length - 1){
			Resources.privateMsg(sender, "One or more of your parameters were not recognized");
			if (useParticleEffects)
				Resources.privateMsg(sender, "See available effects with: /letitrain effects");
			return false;
		}
		
		//Impossible parameters
		if (radius < 1 || amount < 1){
			Resources.privateMsg(sender, "Send at least one entity with a radius of at least 1");
			return true;
		}else if (amount > LetItRain.maxAmount){
			Resources.privateMsg(sender, "The maximum entities allowed is " + LetItRain.maxAmount + "; " + amount + " is too large");
			return true;
		}
		
		//Max radius
		if(isTime && radius > LetItRain.maxRadius){
			Resources.privateMsg(sender, "The maximum radius is " + LetItRain.maxRadius + "; " + radius + " is too large");
			return true;
		}
		
		//Returns false if console forgot name as parameter
		if(targetName == null && isNotPlayer(sender)) {
			return true;
		}

		//Defaults
		if (targetName == null){
			targetName = ((Player) sender).getDisplayName();
			targetLocation = ((Player) sender).getLocation();
		}
		
		//Test whether the animal is blacklisted
		if (entityToDrop != null){
			try{
				if (LetItRain.config.getBoolean("LetItRain.Rain.Blacklist." + entityToDrop.getEntityClass().getSimpleName())){
					Resources.privateMsg(sender, "The entity you chose has been blacklisted");
					return true;
				}
			}catch(Exception e){
				Resources.privateMsg(sender, "An unknown exception has occurred with your config file. Please try again.");
				return true;
			}
		}
		
		//Test whether the lava or water is blacklisted
		if (stack.getType().equals(Material.WATER) || stack.getType().equals(Material.LAVA)){
			try{
				if (LetItRain.config.getBoolean("LetItRain.Rain.Blacklist.Lava") || LetItRain.config.getBoolean("LetItRain.Rain.Blacklist.Water")){
					Resources.privateMsg(sender, "The item you chose has been blacklisted");
					return true;
				}
			}catch(Exception e){
				Resources.privateMsg(sender, "An unknown exception has occurred with your config file. Please try again.");
				return true;
			}
		}

		if (isTime && isLightning) {
			// Bump up amount by TIME_DELAY_SECONDS
			amount += TIME_DELAY_SECONDS;
		}

		final long initTime = System.currentTimeMillis();
		final int myTaskIdentifier = GetNextTaskID();
		final PotionType fPotion = potion;
		final Location fLocation = targetLocation;
		final ItemStack fStack = stack;
		final int fRadius = radius;
		final int fAmount = amount;
		final int fAmountPerSecond = amountPerSecond;
		final EntityType fEntityToDrop = entityToDrop;
		final boolean fIsOnFire = isOnFire;
		final boolean fisLightning = isLightning;
		final boolean fUseParticleEffects = useParticleEffects;
		final Particle fEffectType = effectType;

		if((!LetItRain.rainLava && stack.getType().equals(Material.LAVA)) || (!LetItRain.rainWater && stack.getType().equals(Material.WATER))){
			// WATER or LAVA
			Resources.privateMsg(sender, "Do not use water or lava! Bad things happen...");

			/*
			World w = targetLocation.getWorld();
			if(recognizedParams == 1)
				radius = amount;

			log.info("Dropping " + stack.getType().name() + " over a radius of " + radius);

			for(int i = -radius; i < radius; i++){
				double boundary = Math.sqrt(Math.pow(radius, 2) - Math.pow(i, 2));
				for(int j = -(int)boundary; j < boundary; j++)
					w.getBlockAt(new Location(targetLocation.getWorld(), targetLocation.getX() + i, targetLocation.getY() + 50, targetLocation.getZ() + j)).setType(Material.getMaterial(stack.getType().name()+"_BUCKET"));
			}
			*/
		}else if(isTime){

			int totalItemsToDrop = fAmountPerSecond * fAmount;

			if (fisLightning) {
				log.info("Lightning strikes for " + (fAmount - TIME_DELAY_SECONDS) + " seconds, radius " + fRadius);
			} else {
				String itemDescription = getDroppedItemDescription(fEntityToDrop, fStack, fPotion);
				String effectDescription = getEffectDescription(useParticleEffects, effectType);
				log.info("Dropping " + totalItemsToDrop + " " + itemDescription + " over " + fAmount + " seconds " +
						"(" + fAmountPerSecond + " items/sec), radius " + fRadius + effectDescription);
			}

			int id = LetItRain.server.getScheduler().scheduleSyncRepeatingTask(LetItRain.plugin, new Runnable(){

				// TIME based spawning (drop fAmountPerSecond for fAmount seconds)

				@Override
				public void run() {
					if(!spawnEntities(fLocation, fEntityToDrop, sender, fStack, fPotion, fAmountPerSecond, fRadius,
							fIsOnFire, fisLightning, fUseParticleEffects, fEffectType) ||
							System.currentTimeMillis() - initTime > Math.max(fAmount * 1000 - TIME_DELAY_SECONDS * 1000, 1000))
					StopScheduler(myTaskIdentifier);
				}
				
			}, 0L, 20L); // There are 20 server ticks per second
			runningTasks.put(myTaskIdentifier, id);

		}else if(isLightning){

			log.info(fAmount + " lightning strikes, radius " + fRadius);

			int id = LetItRain.server.getScheduler().scheduleSyncRepeatingTask(LetItRain.plugin, new Runnable(){

				@Override
				public void run() {
					spawnEntities(fLocation, fEntityToDrop, sender, fStack, fPotion, fAmount, fRadius,
							fIsOnFire, fisLightning, false, fEffectType);
				}				
			}, 0L,  10); // 2 lightning strikes per 1 second
			runningTasks.put(myTaskIdentifier, id);
			
			// Stop after x amount of lightning strikes
			LetItRain.server.getScheduler().scheduleSyncDelayedTask(LetItRain.plugin, new Runnable(){
				@Override
				public void run() {
					StopScheduler(myTaskIdentifier);
				}				
			}, (10*fAmount));
			
		}else{
			// ALL other drops

			String itemDescription = getDroppedItemDescription(fEntityToDrop, fStack, fPotion);
			String effectDescription = getEffectDescription(useParticleEffects, effectType);
			log.info("Dropping " + amount + " " + itemDescription + " over a radius of " + radius + effectDescription);

			boolean res = spawnEntities(targetLocation, fEntityToDrop, sender, fStack, fPotion, amount, radius,
					fIsOnFire, isLightning, useParticleEffects, effectType);
			if(!res)
				return true;
		}
		
		String name = "";
		
		if (fEntityToDrop != null)
			name = fEntityToDrop.getEntityClass().getSimpleName();
		else if(potion != null)
			name = potion.name() + " potion";
		else if(isLightning)
			name = "Lightning";
		else
			name = StringTranslator.getName(stack);

		name = name.replaceAll("_", " ").toLowerCase();
		
		// Fix up spawn egg names
		if (name.startsWith("spawn ")) {
			name = name.replace("spawn ", "") + " egg";
		}

		// Change "spruce wood" to "spruce log"
		if (name.endsWith(" wood")) {
			name = name.replace(" wood", " log");
		}

		if(amount > 1)
			name = toPlural(name);

		if(isLightning){
			Resources.broadcast(LetItRain.rainLightnings.replace("[player]", targetName));
			return true;
		}
		displayMsg(targetName, name, isOnFire);
		
		return true;

	}

	private static String getDroppedItemDescription(EntityType entityType, ItemStack stack, PotionType potionType) {
		if (entityType != null)
			return toPlural(entityType.name());

		if (potionType != null)
			return potionType.name().toLowerCase() + " potions";

		if (stack.getAmount() == 1)
			return toPlural(stack.getType().name());
		else
			return stack.toString();
	}

	private static String getEffectDescription(boolean useParticleEffects, Particle effectType) {
		if (!useParticleEffects)
			return "";

		return ", using effect " + effectType.name().toLowerCase();
	}

	private static boolean spawnEntities(Location location, EntityType entityType, CommandSender sender, ItemStack stack,
					PotionType potionType, int amount, int radius, boolean isOnFire, boolean isLightning,
					boolean useParticleEffects, Particle effectType){
		
		Location newLoc;
		if (isLightning){
			newLoc = location.clone();
			newLoc.setX(location.getX()+(double)randomGen.nextInt(radius*2)-(double)radius);
			newLoc.setZ(location.getZ()+(double)randomGen.nextInt(radius*2)-(double)radius);
			
			World world = location.getWorld();
			newLoc = world.getHighestBlockAt(newLoc.clone()).getLocation();
			world.createExplosion(newLoc, LetItRain.dLightningPower);
			world.strikeLightning(newLoc);
			return true;
		}

		List<Location> locs = new ArrayList<Location>();

		try{
			//Spawn entity
			for (int i = 0; i < amount; i++){
				newLoc = location.clone();
				newLoc.setX(location.getX()+(double)randomGen.nextInt(radius*2)-(double)radius);	
				newLoc.setY(location.getY()+(double)randomGen.nextInt(newLoc.getWorld().getMaxHeight()-100)+100.0);
				newLoc.setZ(location.getZ()+(double)randomGen.nextInt(radius*2)-(double)radius);

				locs.add(newLoc);

				if (entityType != null){
					if (entityType.name().equalsIgnoreCase("AREA_EFFECT_CLOUD")){
						newLoc = newLoc.getWorld().getHighestBlockAt(newLoc).getLocation();		
						newLoc.add(0, 1, 0);
					}
					
					final Entity creature = location.getWorld().spawn(newLoc, entityType.getEntityClass());
					thrownedItems.put(creature, isOnFire);
					
					if (entityType.name().equalsIgnoreCase("DRAGON_FIREBALL")){
						((DragonFireball)creature).setDirection(new Vector(0, -1, 0));
					}						
					if (entityType.name().equalsIgnoreCase("SHULKER")){
						moveDownCreature((Shulker)creature);							
					}					
						
					if (creature instanceof Fireball){
						((Fireball)creature).setDirection(new Vector(0, -1, 0));
					}
					if (creature instanceof ExperienceOrb){
						((ExperienceOrb) creature).setExperience(1000 + (int)randomGen.nextFloat()*300);
					}
					if (creature instanceof TNTPrimed){
						((TNTPrimed) creature).setFuseTicks(150);
					}
					if (isOnFire){
						creature.setFireTicks(1000 + (int)randomGen.nextFloat()*300);
					}
				} else {

					if(potionType != null){
						stack = new Potion(potionType).toItemStack(1);
					}
					
					location.getWorld().dropItem(newLoc, stack);
				}
			}
			// Do we want particle effects for the drops?
			if (useParticleEffects) {
				doParticleEffects(locs, effectType);
			}
		}catch(Exception e){
			Resources.privateMsg(sender, "This entity or world is invalid");
			return false;
		}
		return true;
	}
	
	private static void doParticleEffects(final List<Location> SpawnLocs, Particle effectType) {
		final long interval = 2; // particle tick interval (run effects every X ticks)
		final int seconds = 10;   // span effects over X seconds
		final int amount = SpawnLocs.size();
		final int perBatch = (int) Math.ceil(amount / Math.ceil((seconds * 20) / interval)); // split into per-tick batches
		final int minOffsetY = 3;
		final int maxOffsetY = 6;
		final Particle fEffectType = effectType;

		final int myTaskIdentifier = GetNextTaskID();
		
		int id = LetItRain.server.getScheduler().scheduleSyncRepeatingTask(LetItRain.plugin, new Runnable(){
			List<Location> locs = SpawnLocs;

			@Override
			public void run() {
				for (int x = 0; x < perBatch; x++) {
					if (locs.size() == 0) {
						StopScheduler(myTaskIdentifier);
						return;
					} else {
						Location loc = locs.remove(locs.size() - 1);
						loc.setY(loc.getWorld().getHighestBlockYAt(loc) + minOffsetY + randomGen.nextInt(maxOffsetY - minOffsetY));

						final int count = 3;
						final float offsetX = 0.15f;
						final float offsetY = 0.4f;
						final float offsetZ = 0.15f;
						final float extra = 3f;	// Usually speed

						loc.getWorld().spawnParticle(fEffectType, loc, count, offsetX, offsetY, offsetZ, extra);
					}
				}
			}
		}, 1L, interval);
		runningTasks.put(myTaskIdentifier, id);
		return;
	}
	
	private static void moveDownCreature(final Entity creature){
		final int taskid = new Random().nextInt();
		int id = LetItRain.server.getScheduler().scheduleSyncRepeatingTask(LetItRain.plugin, new Runnable(){

			@Override
			public void run() {
				Location l = creature.getLocation();
				Location newl = new Location(l.getWorld(), l.getX(), l.getY()-4, l.getZ());
				
				if (newl.getY() <= l.getWorld().getHighestBlockAt(l).getLocation().getY()){
					//creature.teleport(l.getWorld().getHighestBlockAt(l).getLocation());
					StopScheduler(taskid);
				} else {
					creature.teleport(newl);
				}
			}				
		}, 20L, 10L);
		runningTasks.put(taskid, id);
	}	

	private EntityType findEntity(String token){
		
		token = toSingular(token);
		if (token.equalsIgnoreCase("xporb") || token.equalsIgnoreCase("xp")){
			return EntityType.EXPERIENCE_ORB;
		}
		for(EntityType o: EntityType.values()){
			String name = o.name() == null ? "": o.name();
			String simpleName = o.getEntityClass() == null || o.getEntityClass().getSimpleName() == null ? "": o.getEntityClass().getSimpleName();
			
			if(toSingular(simpleName).equalsIgnoreCase(token) ||
					toSingular(name).equalsIgnoreCase(token))
					return o;
		}
		return null;
	}
	
	private MaterialDefinition getItem(String search)
	{
		// Get item value + data value
		String[] parts = search.split(":");
		String itemname = parts[0];
		MaterialDefinition def = getMaterial(itemname);
		if (def == null) return null;
		
		if(search.contains(":")) {
			String dpart = search.split(":")[1];
			try {
				short data = Short.parseShort(dpart);
				if(data < 0)
					throw new IllegalArgumentException("Data value for " + itemname + " cannot be less than 0");

				// Return new definition with specified data value
				return new MaterialDefinition(def.getMaterial(), data);
			}
			catch(NumberFormatException e) {
				throw new IllegalArgumentException("Data value after " + itemname);
			}
		} else {
			return def;
		}
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


	private Material findMaterial(String token){
		
		try{
			int id = Integer.parseInt(token);
			return Material.getMaterial(id);
		}catch(NumberFormatException e){	
			return Material.getMaterial(toSingular(token).toUpperCase());
		}
	}
		
	private PotionType findPotion(String token){
		
		token = token.replaceAll("[(potion|instant)_ ]", "").toLowerCase();
		
		for(PotionType o: PotionType.values())
			if(o.name().replaceAll("[(POTION|INSTANT)_ ]", "").equalsIgnoreCase(token))
				return o;
		return null;
	}

	private static int GetNextTaskID() {
		return taskIdentifier.getAndIncrement();
	}
	
	private static void StopScheduler(int id){
		Integer i = runningTasks.remove(id);
		if(i != null)
			LetItRain.server.getScheduler().cancelTask(i);
	}

	/**
	 * Display help
	 */
	private void displayHelp(String label, CommandSender sender, boolean useParticleEffects){
		if (useParticleEffects) {
			Resources.privateMsg(sender, "/" + label + " <entity|item|hand> <player|coordName> <amount|duration|amountPerSec/duration> <radius> [effect]");
			Resources.privateMsg(sender, "To specify effect, all parameters must be defined. Otherwise, uses FIREWORKS_SPARK.");
		} else {
			Resources.privateMsg(sender, "/" + label + " <entity|item|hand> <player|coordName> <amount|duration|amountPerSec/duration> <radius>");
			Resources.privateMsg(sender, "All parameters optional except entity. Order can be changed except for amount and radius.");
		}
		Resources.privateMsg(sender, "Alternatives: /rain | /firerain | /effectrain");
		Resources.privateMsg(sender, "  Specify strength potion with: /rain potion:strength");
		Resources.privateMsg(sender, "  Lightning strike with: /rain lightning");
		Resources.privateMsg(sender, "  Custom drops: xporb, fireball, dragon_fireball, TNTPrimed, area_effect_cloud");
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
		
		if(word.equals("lava") || word.equals("water") || word.equals("wool") || word.endsWith("grass")
				|| word.endsWith("glass") || word.endsWith("beef") || word.endsWith("planks"))
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
		
		if(!sender.isOp() && !sender.hasPermission("LetItRain.rain.coordinates")){
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
			if(!LetItRain.coordinates.add(new Coordinate(args[1], p.getWorld().getName(), l.getX(), l.getY(), l.getZ())))
				Resources.privateMsg(sender, "The command has failed. It is likely that a location with the same name already exists");
			else
				coords.set("LetItRain." + p.getWorld().getName() + "." + args[1], l.getX() + " " + l.getY() + " " + l.getZ());
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

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		SortedSet<String> tab = new TreeSet<String>();  
		List<String> tabsort = new ArrayList<String>(); 
		
		if (arg3.length == 1){
			Iterator<EntityType> it = LetItRain.defaultBlackList.iterator();
			while (it.hasNext()){
				EntityType type = it.next();
				String name = type.getEntityClass().getSimpleName();
				if (name.toLowerCase().startsWith(arg3[0].toLowerCase()) && !LetItRain.config.getBoolean("LetItRain.Rain.Blacklist."+name)){
					tab.add(name);
				}
			}			
		}
		tabsort.addAll(tab);
		return tabsort;
	}
}
