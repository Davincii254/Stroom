/*
 * This file is generated by jOOQ.
 */
package stroom.proxy.repo.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import stroom.proxy.repo.db.jooq.DefaultSchema;
import stroom.proxy.repo.db.jooq.Keys;
import stroom.proxy.repo.db.jooq.tables.records.ForwardAggregateRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ForwardAggregate extends TableImpl<ForwardAggregateRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>forward_aggregate</code>
     */
    public static final ForwardAggregate FORWARD_AGGREGATE = new ForwardAggregate();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ForwardAggregateRecord> getRecordType() {
        return ForwardAggregateRecord.class;
    }

    /**
     * The column <code>forward_aggregate.id</code>.
     */
    public final TableField<ForwardAggregateRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>forward_aggregate.fk_forward_url_id</code>.
     */
    public final TableField<ForwardAggregateRecord, Integer> FK_FORWARD_URL_ID = createField(DSL.name("fk_forward_url_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>forward_aggregate.fk_aggregate_id</code>.
     */
    public final TableField<ForwardAggregateRecord, Integer> FK_AGGREGATE_ID = createField(DSL.name("fk_aggregate_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>forward_aggregate.success</code>.
     */
    public final TableField<ForwardAggregateRecord, Boolean> SUCCESS = createField(DSL.name("success"), SQLDataType.BOOLEAN, this, "");

    private ForwardAggregate(Name alias, Table<ForwardAggregateRecord> aliased) {
        this(alias, aliased, null);
    }

    private ForwardAggregate(Name alias, Table<ForwardAggregateRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>forward_aggregate</code> table reference
     */
    public ForwardAggregate(String alias) {
        this(DSL.name(alias), FORWARD_AGGREGATE);
    }

    /**
     * Create an aliased <code>forward_aggregate</code> table reference
     */
    public ForwardAggregate(Name alias) {
        this(alias, FORWARD_AGGREGATE);
    }

    /**
     * Create a <code>forward_aggregate</code> table reference
     */
    public ForwardAggregate() {
        this(DSL.name("forward_aggregate"), null);
    }

    public <O extends Record> ForwardAggregate(Table<O> child, ForeignKey<O, ForwardAggregateRecord> key) {
        super(child, key, FORWARD_AGGREGATE);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public UniqueKey<ForwardAggregateRecord> getPrimaryKey() {
        return Keys.PK_FORWARD_AGGREGATE;
    }

    @Override
    public List<UniqueKey<ForwardAggregateRecord>> getKeys() {
        return Arrays.<UniqueKey<ForwardAggregateRecord>>asList(Keys.PK_FORWARD_AGGREGATE);
    }

    @Override
    public List<ForeignKey<ForwardAggregateRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<ForwardAggregateRecord, ?>>asList(Keys.FK_FORWARD_AGGREGATE_FORWARD_URL_1, Keys.FK_FORWARD_AGGREGATE_AGGREGATE_1);
    }

    private transient ForwardUrl _forwardUrl;
    private transient Aggregate _aggregate;

    public ForwardUrl forwardUrl() {
        if (_forwardUrl == null)
            _forwardUrl = new ForwardUrl(this, Keys.FK_FORWARD_AGGREGATE_FORWARD_URL_1);

        return _forwardUrl;
    }

    public Aggregate aggregate() {
        if (_aggregate == null)
            _aggregate = new Aggregate(this, Keys.FK_FORWARD_AGGREGATE_AGGREGATE_1);

        return _aggregate;
    }

    @Override
    public ForwardAggregate as(String alias) {
        return new ForwardAggregate(DSL.name(alias), this);
    }

    @Override
    public ForwardAggregate as(Name alias) {
        return new ForwardAggregate(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ForwardAggregate rename(String name) {
        return new ForwardAggregate(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ForwardAggregate rename(Name name) {
        return new ForwardAggregate(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, Integer, Integer, Boolean> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
