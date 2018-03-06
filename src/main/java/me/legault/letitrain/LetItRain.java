/**
 * 
 * @title Let It Rain
 * @author Bathlamos, Maty241
 * @version 4.02
 * 
 */

package me.legault.letitrain;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class
 * Receives the command and calls proper class
 *
 */
public class LetItRain extends JavaPlugin{

	private static Logger log;
	public static Configuration config;
	public static Server server;
	public static Plugin plugin;

    public static int version;

	public void onEnable(){
		plugin = this;
		config = new Configuration();
		log = this.getLogger();
		
		server = this.getServer();
		
		server.getPluginManager().registerEvents(new Events(), this);
		
		version = getBukkitVersion();

        Rain rainExec = new Rain();
		getCommand("rain").setExecutor(rainExec);
		getCommand("firerain").setExecutor(rainExec);
		getCommand("effectrain").setExecutor(rainExec);

        Zeus zeusExec = new Zeus(this);
		getCommand("zeus").setExecutor(zeusExec);

        Punish punishExec = new Punish(this);
		getCommand("strike").setExecutor(punishExec);

        Launcher launcherExec = new Launcher();
		getCommand("launcher").setExecutor(launcherExec);

        RemoveItemsnSlaughter removeItems = new RemoveItemsnSlaughter();
		getCommand("removeItems").setExecutor(removeItems);
		getCommand("slaughter").setExecutor(removeItems);

        LetItRainHelp lirh = new LetItRainHelp(this);
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
		} catch (NumberFormatException ignored){
		}
		return Integer.parseInt((version[0]+version[1]).substring(1)+lesserVersion);
    }



	
}
