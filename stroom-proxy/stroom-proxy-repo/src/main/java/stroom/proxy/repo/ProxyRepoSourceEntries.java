package stroom.proxy.repo;

import stroom.data.zip.StroomZipFileType;
import stroom.db.util.JooqUtil;
import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.api.StandardHeaderArguments;
import stroom.proxy.repo.db.jooq.tables.records.SourceItemRecord;
import stroom.util.io.FileUtil;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.inject.Inject;

import static stroom.proxy.repo.db.jooq.tables.Source.SOURCE;
import static stroom.proxy.repo.db.jooq.tables.SourceEntry.SOURCE_ENTRY;
import static stroom.proxy.repo.db.jooq.tables.SourceItem.SOURCE_ITEM;

public class ProxyRepoSourceEntries {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRepoSourceEntries.class);

    private final ProxyRepoDbConnProvider connProvider;
    private final ErrorReceiver errorReceiver;
    private final Path repoDir;

    @Inject
    public ProxyRepoSourceEntries(final ProxyRepoDbConnProvider connProvider,
                                  final ErrorReceiver errorReceiver,
                                  final ProxyRepoConfig proxyRepoConfig) {
        this.connProvider = connProvider;
        this.errorReceiver = errorReceiver;
        repoDir = Paths.get(proxyRepoConfig.getRepoDir());



    }

    public void examine() {
        JooqUtil.context(connProvider, context -> {
            try (final Stream<Record2<Integer, String>> stream = context
                    .select(SOURCE.ID, SOURCE.PATH)
                    .from(SOURCE)
                    .where(SOURCE.EXAMINED.isFalse())
                    .orderBy(SOURCE.LAST_MODIFIED_TIME_MS, SOURCE.ID)
                    .stream()) {
                stream.forEach(record -> {
                    final int id = record.get(SOURCE.ID);
                    final String path = record.get(SOURCE.PATH);

                    CompletableFuture.runAsync(() -> examine(id, path));
                });
            }
        });
    }

    private void examine(final int sourceId,
                         final String sourcePath) {
        final Path fullPath = repoDir.resolve(sourcePath);
        final AtomicInteger itemNumber = new AtomicInteger();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Storing zip info for  '" + FileUtil.getCanonicalPath(fullPath) + "'");
        }

        // Start a transaction for all of the database changes.
        JooqUtil.transaction(connProvider, context -> {
            try (final ZipFile zipFile = new ZipFile(Files.newByteChannel(fullPath))) {

                final Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
                while (entries.hasMoreElements()) {
                    final ZipArchiveEntry entry = entries.nextElement();

                    // Skip directories
                    if (!entry.isDirectory()) {
                        final String fileName = entry.getName();

                        // Split into stem and extension.
                        int index = fileName.indexOf(".");
                        if (index != -1) {
                            final String dataName = fileName.substring(0, index);
                            final String extension = fileName.substring(index).toLowerCase();

                            // If this is a meta entry then get the feed name.
                            String feedName = null;
                            String typeName = null;

                            int extensionType = -1;
                            if (StroomZipFileType.Meta.getExtension().equals(extension)) {
                                // We need to be able to sort by extension so we can get meta data first.
                                extensionType = 1;

                                try (final InputStream metaStream = zipFile.getInputStream(entry)) {
                                    if (metaStream == null) {
                                        errorReceiver.onError(fullPath, "Unable to find meta?");
                                    } else {
                                        final AttributeMap attributeMap = new AttributeMap();
                                        AttributeMapUtil.read(metaStream, attributeMap);
                                        feedName = attributeMap.get(StandardHeaderArguments.FEED);
                                        typeName = attributeMap.get(StandardHeaderArguments.TYPE);
                                    }
                                } catch (final RuntimeException e) {
                                    errorReceiver.onError(fullPath, e.getMessage());
                                    LOGGER.error(e.getMessage(), e);
                                }
                            } else if (StroomZipFileType.Context.getExtension().equals(extension)) {
                                extensionType = 2;
                            } else if (StroomZipFileType.Data.getExtension().equals(extension)) {
                                extensionType = 3;
                            }

                            // Don't add unknown types.
                            if (extensionType != -1) {
                                final int dataId = addItem(
                                        context,
                                        sourceId,
                                        itemNumber,
                                        dataName,
                                        feedName,
                                        typeName);
                                addItemEntry(context,
                                        dataId,
                                        extension,
                                        extensionType,
                                        entry.getSize());
                            }
                        }
                    }
                }

                // Mark the source as having been examined.
                context
                        .update(SOURCE)
                        .set(SOURCE.EXAMINED, true)
                        .where(SOURCE.ID.eq(sourceId))
                        .execute();

            } catch (final IOException | RuntimeException e) {
                // Unable to open file ... must be bad.
                errorReceiver.onError(fullPath, e.getMessage());
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    private int addItem(final DSLContext context,
                        final int sourceId,
                        final AtomicInteger itemMumber,
                        final String name,
                        final String feedName,
                        final String typeName) {
        final Optional<SourceItemRecord> optional = context
                .selectFrom(SOURCE_ITEM)
                .where(SOURCE_ITEM.FK_SOURCE_ID.eq(sourceId))
                .and(SOURCE_ITEM.NAME.eq(name))
                .fetchOptional();

        if (optional.isPresent()) {
            final SourceItemRecord record = optional.get();
            if ((feedName != null && record.getFeedName() == null) ||
                    (typeName != null && record.getTypeName() == null)) {
                // Update the record with the feed and type name.
                context
                        .update(SOURCE_ITEM)
                        .set(SOURCE_ITEM.FEED_NAME, feedName)
                        .set(SOURCE_ITEM.TYPE_NAME, typeName)
                        .where(SOURCE_ITEM.ID.eq(record.getId()))
                        .execute();
            }

            return record.getId();
        }

        return context
                .insertInto(SOURCE_ITEM,
                        SOURCE_ITEM.FK_SOURCE_ID,
                        SOURCE_ITEM.NUMBER,
                        SOURCE_ITEM.NAME,
                        SOURCE_ITEM.FEED_NAME,
                        SOURCE_ITEM.TYPE_NAME)
                .values(sourceId, itemMumber.incrementAndGet(), name, feedName, typeName)
                .returning(SOURCE_ITEM.ID)
                .fetchOne()
                .getId();
    }

    private int addItemEntry(final DSLContext context,
                             final int dataId,
                             final String extension,
                             final int extensionType,
                             final long size) {
        return context
                .insertInto(SOURCE_ENTRY,
                        SOURCE_ENTRY.FK_SOURCE_ITEM_ID,
                        SOURCE_ENTRY.EXTENSION,
                        SOURCE_ENTRY.EXTENSION_TYPE,
                        SOURCE_ENTRY.BYTE_SIZE)
                .values(dataId, extension, extensionType, size)
                .returning(SOURCE_ENTRY.ID)
                .fetchOne()
                .getId();
    }
}
