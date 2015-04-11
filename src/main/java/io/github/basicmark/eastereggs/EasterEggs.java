package io.github.basicmark.eastereggs;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class EasterEggs extends JavaPlugin implements Listener {
	static final String displayPrefix = "" + ChatColor.BOLD + ChatColor.AQUA;
	Random random;
	int dropChance;
	Map<Integer, ItemStack[]> drops;

	public void loadConfig() {
		FileConfiguration config = getConfig();
		
		dropChance = config.getInt("dropchance", 5);
		ConfigurationSection dropConfig = config.getConfigurationSection("drops");
		Set<String> keys = dropConfig.getKeys(false);
		for (String key : keys) {
			ConfigurationSection items = dropConfig.getConfigurationSection(key);
			ItemStack[] newArray = new ItemStack[items.getKeys(false).size()];
			int index = 0;
			for (String item : items.getKeys(false)) {
				ItemStack newItem = (ItemStack) items.get(item);
				newArray[index++] = newItem;
			}
			drops.put(Integer.parseInt(key), newArray);
		}
	}

	public void onEnable(){
		random = new Random();
		drops = new HashMap<Integer, ItemStack[]>();
		// Create/load the config file
		saveDefaultConfig();
		loadConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}

	public void onDisable(){
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("eastereggs")){
			if ((args.length == 1) && args[0].equals("reload")) {
				if (sender.hasPermission("eastereggs.cmd.reload")) {
					reloadConfig();
					loadConfig();
				} else {
					sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
				}
				return true;
			}
			if ((args.length == 1) && args[0].equals("give")) {
				if (sender.hasPermission("eastereggs.cmd.give")) {
					if (sender instanceof Player) {
						Player player = (Player) sender;
						player.getInventory().addItem(new ItemStack(Material.DRAGON_EGG, 64));
					} else {
						sender.sendMessage(ChatColor.RED + "Only players can be given items!");
					}
				} else {
					sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
				}
				return true;
			}
		}
		return false;
	}

	private void fireworkEffect(Location location) {
		Random random = new Random();
		Boolean flicker = random.nextBoolean();
		Boolean trail = random.nextBoolean();
		Color mainColor = Color.fromBGR(random.nextInt(255), random.nextInt(255), random.nextInt(255));
		Color fadeColor = Color.fromBGR(random.nextInt(255), random.nextInt(255), random.nextInt(255));
		Type type = Type.values()[random.nextInt(Type.values().length-1)];
		FireworkEffect effect = FireworkEffect.builder().flicker(flicker).withColor(mainColor).withFade(fadeColor).with(type).trail(trail).build();
		Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
		FireworkMeta fwm = fw.getFireworkMeta();
		fwm.addEffect(effect);
		fwm.setPower(0);
		fw.setFireworkMeta(fwm); 
	}

	private void spawnDrops(Location location) {
		ItemStack toDrop = null;
		/* Pick a random number and see which chance group it falls into */
		int rand = random.nextInt(99);
		int start = 0;

		for (int chance : drops.keySet()) {
			if ((rand >= start) && (rand < (start + chance))) {
				/* Select one of the items from this chance group */
				ItemStack[] dropArray = drops.get(chance);
				toDrop = dropArray[random.nextInt(dropArray.length)];
				break;
			}
			start += chance;
		}

		World world = location.getWorld();
		if (toDrop != null) {
			world.dropItemNaturally(location, toDrop);
		}

		fireworkEffect(location);
	}

	/*
	 * If the egg can't land and gets turned into an item then just give the drops.
	 */
	@EventHandler
	public void onItemSpawnEvent(ItemSpawnEvent event) {
		Item spawned = event.getEntity();
		if (spawned.getItemStack().getType() == Material.DRAGON_EGG) {
			spawnDrops(spawned.getLocation());
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.hasBlock()) {
			Block clicked = event.getClickedBlock();
			if (clicked.getType() == Material.DRAGON_EGG) {
				/* Keep normal behaviour for creative players */
				if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE))
					return;

				if (random.nextInt(dropChance) == 0) {
					clicked.setType(Material.AIR);
					spawnDrops(clicked.getLocation());
					event.setCancelled(true);
				}
			}
		}
	}
}

