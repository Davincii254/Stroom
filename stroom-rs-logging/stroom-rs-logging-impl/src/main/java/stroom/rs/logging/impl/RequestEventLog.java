package stroom.rs.logging.impl;

public interface RequestEventLog {
    void log (LoggingInfo info, Object requestEntity, Object responseEntity);
}
