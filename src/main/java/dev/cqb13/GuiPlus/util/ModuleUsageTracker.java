package dev.cqb13.GuiPlus.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ModuleUsageTracker {
    private static final Map<String, UsageData> usageData = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService SAVE_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final long SAVE_DEBOUNCE_MS = 1000;
    private static File usageFile;
    private static long lastSaveTime = 0;

    public record UsageData(int toggleCount, long lastUsedTimestamp) {
    }

    public static void init(File configDir) {
        usageFile = new File(configDir, "gui-plus/usage.json");
        load();
    }

    public static void recordUsage(Module module) {
        if (module == null)
            return;

        String moduleName = module.name;
        UsageData existing = usageData.get(moduleName);
        long now = System.currentTimeMillis();

        if (existing != null) {
            usageData.put(moduleName, new UsageData(existing.toggleCount() + 1, now));
        } else {
            usageData.put(moduleName, new UsageData(1, now));
        }

        scheduleSave();
    }

    public static int getUseCount(Module module) {
        if (module == null)
            return 0;
        UsageData data = usageData.get(module.name);
        return data != null ? data.toggleCount() : 0;
    }

    public static long getLastUsed(Module module) {
        if (module == null)
            return 0;
        UsageData data = usageData.get(module.name);
        return data != null ? data.lastUsedTimestamp() : 0;
    }

    public static List<Module> getRecentlyUsedModules(int limit) {
        return usageData.entrySet().stream()
                .sorted(Map.Entry.<String, UsageData>comparingByValue(
                        Comparator.comparingLong(UsageData::lastUsedTimestamp).reversed()))
                .limit(limit)
                .map(entry -> Modules.get().get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static void scheduleSave() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime < SAVE_DEBOUNCE_MS)
            return;

        lastSaveTime = now;
        SAVE_EXECUTOR.schedule(() -> {
            save();
        }, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private static void save() {
        if (usageFile == null)
            return;

        try {
            usageFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(usageFile)) {
                GSON.toJson(usageData, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        if (usageFile == null || !usageFile.exists())
            return;

        try (FileReader reader = new FileReader(usageFile)) {
            Type type = new TypeToken<Map<String, UsageData>>() {
            }.getType();
            Map<String, UsageData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                usageData.clear();
                usageData.putAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
