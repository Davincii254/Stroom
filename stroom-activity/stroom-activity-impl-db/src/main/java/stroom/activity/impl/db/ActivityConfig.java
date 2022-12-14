package stroom.activity.impl.db;

import stroom.config.common.AbstractDbConfig;
import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.config.common.HasDbConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.shared.BootStrapConfig;
import stroom.util.shared.IsStroomConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder(alphabetic = true)
public class ActivityConfig extends AbstractConfig implements IsStroomConfig, HasDbConfig {

    private final ActivityDbConfig dbConfig;

    public ActivityConfig() {
        dbConfig = new ActivityDbConfig();
    }

    @SuppressWarnings("unused")
    @JsonCreator
    public ActivityConfig(@JsonProperty("db") final ActivityDbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    @JsonProperty("db")
    public ActivityDbConfig getDbConfig() {
        return dbConfig;
    }

    @BootStrapConfig
    public static class ActivityDbConfig extends AbstractDbConfig {

        public ActivityDbConfig() {
            super();
        }

        @JsonCreator
        public ActivityDbConfig(
                @JsonProperty(PROP_NAME_CONNECTION) final ConnectionConfig connectionConfig,
                @JsonProperty(PROP_NAME_CONNECTION_POOL) final ConnectionPoolConfig connectionPoolConfig) {
            super(connectionConfig, connectionPoolConfig);
        }
    }
}
