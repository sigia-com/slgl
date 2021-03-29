package io.slgl.api.repository;

import com.amazon.ion.IonValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import io.slgl.api.ExecutionContext;
import io.slgl.api.utils.UncheckedIonValueMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.qldb.Result;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.slgl.api.utils.RepositoryUtils.buildIndexValue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class LinkRepository {

    private final UncheckedIonValueMapper mapper = ExecutionContext.get(UncheckedIonValueMapper.class);
    private final TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);

    private final LinkIndexRepository linkIndexRepository = ExecutionContext.get(LinkIndexRepository.class);

    public void write(LinkEntity link) {
        checkArgument(isNotBlank(link.getSourceNode()));
        checkArgument(isNotBlank(link.getTargetNode()));
        checkArgument(isNotBlank(link.getTargetAnchor()));

        String query = "INSERT INTO link ?";
        List<IonValue> params = ImmutableList.of(
                mapper.serialize(link));

        Result result = transactionManager.getCurrentTransaction().execute(query, params);

        QldbResult qldbResult = mapper.parse(result.iterator().next(), QldbResult.class);
        link.setId(qldbResult.getDocumentId());

        linkIndexRepository.insert(link);
    }

    public void delete(LinkEntity link) {
        checkNotNull(link);

        String query = "DELETE FROM link BY link_id" +
                " WHERE link_id = ?";
        List<IonValue> params = ImmutableList.of(
                mapper.serialize(link.getId()));

        transactionManager.getCurrentTransaction().execute(query, params);

        linkIndexRepository.delete(link);
    }

    public LinkEntity readById(String id) {
        checkNotNull(id);

        String query = "SELECT link_id AS id, link.*" +
                " FROM link BY link_id" +
                " WHERE link_id = ?";

        return readOne(query, id);
    }

    public LinkEntity read(String sourceNode, String targetNode, String targetAnchor) {
        checkNotNull(sourceNode);
        checkNotNull(targetNode);
        checkNotNull(targetAnchor);

        String query = "SELECT link_id AS id, link.*" +
                " FROM link_index" +
                " INNER JOIN link BY link_id ON link_id = link_index.link_id" +
                " WHERE link_index.sn_tn_ta = ?" +
                " AND link.source_node = ?" +
                " AND link.target_node = ?" +
                " AND link.target_anchor = ?";

        return readOne(query, buildIndexValue(sourceNode, targetNode, targetAnchor), sourceNode, targetNode, targetAnchor);
    }

    public List<LinkEntity> readAllBySourceNodeAndTargetAnchor(String sourceNode, String targetAnchor) {
        checkNotNull(sourceNode);
        checkNotNull(targetAnchor);

        String query = "SELECT link_id AS id, link.*" +
                " FROM link_index" +
                " INNER JOIN link BY link_id ON link_id = link_index.link_id" +
                " WHERE link_index.sn_ta = ?" +
                " AND link.source_node = ?" +
                " AND link.target_anchor = ?";

        return readAll(query, buildIndexValue(sourceNode, targetAnchor), sourceNode, targetAnchor);
    }

    public List<LinkEntity> readAllByTargetNodeAndTargetAnchor(String targetNode, String targetAnchor) {
        checkNotNull(targetNode);
        checkNotNull(targetAnchor);

        String query = "SELECT link_id AS id, link.*" +
                " FROM link_index" +
                " INNER JOIN link BY link_id ON link_id = link_index.link_id" +
                " WHERE link_index.tn_ta = ?" +
                " AND link.target_node = ?" +
                " AND link.target_anchor = ?";

        return readAll(query, buildIndexValue(targetNode, targetAnchor), targetNode, targetAnchor);
    }

    public LinkEntity readFirstByTargetNodeAndTargetAnchor(String targetNode, String targetAnchor) {
        checkNotNull(targetNode);
        checkNotNull(targetAnchor);

        String query = "SELECT link_id AS id, link.*" +
                " FROM link_index" +
                " INNER JOIN link BY link_id ON link_id = link_index.link_id" +
                " WHERE link_index.tn_ta_first = ?" +
                " AND link.target_node = ?" +
                " AND link.target_anchor = ?";

        return readOne(query, buildIndexValue(targetNode, targetAnchor), targetNode, targetAnchor);
    }

    public LinkEntity readLastByTargetNodeAndTargetAnchor(String targetNode, String targetAnchor) {
        checkNotNull(targetNode);
        checkNotNull(targetAnchor);

        String query = "SELECT link_id AS id, link.*" +
                " FROM link_index" +
                " INNER JOIN link BY link_id ON link_id = link_index.link_id" +
                " WHERE link_index.tn_ta_last = ?" +
                " AND link.target_node = ?" +
                " AND link.target_anchor = ?";

        return readOne(query, buildIndexValue(targetNode, targetAnchor), targetNode, targetAnchor);
    }

    private List<LinkEntity> readAll(String query, Object... params) {
        return transactionManager.ensureReadTransaction(tx -> {
            Result result = tx.execute(query, convertToParamsList(params));

            return Streams.stream(result)
                    .map(value -> mapper.parse(value, LinkEntity.class))
                    .collect(Collectors.toList());
        });
    }

    private LinkEntity readOne(String query, Object... params) {
        return transactionManager.ensureReadTransaction(tx -> {
            Result result = tx.execute(query, convertToParamsList(params));

            if (result.isEmpty()) {
                return null;
            }

            IonValue entryValue = result.iterator().next();
            return mapper.parse(entryValue, LinkEntity.class);
        });
    }

    private List<IonValue> convertToParamsList(Object... params) {
        return Arrays.stream(params)
                .map(param -> mapper.serialize(param))
                .collect(Collectors.toList());
    }
}
