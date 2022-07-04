/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record7;
import org.jooq.Row7;
import org.jooq.impl.UpdatableRecordImpl;

import stroom.proxy.repo.db.jooq.tables.Aggregate;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AggregateRecord extends UpdatableRecordImpl<AggregateRecord> implements Record7<Long, Long, Long, Long, Integer, Boolean, Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>aggregate.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>aggregate.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>aggregate.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>aggregate.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>aggregate.fk_feed_id</code>.
     */
    public void setFkFeedId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>aggregate.fk_feed_id</code>.
     */
    public Long getFkFeedId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>aggregate.byte_size</code>.
     */
    public void setByteSize(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>aggregate.byte_size</code>.
     */
    public Long getByteSize() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>aggregate.items</code>.
     */
    public void setItems(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>aggregate.items</code>.
     */
    public Integer getItems() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>aggregate.complete</code>.
     */
    public void setComplete(Boolean value) {
        set(5, value);
    }

    /**
     * Getter for <code>aggregate.complete</code>.
     */
    public Boolean getComplete() {
        return (Boolean) get(5);
    }

    /**
     * Setter for <code>aggregate.new_position</code>.
     */
    public void setNewPosition(Long value) {
        set(6, value);
    }

    /**
     * Getter for <code>aggregate.new_position</code>.
     */
    public Long getNewPosition() {
        return (Long) get(6);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record7 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row7<Long, Long, Long, Long, Integer, Boolean, Long> fieldsRow() {
        return (Row7) super.fieldsRow();
    }

    @Override
    public Row7<Long, Long, Long, Long, Integer, Boolean, Long> valuesRow() {
        return (Row7) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return Aggregate.AGGREGATE.ID;
    }

    @Override
    public Field<Long> field2() {
        return Aggregate.AGGREGATE.CREATE_TIME_MS;
    }

    @Override
    public Field<Long> field3() {
        return Aggregate.AGGREGATE.FK_FEED_ID;
    }

    @Override
    public Field<Long> field4() {
        return Aggregate.AGGREGATE.BYTE_SIZE;
    }

    @Override
    public Field<Integer> field5() {
        return Aggregate.AGGREGATE.ITEMS;
    }

    @Override
    public Field<Boolean> field6() {
        return Aggregate.AGGREGATE.COMPLETE;
    }

    @Override
    public Field<Long> field7() {
        return Aggregate.AGGREGATE.NEW_POSITION;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public Long component2() {
        return getCreateTimeMs();
    }

    @Override
    public Long component3() {
        return getFkFeedId();
    }

    @Override
    public Long component4() {
        return getByteSize();
    }

    @Override
    public Integer component5() {
        return getItems();
    }

    @Override
    public Boolean component6() {
        return getComplete();
    }

    @Override
    public Long component7() {
        return getNewPosition();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public Long value2() {
        return getCreateTimeMs();
    }

    @Override
    public Long value3() {
        return getFkFeedId();
    }

    @Override
    public Long value4() {
        return getByteSize();
    }

    @Override
    public Integer value5() {
        return getItems();
    }

    @Override
    public Boolean value6() {
        return getComplete();
    }

    @Override
    public Long value7() {
        return getNewPosition();
    }

    @Override
    public AggregateRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public AggregateRecord value2(Long value) {
        setCreateTimeMs(value);
        return this;
    }

    @Override
    public AggregateRecord value3(Long value) {
        setFkFeedId(value);
        return this;
    }

    @Override
    public AggregateRecord value4(Long value) {
        setByteSize(value);
        return this;
    }

    @Override
    public AggregateRecord value5(Integer value) {
        setItems(value);
        return this;
    }

    @Override
    public AggregateRecord value6(Boolean value) {
        setComplete(value);
        return this;
    }

    @Override
    public AggregateRecord value7(Long value) {
        setNewPosition(value);
        return this;
    }

    @Override
    public AggregateRecord values(Long value1, Long value2, Long value3, Long value4, Integer value5, Boolean value6, Long value7) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AggregateRecord
     */
    public AggregateRecord() {
        super(Aggregate.AGGREGATE);
    }

    /**
     * Create a detached, initialised AggregateRecord
     */
    public AggregateRecord(Long id, Long createTimeMs, Long fkFeedId, Long byteSize, Integer items, Boolean complete, Long newPosition) {
        super(Aggregate.AGGREGATE);

        setId(id);
        setCreateTimeMs(createTimeMs);
        setFkFeedId(fkFeedId);
        setByteSize(byteSize);
        setItems(items);
        setComplete(complete);
        setNewPosition(newPosition);
    }
}
