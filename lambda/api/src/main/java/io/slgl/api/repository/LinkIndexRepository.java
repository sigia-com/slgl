package io.slgl.api.repository;

import com.amazon.ion.IonValue;
import com.google.common.collect.ImmutableList;
import io.slgl.api.ExecutionContext;
import io.slgl.api.utils.UncheckedIonValueMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.qldb.Result;

import java.util.ArrayList;
import java.util.List;

import static io.slgl.api.utils.RepositoryUtils.buildIndexValue;

@Slf4j
public class LinkIndexRepository {

    private UncheckedIonValueMapper mapper = ExecutionContext.get(UncheckedIonValueMapper.class);
    private TransactionManager transactionManager = ExecutionContext.get(TransactionManager.class);

    public void insert(LinkEntity link) {
        LinkIndexEntity linkIndex = new LinkIndexEntity()
                .setLinkId(link.getId())
                .setSourceNodeTargetNodeTargetAnchor(buildIndexValue(link.getSourceNode(), link.getTargetNode(), link.getTargetAnchor()))
                .setSourceNodeTargetAnchor(buildIndexValue(link.getSourceNode(), link.getTargetAnchor()))
                .setTargetNodeTargetAnchor(buildIndexValue(link.getTargetNode(), link.getTargetAnchor()));

        linkIndex.setTargetNodeTargetAnchorLast(linkIndex.getTargetNodeTargetAnchor());

        LinkIndexEntity last = findLast(linkIndex.getTargetNodeTargetAnchor());

        if (last != null) {
            linkIndex.setTargetNodeTargetAnchorPreviousId(last.getId());
        } else {
            linkIndex.setTargetNodeTargetAnchorFirst(linkIndex.getTargetNodeTargetAnchor());
        }

        String query = "INSERT INTO link_index ?";
        List<IonValue> params = ImmutableList.of(
                mapper.serialize(linkIndex));

        Result result = transactionManager.getCurrentTransaction().execute(query, params);

        QldbResult qldbResult = mapper.parse(result.iterator().next(), QldbResult.class);
        linkIndex.setId(qldbResult.getDocumentId());

        if (last != null) {
            clearLastAndSetNextId(last, linkIndex.getId());
        }
    }

    private LinkIndexEntity findLast(String targetNodeTargetAnchor) {
        String query = "SELECT *" +
                " FROM link_index BY id" +
                " WHERE tn_ta_last = ?";

        List<IonValue> params = ImmutableList.of(
                mapper.serialize(targetNodeTargetAnchor));

        Result result = transactionManager.getCurrentTransaction().execute(query, params);

        if (result.isEmpty()) {
            return null;
        }

        IonValue entryValue = result.iterator().next();
        return mapper.parse(entryValue, LinkIndexEntity.class);
    }

    private void clearLastAndSetNextId(LinkIndexEntity entity, String nextId) {
        String query = "UPDATE link_index BY id" +
                " SET tn_ta_last = NULL, tn_ta_next_id = ?" +
                " WHERE id = ?";

        List<IonValue> params = ImmutableList.of(
                mapper.serialize(nextId),
                mapper.serialize(entity.getId()));

        transactionManager.getCurrentTransaction().execute(query, params);
    }

    public void delete(LinkEntity link) {
        LinkIndexEntity linkIndex = findByLink(link);

        String first = linkIndex.getTargetNodeTargetAnchorFirst();
        String last = linkIndex.getTargetNodeTargetAnchorLast();
        String nextId = linkIndex.getTargetNodeTargetAnchorNextId();
        String previousId = linkIndex.getTargetNodeTargetAnchorPreviousId();

        if (nextId != null) {
            updateFirstAndPreviousId(nextId, first, previousId);
        }
        if (previousId != null) {
            updateLastAndNextId(previousId, last, nextId);
        }
    }

    private LinkIndexEntity findByLink(LinkEntity link) {
        // we could query using 'link_id' but there is no index on that column
        String query = "SELECT *" +
                " FROM link_index BY id" +
                " WHERE sn_tn_ta = ?";

        String indexValue = buildIndexValue(link.getSourceNode(), link.getTargetNode(), link.getTargetAnchor());
        List<IonValue> params = ImmutableList.of(
                mapper.serialize(indexValue));

        Result result = transactionManager.getCurrentTransaction().execute(query, params);
        if (result.isEmpty()) {
            throw new RuntimeException("Unexpected error: unable to find link index for link: id=" + link.getId() + " sn_tn_ta=" + indexValue);
        }

        IonValue entryValue = result.iterator().next();
        return mapper.parse(entryValue, LinkIndexEntity.class);
    }

    private LinkIndexEntity findById(String id) {
        String query = "SELECT *" +
                " FROM link_index BY id" +
                " WHERE id = ?";

        List<IonValue> params = ImmutableList.of(
                mapper.serialize(id));

        Result result = transactionManager.getCurrentTransaction().execute(query, params);
        if (result.isEmpty()) {
            throw new RuntimeException("Unexpected error: unable to find link index: id=" + id);
        }

        IonValue entryValue = result.iterator().next();
        return mapper.parse(entryValue, LinkIndexEntity.class);
    }

    private void updateLastAndNextId(String id, String last, String nextId) {
        String query = "UPDATE link_index BY id" +
                " SET tn_ta_last = " + (last != null ? "?" : "NULL") + "," +
                " tn_ta_next_id = " + (nextId != null ? "?" : "NULL") +
                " WHERE id = ?";

        List<IonValue> params = new ArrayList<>();
        if (last != null) {
            params.add(mapper.serialize(last));
        }
        if (nextId != null) {
            params.add(mapper.serialize(nextId));
        }
        params.add(mapper.serialize(id));

        transactionManager.getCurrentTransaction().execute(query, params);
    }

    private void updateFirstAndPreviousId(String id, String first, String previousId) {
        String query = "UPDATE link_index BY id" +
                " SET tn_ta_first = " + (first != null ? "?" : "NULL") + "," +
                " tn_ta_previous_id = " + (previousId != null ? "?" : "NULL") +
                " WHERE id = ?";

        List<IonValue> params = new ArrayList<>();
        if (first != null) {
            params.add(mapper.serialize(first));
        }
        if (previousId != null) {
            params.add(mapper.serialize(previousId));
        }
        params.add(mapper.serialize(id));

        transactionManager.getCurrentTransaction().execute(query, params);
    }
}
