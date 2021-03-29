package io.slgl.setup.actions;

import io.slgl.api.ExecutionContext;
import io.slgl.api.repository.TransactionManager;
import io.slgl.setup.utils.QldbService;
import io.slgl.setup.utils.QldbTableMetadata;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QldbLinkTableSetup {

    private static final String TABLE_NAME = "link";

    private final QldbService qldb = ExecutionContext.get(QldbService.class);
    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);

    public void execute() {
        transactionManager.executeInTransaction(tx -> {
            QldbTableMetadata tableMetadata = qldb.createTableIfNotExists(tx, TABLE_NAME);
        });

        qldb.waitForTableToBecomeVisible(TABLE_NAME);
    }
}
