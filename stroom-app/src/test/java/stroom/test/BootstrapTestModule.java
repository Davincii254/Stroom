package stroom.test;

import stroom.app.guice.BootStrapModule;
import stroom.config.app.AppConfig;
import stroom.config.app.Config;
import stroom.config.app.ConfigHolder;
import stroom.test.common.util.db.DbTestModule;
import stroom.util.io.FileUtil;

import com.google.inject.AbstractModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BootstrapTestModule extends AbstractModule {

    private final BootStrapModule bootStrapModule;

//    private BootstrapTestModule(final Config config, final ConfigHolder configHolder) {
//        super(config, null, configHolder, DbTestModule::new, AppConfigTestModule::new);
//
//
//        final ConfigHolder configHolder = new ConfigHolderImpl();
//        final Config config = new Config(configHolder.getBootStrapConfig());
//
//        // Delegate to the normal BootStrapModule but use different Db and AppConfig modules
//        bootStrapModule = new BootStrapModule(
//                config,
//                null,
//                configHolder,
//                DbTestModule::new,
//                AppConfigTestModule::new);

//        super(new Config(new ConfigHolderImpl()), null, Path.of("Dummy"),
//        DbTestModule::new, AppConfigTestModule::new);
//        config.setYamlAppConfig(configHolder.getBootStrapConfig());
//    }

    public BootstrapTestModule() {
        final ConfigHolder configHolder = new ConfigHolderImpl();
        final Config config = new Config(configHolder.getBootStrapConfig());

        // Delegate to the normal BootStrapModule but use different Db and AppConfig modules
        bootStrapModule = new BootStrapModule(
                config,
                null,
                configHolder,
                DbTestModule::new,
                AppConfigTestModule::new);
    }

//    public static BootstrapTestModule create() {
//        final ConfigHolder configHolder = new ConfigHolderImpl();
//        final Config config = new Config(configHolder.getBootStrapConfig());
//
//        return new BootstrapTestModule(config, configHolder);
//    }

    @Override
    protected void configure() {
        super.configure();

        install(bootStrapModule);
//        bootStrapModule.configure();
//
//        bind(Config.class).toInstance(config);
//
//        install(new AppConfigTestModule(configHolder));
//
//        install(new DbTestModule());
//        install(new DbConnectionsModule());
//
//        // Any DAO/Service modules that we must have
//        install(new GlobalConfigBootstrapModule());
//        install(new GlobalConfigDaoModule());
//        install(new DirProvidersModule());
    }

    private static class ConfigHolderImpl implements ConfigHolder {

        private final Config config;
        private final AppConfig appConfig;
        private final Path path;

        ConfigHolderImpl() {
            try {
                final Path dir = Files.createTempDirectory("stroom");
                this.path = dir.resolve("test.yml");

                this.appConfig = new AppConfig();
                appConfig.getPathConfig().setTemp(FileUtil.getCanonicalPath(dir));
                appConfig.getPathConfig().setHome(FileUtil.getCanonicalPath(dir));

                this.config = new Config();
                this.config.setYamlAppConfig(appConfig);
            } catch (final IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
        }

        @Override
        public AppConfig getBootStrapConfig() {
            return appConfig;
        }

        @Override
        public Path getConfigFile() {
            return path;
        }

        public Config getConfig() {
            return config;
        }
    }
}
