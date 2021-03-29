package io.slgl.api.repository;

import com.amazon.ion.IonValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import io.slgl.api.ExecutionContext;
import io.slgl.api.utils.UncheckedIonValueMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.qldb.Result;

import java.util.List;
import java.util.stream.Collectors;

import static io.slgl.api.utils.RepositoryUtils.buildIndexValue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class NodeRepository {

    private UncheckedIonValueMapper mapper = ExecutionContext.get(UncheckedIonValueMapper.class);
    private TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);

    public void write(NodeEntity entry) {
        Preconditions.checkArgument(isNotBlank(entry.getId()));

        String query = "INSERT INTO node ?";
        IonValue entryValue = mapper.serialize(entry);

        transactionManager.getCurrentTransaction().execute(query, ImmutableList.of(entryValue));
    }

    public NodeEntity readById(String id) {
        Preconditions.checkNotNull(id);

        String query = "SELECT * FROM node WHERE \"@id\" = ?";
        IonValue idValue = mapper.serialize(id);

        return transactionManager.ensureReadTransaction(tx -> {
            Result result = tx.execute(query, List.of(idValue));

            if (result.isEmpty()) {
                return null;
            }

            IonValue entryValue = result.iterator().next();
            return mapper.parse(entryValue, NodeEntity.class);
        });
    }

    public List<NodeEntity> readAllLinkedToNode(String nodeId, String anchor) {
        Preconditions.checkNotNull(nodeId);

        String query = "SELECT node.*" +
                        " FROM link_index" +
                        " INNER JOIN link BY link_id ON link_id = link_index.link_id" +
                        " INNER JOIN node ON node.\"@id\" = link.source_node" +
                        " WHERE link_index.tn_ta = ?" +
                        " AND link.target_node = ?" +
                        " AND link.target_anchor = ?";

        List<IonValue> params = ImmutableList.of(
                mapper.serialize(buildIndexValue(nodeId, anchor)),
                mapper.serialize(nodeId),
                mapper.serialize(anchor));

        return transactionManager.ensureReadTransaction(tx -> {
            Result result = tx.execute(query, params);

            return Streams.stream(result)
                    .map(value -> mapper.parse(value, NodeEntity.class))
                    .collect(Collectors.toList());
        });
    }
}