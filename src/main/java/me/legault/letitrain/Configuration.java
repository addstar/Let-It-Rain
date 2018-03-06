package me.legault.letitrain;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * me.legault.letitrain
 * Created for the Addstar MC for Let-It-Rain
 * Created by Narimm on 6/03/2018.
 */
public class Configuration {

    private static Logger log;

    public int defLightAmount, Zeusdelay, dAmount, maxAmount, maxRadius, dRadius, dLightningPower;
    public boolean usingZeus, dRemoveArtifact, destructiveArrows, checkForUpdate, rainBlocks, rainPotions, rainLava, rainWater, dispenserWorksWithFireSnowballs;
    public String rainLightnings, ZeusWait, dPunishMsg, dZeusMsg, dGrenadeMsg, dRainMsg, dFirerainMsg;
    private int item;
    private int itemZeus;
    public Material zeusMaterial, launcherMaterial;
    public List<String> entityBlackListName;
    private File coordFile = new File("plugins" + File.separator + "LetItRain" + File.separator + "coordinates.yml");


    private static FileConfiguration config, coords;
    public Map<String, Location> coordinates;

    public Configuration() {
        log = LetItRain.plugin.getLogger();
        loadConfig();
        initializeCoordinates();
    }

    private void loadConfig() {
        config = LetItRain.plugin.getConfig();
        setConfigHeader(config);
        setDefaults();
        saveConfig();
    }

    public void reloadConfig() {
        try {

            File confRain = new File("plugins" + File.separator + "LetItRain" + File.separator + "config.yml");
            config.load(confRain);
            setDefaults();
            config.save(confRain);

        } catch (Exception e) {
            e.printStackTrace();
            log.severe("An error has been detected with the config file. Default values will be loaded");
        } finally {
            LetItRain.plugin.saveConfig();
        }
    }

    public void saveConfig() {
        saveCoordinates();
        LetItRain.plugin.saveConfig();
    }

    private void setDefaults() {
        usingZeus = conf("LetItRain.Zeus.Show Player Using Zeus", true);
        Zeusdelay = conf("LetItRain.Zeus.Delay", 5);
        ZeusWait = conf("LetItRain.Zeus.Delay Message", "&cWait to use the Lightning again");
        checkForUpdate = conf("LetItRain.Check for updates", true);
        dLightningPower = conf("LetItRain.Zeus.Lightning explosion power", 15);
        dZeusMsg = conf("LetItRain.Zeus.Message", "[player] shall bring peace");
        dGrenadeMsg = conf("LetItRain.Grenade Launcher.Message", "Feel the power of [player]");
        dPunishMsg = conf("LetItRain.Punish.Message", "May the Gods punish [player] for his incompetence ");

        dispenserWorksWithFireSnowballs = conf("LetItRain.Rain.Dispensers can shoot explosive snowballs", true);
        rainLightnings = conf("LetItRain.Rain.Raining Lightnings", "The power of Zeus is with [player]");
        maxAmount = conf("LetItRain.Rain.Maximum amount", 4096);
        maxRadius = conf("LetItRain.Rain.Maximum radius", 200);
        defLightAmount = conf("LetItRain.Rain.Default Lightning amount", 10);
        dRemoveArtifact = !conf("LetItRain.Rain.Drops from corpses", true);//Note: the nots are important. Don't delete
        dRainMsg = conf("LetItRain.Rain.Rain Message", "May [entity] rain upon [player] ");
        dFirerainMsg = conf("LetItRain.Rain.Firerain message", "May burning [entity] rain upon [player] ");
        destructiveArrows = conf("LetItRain.Rain.Deep impact arrows", true);
        //itemZeus = conf("LetItRain.Zeus.Launcher id", 369);
        //item = conf("LetItRain.Grenade Launcher.Launcher id", 377);
        dAmount = conf("LetItRain.Rain.Default amount", 500);
        dRadius = conf("LetItRain.Rain.Default radius", 30);
        rainBlocks = !conf("LetItRain.Rain.Blacklist.Block", false); //Note: the nots are important. Don't delete
        rainPotions = !conf("LetItRain.Rain.Blacklist.Potion", false);//Note: the nots are important. Don't delete
        rainLava = conf("LetItRain.Rain.Blacklist.Lava", false);
        rainWater = conf("LetItRain.Rain.Blacklist.Water", false);

        Object mat = config.get("LetItRain..Grenade Launcher.Launcher", Material.BLAZE_POWDER);
        if (mat instanceof Material) {
            launcherMaterial = (Material) mat;
        } else {
            item = config.getInt("LetItRain..Grenade Launcher.Launcher id");
            launcherMaterial = Material.getMaterial(itemZeus);
            config.set("LetItRain..Grenade Launcher.Launcher id", null);
            if (launcherMaterial != null) {
                config.set("LetItRain..Grenade Launcher.Launcher", launcherMaterial);
            } else {
                config.set("LetItRain..Grenade Launcher.Launcher", Material.BLAZE_POWDER);
            }
        }

        mat = config.get("LetItRain.Zeus.Launcher", Material.DEAD_BUSH);
        if (mat instanceof Material) {
            zeusMaterial = (Material) mat;
        } else {
            itemZeus = config.getInt("LetItRain.Zeus.Launcher id");
            zeusMaterial = Material.getMaterial(itemZeus);
            config.set("LetItRain.Zeus.Launcher id", null);
            if (zeusMaterial != null) {
                config.set("LetItRain.Zeus.Launcher", zeusMaterial);
            } else {
                config.set("LetItRain.Zeus.Launcher", Material.DEAD_BUSH);

            }
        }
        loadEntityBlackList();
    }

    private void loadEntityBlackList(){
        String basePath ="LetItRain.Rain.Blacklist" ;
        ConfigurationSection result = config.getConfigurationSection(basePath);
        if(result != null){
        for(String key : result.getKeys(false)){
            boolean bool = config.getBoolean(basePath+"."+key);
            if(bool){
                entityBlackListName.add(key);
            }
        }
        }else setDefaultEntityBlackList();
    }

    private void setDefaultEntityBlackList(){
        List<EntityType> entityBlackList;
        entityBlackList = new ArrayList<>();
        entityBlackList.add(EntityType.ENDER_DRAGON);
        entityBlackList.add(EntityType.ENDERMAN);
        entityBlackList.add(EntityType.BLAZE);
        entityBlackList.add(EntityType.GHAST);
        entityBlackList.add(EntityType.IRON_GOLEM);
        entityBlackList.add(EntityType.ENDER_CRYSTAL);
        entityBlackList.add(EntityType.SNOWMAN);
        entityBlackList.add(EntityType.SLIME);
        entityBlackList.add(EntityType.MAGMA_CUBE);
        entityBlackList.add(EntityType.BAT);
        entityBlackList.add(EntityType.ENDER_SIGNAL);
        for (EntityType type: entityBlackList){
            entityBlackListName.add(type.getEntityClass().getSimpleName());
            Collections.sort(entityBlackListName);
        }
        for (String type:entityBlackListName) {
            config.set("LetItRain.Rain.Blacklist."+type, true);
        }
    }


    private static String conf(String identifier, String value) {
        if (!config.contains(identifier)) {
            config.set(identifier, value);
            return value;
        } else
            return config.getString(identifier);
    }

    private static boolean conf(String identifier, boolean value) {
        if (!config.contains(identifier)) {
            config.set(identifier, value);
            return value;
        } else
            return config.getBoolean(identifier);
    }

    private static int conf(String identifier, int value) {
        if (!config.contains(identifier)) {
            config.set(identifier, value);
            return value;
        } else
            return config.getInt(identifier);
    }

    private void initializeCoordinates() {
        coordinates = new HashMap<>();
        if (coordFile.exists()) {
            coords = YamlConfiguration.loadConfiguration(coordFile);
            loadCoordinates();
        }
        setConfigHeader(coords);
        saveCoordinates();
    }

    private void loadCoordinates() {
        Object result = coords.get("Locations", null);

        if (result != null && result instanceof Map) {
            try {
                coordinates.putAll((Map<? extends String, ? extends Location>) result);
            } catch (ClassCastException e) {
                log.warning("Location Configuration error");
            }
        }
    }

    public void saveCoordinates() {
        try {
            coords.set("Locations", coordinates);
            coords.save(coordFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setConfigHeader(FileConfiguration  fileConfig){
        String EOL = System.getProperty("line.separator");
        fileConfig.options().header(
                "Let It Rain plugin"+EOL+EOL
                +LetItRain.plugin.getDescription().getAuthors()+EOL
                +"Version: " +LetItRain.version + EOL
        );
    }
}
