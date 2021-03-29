package io.slgl.setup.actions;

import io.slgl.api.ExecutionContext;
import io.slgl.api.repository.TransactionManager;
import io.slgl.setup.utils.QldbService;
import io.slgl.setup.utils.QldbTableMetadata;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QldbLinkIndexTableSetup {

    private static final String TABLE_NAME = "link_index";

    private final QldbService qldb = ExecutionContext.get(QldbService.class);
    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);

    public void execute() {
        transactionManager.executeInTransaction(tx -> {
            QldbTableMetadata tableMetadata = qldb.createTableIfNotExists(tx, TABLE_NAME);

            qldb.createIndexIfNotExists(tx, tableMetadata, TABLE_NAME, "sn_tn_ta");
            qldb.createIndexIfNotExists(tx, tableMetadata, TABLE_NAME, "sn_ta");
            qldb.createIndexIfNotExists(tx, tableMetadata, TABLE_NAME, "tn_ta");
            qldb.createIndexIfNotExists(tx, tableMetadata, TABLE_NAME, "tn_ta_first");
            qldb.createIndexIfNotExists(tx, tableMetadata, TABLE_NAME, "tn_ta_last");
        });

        qldb.waitForTableToBecomeVisible(TABLE_NAME);
    }
}
