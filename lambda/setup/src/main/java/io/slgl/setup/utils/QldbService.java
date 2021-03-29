package io.slgl.setup.utils;

import com.amazon.ion.IonValue;
import com.google.common.base.Objects;
import io.slgl.api.ExecutionContext;
import io.slgl.api.repository.TransactionManager;
import io.slgl.api.utils.UncheckedIonValueMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.qldb.Executable;
import software.amazon.qldb.Result;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@Slf4j
public class QldbService {

    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);
    private final UncheckedIonValueMapper mapper = ExecutionContext.get(UncheckedIonValueMapper.class);

    public void waitForTableToBecomeVisible(String tableName) {
        LocalTime timeout = LocalTime.now().plus(30, ChronoUnit.SECONDS);
        boolean found = false;
        int tries = 0;
        while (!found) {
            if (LocalTime.now().isAfter(timeout)) {
                throw new IllegalStateException(tableName + " not created within 30 seconds in " + tries + " tries");
            }

            tries++;
            found = ExecutionContext.get(TransactionManager.class).executeInReadTransaction(tx -> {
                Result result = tx.execute("SELECT name FROM information_schema.user_tables WHERE name = '" + tableName + "'");
                return !result.isEmpty();
            });

            log.info("Table found {} after {} tries: {}", tableName, tries, found);
        }
    }

    public QldbTableMetadata createTableIfNotExists(Executable tx, String tableName) {
        Optional<QldbTableMetadata> existingMetadata = Optional.ofNullable(getTableMetadata(tableName));
        return existingMetadata.orElseGet(() -> {
            log.info("Creating {} table", tableName);
            tx.execute(format("CREATE TABLE %s", tableName));
            QldbTableMetadata metadata = getTableMetadata(tableName);
            return requireNonNull(metadata, tableName + " table metadata should be present after creation");
        });
    }

    public void createIndexIfNotExists(Executable tx, QldbTableMetadata tableMetadata, String tableName, String column) {
        if (tableMetadata.getIndexes() != null) {
            for (QldbIndexMetadata index : tableMetadata.getIndexes()) {
                if (Objects.equal(index.getExpr(), "[" + column + "]")) {
                    log.info("Index  [{}] already exists in {} table", column, tableName);
                    return;
                }
            }
        }

        String query = format("CREATE INDEX ON %s (\"%s\")", tableName, column);
        log.info("Creating index in `{}` table for column `{}` with query: \n\t{}", tableName, column, query);
        tx.execute(query);
    }

    public QldbTableMetadata getTableMetadata(String tableName) {
        String query = "SELECT * FROM information_schema.user_tables WHERE name = '" + tableName + "'";

        Result result = transactionManager.getCurrentTransaction().execute(query);

        if (result.isEmpty()) {
            log.info("No table metadata for " + tableName + " table");
            return null;
        }

        IonValue entryValue = result.iterator().next();
        return mapper.parse(entryValue, QldbTableMetadata.class);
    }
}
