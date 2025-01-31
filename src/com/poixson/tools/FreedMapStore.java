package com.poixson.tools;

import static com.poixson.tools.xJavaPlugin.LOG_PREFIX;
import static com.poixson.utils.Utils.SafeClose;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.map.MapView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.poixson.pluginlib.pxnPluginLib;
import com.poixson.tools.events.PluginSaveEvent;
import com.poixson.tools.events.xListener;


public class FreedMapStore extends xListener<pxnPluginLib> {

	public static final int MAX_MAP_ID = Integer.MAX_VALUE;

	protected final ConcurrentSkipListSet<Integer> freed = new ConcurrentSkipListSet<Integer>();

	protected final File file;

	protected final AtomicBoolean changed = new AtomicBoolean(false);



	public FreedMapStore(final pxnPluginLib plugin, final String path) {
		super(plugin);
		this.file = new File(path, "freed-maps.json");
	}



	public synchronized void load() throws IOException {
		this.freed.clear();
		if (this.file.isFile()) {
			this.log().info(LOG_PREFIX + "Loading: freed-maps.json");
			BufferedReader reader = null;
			try {
				reader = Files.newBufferedReader(this.file.toPath());
				final Type token = new TypeToken<HashSet<Integer>>() {}.getType();
				final Set<Integer> set = (new Gson()).fromJson(reader, token);
				for (final Integer id : set)
					this.freed.add(id);
				this.changed.set(false);
			} catch (IOException e) {
				throw e;
			} finally {
				SafeClose(reader);
			}
		} else {
			this.log().info(LOG_PREFIX + "File not found: freed-maps.json");
			this.changed.set(true);
		}
	}
	public boolean save() throws IOException {
		if (this.changed.getAndSet(false)) {
			final Integer[] list = this.freed.toArray(new Integer[0]);
			final int[] result = new int[list.length];
			if (list.length > 0) {
				int i = 0;
				for (final Integer id : list)
					result[i++] = id.intValue();
			}
			this.log().info(String.format("%sSaving %d freed maps", LOG_PREFIX, result.length));
			BufferedWriter writer = null;
			try {
				final String data = (new Gson()).toJson(result);
				writer = new BufferedWriter(new FileWriter(this.file));
				writer.write(data);
				return true;
			} catch (IOException e) {
				throw e;
			} finally {
				SafeClose(writer);
			}
		}
		return false;
	}

	@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
	public void onPluginSave(final PluginSaveEvent event) {
		try {
			this.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	public int get() {
		final Integer next = this.freed.pollFirst();
		if (next != null) {
			this.changed.set(true);
			return next.intValue();
		}
		final MapView map = Bukkit.createMap(Bukkit.getWorld("world"));
		return map.getId();
	}
	public boolean release(final int map_id) {
		final boolean result = this.freed.add(Integer.valueOf(map_id));
		this.changed.set(true);
		return result;
	}



	public Logger log() {
		return this.plugin.getLogger();
	}



}
