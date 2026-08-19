package com.github.fengzhizi319.privacy.sdk.dynclassification.loader;

import com.github.fengzhizi319.privacy.sdk.dynclassification.engine.CompositeRuleEngine;
import com.github.fengzhizi319.privacy.sdk.dynclassification.engine.ConfigurableRuleEngine;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads, caches, and hot-reloads YAML rule configurations.
 * 负责加载、缓存和热重载 YAML 规则配置。
 */
public class ProfileLoader {
    private static final Logger log = LoggerFactory.getLogger(ProfileLoader.class);

    private final Path rulesDir;
    private final boolean hotReload;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, DomainTaxonomy> taxonomyCache = new ConcurrentHashMap<>();
    private final Map<String, RuleProfile> profileCache = new ConcurrentHashMap<>();
    private final Map<String, StandardDef> standardCache = new ConcurrentHashMap<>();
    private final Map<String, ConfigurableRuleEngine> engineCache = new ConcurrentHashMap<>();
    private final Map<String, CompositeRuleEngine> compositeCache = new ConcurrentHashMap<>();

    public ProfileLoader(String rulesDir) {
        String dir = rulesDir;
        if (dir == null || dir.isEmpty()) {
            dir = System.getenv("PRIVACY_DYNCLASSIFICATION_RULES_DIR");
        }
        if (dir == null || dir.isEmpty()) {
            dir = "rules";
        }
        this.rulesDir = Paths.get(dir);
        
        String hotReloadEnv = System.getenv("PRIVACY_DYNCLASSIFICATION_HOT_RELOAD");
        this.hotReload = !"false".equalsIgnoreCase(hotReloadEnv);
    }

    /**
     * Gets or builds a rule engine for the given domain/standard.
     * 获取或构建指定领域/标准的规则引擎。
     */
    public ConfigurableRuleEngine getEngine(String domain, String standard) {
        String cacheKey = engineCacheKey(domain, standard);
        
        lock.readLock().lock();
        try {
            ConfigurableRuleEngine cached = engineCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            ConfigurableRuleEngine cached = engineCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            ConfigurableRuleEngine engine = buildEngine(domain, standard);
            engineCache.put(cacheKey, engine);
            return engine;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets or builds a composite engine for the given domain/standard.
     * 获取或构建指定领域/标准的复合规则引擎。
     */
    public CompositeRuleEngine getCompositeEngine(String domain, String standard) {
        String cacheKey = engineCacheKey(domain, standard);
        
        CompositeRuleEngine cached = compositeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<CompositeRuleDef> compositeRules = new ArrayList<>();
        if (standard != null && !standard.isEmpty()) {
            try {
                StandardDef stdDef = loadStandard(standard);
                for (String d : stdDef.getDomains()) {
                    RuleProfile profile = loadProfile(d);
                    compositeRules.addAll(profile.getCompositeRules());
                }
            } catch (Exception e) {
                log.warn("Failed to load composite rules for standard {}: {}", standard, e.getMessage());
            }
        } else if (domain != null && !domain.isEmpty()) {
            try {
                RuleProfile profile = loadProfile(domain);
                compositeRules.addAll(profile.getCompositeRules());
            } catch (Exception e) {
                log.warn("Failed to load composite rules for domain {}: {}", domain, e.getMessage());
            }
        }

        CompositeRuleEngine engine = new CompositeRuleEngine(compositeRules, domain, standard);
        compositeCache.put(cacheKey, engine);
        return engine;
    }

    private ConfigurableRuleEngine buildEngine(String domain, String standard) {
        if (standard != null && !standard.isEmpty()) {
            return buildEngineFromStandard(standard);
        }
        if (domain != null && !domain.isEmpty()) {
            return buildEngineFromDomain(domain);
        }
        return buildDefaultEngine();
    }

    private ConfigurableRuleEngine buildEngineFromStandard(String standardId) {
        StandardDef stdDef = loadStandard(standardId);
        DomainTaxonomy taxonomy = loadTaxonomy(stdDef.getTaxonomy());

        List<RuleProfile> profiles = new ArrayList<>();
        for (String d : stdDef.getDomains()) {
            profiles.add(loadProfile(d));
        }

        return new ConfigurableRuleEngine(taxonomy, profiles, String.join(",", stdDef.getDomains()), standardId);
    }

    private ConfigurableRuleEngine buildEngineFromDomain(String domain) {
        DomainTaxonomy taxonomy;
        try {
            taxonomy = loadTaxonomy("default");
        } catch (Exception e) {
            taxonomy = DomainTaxonomy.createDefault();
        }

        RuleProfile profile = loadProfile(domain);
        List<RuleProfile> profiles = List.of(profile);

        return new ConfigurableRuleEngine(taxonomy, profiles, domain, "");
    }

    private ConfigurableRuleEngine buildDefaultEngine() {
        DomainTaxonomy taxonomy;
        try {
            taxonomy = loadTaxonomy("default");
        } catch (Exception e) {
            taxonomy = DomainTaxonomy.createDefault();
        }

        List<RuleProfile> profiles = new ArrayList<>();
        for (String name : new String[]{"general-pii", "medical"}) {
            try {
                profiles.add(loadProfile(name));
            } catch (Exception e) {
                // Skip missing domains
            }
        }

        if (profiles.isEmpty()) {
            profiles.add(RuleProfile.createDefault());
        }

        return new ConfigurableRuleEngine(taxonomy, profiles, "default", "");
    }

    /**
     * Loads a taxonomy from YAML.
     * 从 YAML 加载分类体系。
     */
    public DomainTaxonomy loadTaxonomy(String name) {
        DomainTaxonomy cached = taxonomyCache.get(name);
        if (cached != null) {
            return cached;
        }

        Path path = safePath("taxonomies", name);
        DomainTaxonomy taxonomy = loadYaml(path, DomainTaxonomy.class);
        
        if (taxonomy.getDefaultLevel() == null || taxonomy.getDefaultLevel().isEmpty()) {
            taxonomy.setDefaultLevel("L3");
        }
        if (taxonomy.getVersion() == null || taxonomy.getVersion().isEmpty()) {
            taxonomy.setVersion("1.0.0");
        }

        taxonomyCache.put(name, taxonomy);
        return taxonomy;
    }

    /**
     * Loads a domain profile from YAML.
     * 从 YAML 加载领域配置。
     */
    public RuleProfile loadProfile(String domain) {
        RuleProfile cached = profileCache.get(domain);
        if (cached != null) {
            return cached;
        }

        Path path = safePath("domains", domain);
        RuleProfile profile = loadYaml(path, RuleProfile.class);
        profileCache.put(domain, profile);
        return profile;
    }

    /**
     * Loads a standard definition from YAML.
     * 从 YAML 加载标准定义。
     */
    public StandardDef loadStandard(String standardId) {
        StandardDef cached = standardCache.get(standardId);
        if (cached != null) {
            return cached;
        }

        Path path = safePath("standards", standardId);
        StandardDef standard = loadYaml(path, StandardDef.class);
        standardCache.put(standardId, standard);
        return standard;
    }

    /**
     * Lists available standard names.
     * 列出可用的标准名称。
     */
    public List<String> listStandards() {
        return listYamlFiles("standards");
    }

    /**
     * Lists available domain names.
     * 列出可用的领域名称。
     */
    public List<String> listDomains() {
        return listYamlFiles("domains");
    }

    /**
     * Invalidates all caches.
     * 使所有缓存失效。
     */
    public void invalidateCache() {
        lock.writeLock().lock();
        try {
            taxonomyCache.clear();
            profileCache.clear();
            standardCache.clear();
            engineCache.clear();
            compositeCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- Internal helpers ---

    private Path safePath(String subdir, String name) {
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        Path path = rulesDir.resolve(subdir).resolve(name + ".yaml");
        if (!path.toAbsolutePath().normalize().startsWith(rulesDir.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Path traversal detected: " + name);
        }
        return path;
    }

    private <T> T loadYaml(Path path, Class<T> clazz) {
        if (!Files.exists(path)) {
            throw new RuntimeException("File not found: " + path);
        }
        try (InputStream is = Files.newInputStream(path)) {
            // Use Jackson ObjectMapper with YAML support for snake_case to camelCase conversion
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.dataformat.yaml.YAMLFactory yamlFactory = new com.fasterxml.jackson.dataformat.yaml.YAMLFactory();
            mapper = new com.fasterxml.jackson.databind.ObjectMapper(yamlFactory);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
            return mapper.readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToType(Object data, Class<T> clazz) {
        if (data == null) {
            return null;
        }
        // Use Jackson-like manual conversion via SnakeYAML's TypeDescription
        // For safety, we re-parse with Constructor restricted to our model package
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        // Restrict to safe types only
        Yaml yaml = new Yaml(new org.yaml.snakeyaml.constructor.Constructor(clazz, options));
        // Re-serialize and parse with type constraint
        String yamlStr = new Yaml(new SafeConstructor(new LoaderOptions())).dump(data);
        return yaml.load(yamlStr);
    }

    private List<String> listYamlFiles(String subdir) {
        Path dir = rulesDir.resolve(subdir);
        if (!Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .map(p -> p.getFileName().toString().replaceAll("\\.(yaml|yml)$", ""))
                .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private String engineCacheKey(String domain, String standard) {
        return (domain != null ? domain : "") + ":" + (standard != null ? standard : "");
    }
}
