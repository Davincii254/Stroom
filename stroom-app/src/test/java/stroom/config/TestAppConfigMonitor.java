package stroom.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.config.app.AppConfig;
import stroom.config.app.ConfigLocation;
import stroom.config.app.YamlUtil;
import stroom.config.global.impl.AppConfigMonitor;
import stroom.config.global.impl.ConfigMapper;
import stroom.config.global.impl.GlobalConfigService;
import stroom.config.global.impl.validation.ConfigValidator;
import stroom.test.AbstractCoreIntegrationTest;
import stroom.util.io.FileUtil;

import javax.inject.Inject;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

class TestAppConfigMonitor extends AbstractCoreIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAppConfigMonitor.class);

    private final Path tmpDir = FileUtil.createTempDir(this.getClass().getSimpleName());

    @Inject
    private Validator validator;
    @Inject
    private GlobalConfigService globalConfigService;

    @AfterEach
    void afterEach() throws IOException {
        FileUtil.deleteContents(tmpDir);
        Files.deleteIfExists(tmpDir);
    }

    @Test
    void test() throws Exception {

        final String newPathValue = "XXXYYYZZZ";
        final Path devYamlFile = Paths.get(System.getProperty("user.dir"))
                .getParent()
                .resolve("stroom-app")
                .resolve("dev.yml");

        final Path devYamlCopyPath = tmpDir.resolve(devYamlFile.getFileName());

        // Make a copy of dev.yml so we can hack about with it
        Files.copy(devYamlFile, devYamlCopyPath);

        final Pattern pattern = Pattern.compile("temp:\\s*\"[^\"]+\"");
        final String devYamlStr = Files.readString(devYamlCopyPath);
        final Optional<MatchResult> optMatcher = pattern.matcher(devYamlStr)
                .results()
                .findFirst();

        final Runnable grepFile = () -> {
            try {
                String str = pattern.matcher(Files.readString(devYamlCopyPath))
                        .results()
                        .findFirst()
                        .orElseThrow()
                        .group(0);
                LOGGER.debug("Found str {}", str);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        };

        Assertions.assertThat(optMatcher).isPresent();

        // We need to craft our own instances of these classes rather than use guice
        // so that we can use our own config file
        final AppConfig appConfig = YamlUtil.readAppConfig(devYamlCopyPath);
        final ConfigMapper configMapper = new ConfigMapper(appConfig);
        final ConfigLocation configLocation = new ConfigLocation(devYamlCopyPath);
        final ConfigValidator configValidator = new ConfigValidator(validator);

        Assertions.assertThat(appConfig.getPathConfig().getTemp())
                .isNotEqualTo(newPathValue);

        final AppConfigMonitor appConfigMonitor = new AppConfigMonitor(
            appConfig, configLocation, configMapper, configValidator, globalConfigService);

        // start watching the file for changes
        appConfigMonitor.start();

        // Update the config file
        final String updatedDevYamlStr = pattern.matcher(devYamlStr)
            .replaceAll("temp: \"" + newPathValue + "\"");

        // Ensure the replace worked
        Assertions.assertThat(updatedDevYamlStr).isNotEqualTo(devYamlStr);
        Files.writeString(devYamlCopyPath, updatedDevYamlStr);
        LOGGER.debug("Modified file {}", devYamlCopyPath.toAbsolutePath());

        Instant startTime = Instant.now();
        Instant timeOutTime = startTime.plusSeconds(10);
        while (!appConfig.getPathConfig().getTemp().equals(newPathValue)
            && Instant.now().isBefore(timeOutTime)) {
            LOGGER.debug("value {}", appConfig.getPathConfig().getTemp());
            grepFile.run();
            Thread.sleep(200);
        }

        Assertions.assertThat(appConfig.getPathConfig().getTemp())
                .isEqualTo(newPathValue);

        appConfigMonitor.stop();
    }
}