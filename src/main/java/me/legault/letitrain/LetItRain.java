/**
 * 
 * @title Let It Rain
 * @author Bathlamos, Maty241
 * @version 4.02
 * 
 */

package me.legault.letitrain;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class
 * Receives the command and calls proper class
 *
 */
public class LetItRain extends JavaPlugin{

	private static Logger log;
	public static FileConfiguration config, coords;
	public static Server server;
	public static Plugin plugin;
	public static List<Coordinate> coordinates;
	
	//Defaults
	public static int defLightAmount, Zeusdelay, dAmount, maxAmount, maxRadius, dRadius, dLightningPower;
	public static boolean usingZeus, dRemoveArtifact, destructiveArrows, checkForUpdate, rainBlocks, rainPotions, rainLava, rainWater, dispenserWorksWithFireSnowballs;
	public static String rainLightnings, ZeusWait, dPunishMsg, dZeusMsg, dGrenadeMsg, dRainMsg, dFirerainMsg;
	public static Material item, itemZeus;
	private Rain rainExec;
	private Zeus zeusExec;
	private Punish punishExec;
	private Launcher launcherExec;
	private RemoveItemsnSlaughter removeItems;
	private LetItRainHelp lirh;
	public static int version;
	
	public static List<EntityType> defaultBlackList;
		
	
	public void onEnable(){
		plugin = this;
		log = this.getLogger();
		
		server = this.getServer();
		
		server.getPluginManager().registerEvents(new Events(), this);
		
		version = getBukkitVersion();
		//Sets blacklist
		defaultBlackList = new ArrayList<EntityType>();
		defaultBlackList.add(EntityType.ENDER_DRAGON);
		defaultBlackList.add(EntityType.ENDERMAN);
		defaultBlackList.add(EntityType.BLAZE);
		defaultBlackList.add(EntityType.GHAST);
		defaultBlackList.add(EntityType.IRON_GOLEM);
		defaultBlackList.add(EntityType.ENDER_CRYSTAL);
		defaultBlackList.add(EntityType.SNOWMAN);
		defaultBlackList.add(EntityType.SLIME);
		defaultBlackList.add(EntityType.MAGMA_CUBE);
		defaultBlackList.add(EntityType.BAT);
		defaultBlackList.add(EntityType.ENDER_SIGNAL);
		
		extractCoordinates();
		createConfig();
		
		rainExec = new Rain();
		getCommand("rain").setExecutor(rainExec);
		getCommand("firerain").setExecutor(rainExec);
		getCommand("effectrain").setExecutor(rainExec);
		
		zeusExec = new Zeus(this);
		getCommand("zeus").setExecutor(zeusExec);
		
		punishExec = new Punish(this);
		getCommand("strike").setExecutor(punishExec);
		
		launcherExec = new Launcher(this);
		getCommand("launcher").setExecutor(launcherExec);
		
		removeItems = new RemoveItemsnSlaughter(this);
		getCommand("removeItems").setExecutor(removeItems);
		getCommand("slaughter").setExecutor(removeItems);
		
		lirh = new LetItRainHelp(this);
		getCommand("letitrain").setExecutor(lirh);
		
		log.info(Resources.getPluginTitle() + " enabled");
		
	}
	
	public void onDisable(){
		log.info(Resources.getPluginTitle() + " disabled");
	}
	
    private int getBukkitVersion(){
    	String name = Bukkit.getServer().getClass().getPackage().getName();
		String v = name.substring(name.lastIndexOf('.') + 1) + ".";
    	String[] version = v.replace('_', '.').split("\\.");
		
		int lesserVersion = 0;
		try {
			lesserVersion = Integer.parseInt(version[2]);
		} catch (NumberFormatException ex){				
		}
		return Integer.parseInt((version[0]+version[1]).substring(1)+lesserVersion);
    }
    
	private void extractCoordinates(){
		File coordFile = new File("plugins" + File.separator + "LetItRain" + File.separator + "coordinates.yml");
		coordinates = new LinkedList<Coordinate>();
		coords = YamlConfiguration.loadConfiguration(coordFile);
		
		coords.options().header(
				"Let It Rain programmable coordinates \n" +
				"The format is x y z\n\n" +
				"Maty241, Bathlamos, FabioZumbi12\n" +
				"Version " + LetItRain.plugin.getDescription().getVersion() + "\n\n\n" +
				"http://mathieu.legault.me/\n" +
				"http://bathlamos.me/\n" + 
				"http://areaz12server.net.br/\n");
		coords.set("LetItRain.world.samplePosition", "0 0 0");
		
		for(String g: coords.getConfigurationSection("LetItRain").getKeys(false)){
			for(String f: coords.getConfigurationSection("LetItRain." + g).getKeys(false)){
				String[] value = coords.getString("LetItRain." + g + "." + f).split(" ");
				double[] intValues = new double[3];
				if(value.length != 3){
					log.severe("The following coordinate could not be parsed: LetItRain." + g + "." + f);
					continue;
				}
				for(int i = 0; i < value.length; i++)
					try{
						intValues[i] = Double.parseDouble(value[i]);
					}catch(NumberFormatException e){
						log.severe("The following coordinate could not be parsed: LetItRain." + g + "." + f);
					}
				boolean hasBeenDefined = false;
				for(Coordinate c: coordinates)
					if(c.hasName(f)){
						log.severe("The following coordinate has already been defined: LetItRain." + g + "." + f);
						hasBeenDefined = true;
						break;
					}
				if(!hasBeenDefined)
					coordinates.add(new Coordinate(f, g, intValues[0], intValues[1], intValues[2]));
			}
		}
		
		try {
			coords.save(coordFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void reloadCfg(){		
		try{
			
			File confRain = new File("plugins" + File.separator + "LetItRain" + File.separator + "config.yml");
			config.load(confRain);
			AddProperties(config);			
			config.save(confRain);	
			
		}catch(Exception e){
			e.printStackTrace();
			log.severe("An error has been detected with the config file. Default values will be loaded");
		}finally{
			plugin.saveConfig();
		}
	}
	
	public static void createConfig(){		
		//Defaults
		dLightningPower = 15;
		ZeusWait = "&cWait to use the Lightning again";
		dZeusMsg = "[player] shall bring peace";
		dGrenadeMsg = "Feel the power of [player]";
		dPunishMsg = "May the Gods punish [player] for his incompetence ";
		maxAmount = 4096;
		dRemoveArtifact = true;
		dRainMsg = "May [entity]&r&b rain upon [player]";
		dFirerainMsg = "May burning [entity]&r&b rain upon [player]";
		rainLightnings = "The power of Zeus is with [player]";
		dAmount = 500;
		dRadius = 30;
		maxRadius = 200;
		defLightAmount = 10;
		
		try{
			config = plugin.getConfig();
			File confRain = new File("plugins" + File.separator + "LetItRain" + File.separator + "config.yml");
			AddProperties(config);
			config.save(confRain);	

						
		}catch(Exception e){
			e.printStackTrace();
			log.severe("An error has been detected with the config file. Default values will be loaded");
		}finally{
			plugin.saveConfig();
		}
	}
	
	@SuppressWarnings("deprecation")
	private static void AddProperties(FileConfiguration config){
		config.options().header(
				"Let It Rain plugin \n\n" +
				"Maty241, Bathlamos, FabioZumbi12\n" +
				"Version " + plugin.getDescription().getVersion() + "\n\n\n" +
				"http://mathieu.legault.me/\n" +
				"http://bathlamos.me/\n" + 
				"http://areaz12server.net.br/\n");
		
		usingZeus = conf("LetItRain.Zeus.Show Player Using Zeus", true);
		Zeusdelay = conf("LetItRain.Zeus.Delay", 5);
		ZeusWait = conf("LetItRain.Zeus.Delay Message", ZeusWait);
		checkForUpdate = conf("LetItRain.Check for updates", true);
		dLightningPower = conf("LetItRain.Zeus.Lightning explosion power", dLightningPower);
		dZeusMsg = conf("LetItRain.Zeus.Message", dZeusMsg);
		dGrenadeMsg = conf("LetItRain.Grenade Launcher.Message", dGrenadeMsg);
		dPunishMsg = conf("LetItRain.Punish.Message", dPunishMsg);

		dispenserWorksWithFireSnowballs = conf("LetItRain.Rain.Dispensers can shoot explosive snowballs", true);
		rainLightnings = conf("LetItRain.Rain.Raining Lightnings", rainLightnings);
		maxAmount = conf("LetItRain.Rain.Maximum amount", maxAmount);
		maxRadius = conf("LetItRain.Rain.Maximum radius", maxRadius);
		defLightAmount = conf("LetItRain.Rain.Default Lightning amount", defLightAmount);
		dRemoveArtifact = !conf("LetItRain.Rain.Drops from corpses", dRemoveArtifact);//Note: the nots are important. Don't delete
		dRainMsg = conf("LetItRain.Rain.Rain Message", dRainMsg);
		dFirerainMsg = conf("LetItRain.Rain.Firerain message", dFirerainMsg);
		destructiveArrows = conf("LetItRain.Rain.Deep impact arrows", true);
		dAmount = conf("LetItRain.Rain.Default amount", 500);
		dRadius = conf("LetItRain.Rain.Default radius", 30);
		rainBlocks = !conf("LetItRain.Rain.Blacklist.Block", false); //Note: the nots are important. Don't delete
		rainPotions = !conf("LetItRain.Rain.Blacklist.Potion", false);//Note: the nots are important. Don't delete
		rainLava = conf("LetItRain.Rain.Blacklist.Lava", false);
		rainWater = conf("LetItRain.Rain.Blacklist.Water", false);

		item = Material.getMaterial(conf("LetItRain.Grenade Launcher.Launcher id", "BLAZE_POWDER"));
		if (item == null) {
			log.severe("Invalid item in plugin.yml (<Grenade Launcher.Launcher id>)");
			item = Material.BLAZE_POWDER;
		}

		itemZeus = Material.getMaterial(conf("LetItRain.Zeus.Launcher id", "DEAD_BUSH"));
		if (itemZeus == null) {
			log.severe("Invalid item in plugin.yml (<Zeus.Launcher id>)");
			itemZeus = Material.BLAZE_ROD;
		}

		//Put the entities in alphabetical order
		Map<String, Boolean> entityNames = new HashMap<String, Boolean>();
		for(EntityType e: EntityType.values()){
			if (e.isSpawnable() && !config.contains("LetItRain.Rain.Blacklist." + e.getEntityClass().getSimpleName()))
				entityNames.put(e.getEntityClass().getSimpleName(), defaultBlackList.contains(e));
		}
		SortedSet<String> keys = new TreeSet<String>(entityNames.keySet());
		for(String key: keys){
			Boolean isBlacklisted = entityNames.get(key);
			config.set("LetItRain.Rain.Blacklist." + key, isBlacklisted);
		}
		
	}
	
	private static String conf(String identifier, String value){
		if (!config.contains(identifier)){
			config.set(identifier, value);
			return value;
		}else
			return config.getString(identifier);
	}
	
	private static boolean conf(String identifier, boolean value){
		if (!config.contains(identifier)){
			config.set(identifier, value);
			return value;
		}else
			return config.getBoolean(identifier);
	}
	
	private static int conf(String identifier, int value){
		if (!config.contains(identifier)){
			config.set(identifier, value);
			return value;
		}else
			return config.getInt(identifier);
	}
	
}
