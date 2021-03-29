package io.slgl.api.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.model.AuditorEntity;
import io.slgl.api.model.TemplateEntity;
import io.slgl.api.observer.model.ObserverEntity;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.repository.NodeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.slgl.api.camouflage.service.CamouflageHelper.extractCamouflageData;
import static io.slgl.api.camouflage.service.CamouflageHelper.isNodeCamouflaged;

public class LinksGetter {

    private NodeRepository nodeRepository = ExecutionContext.get(NodeRepository.class);
    private StateService stateService = ExecutionContext.get(StateService.class);

    public List<TemplateEntity> getTemplates(NodeEntity node) {
        return getLinks(node, "#templates", TemplateEntity.class);
    }

    public List<ObserverEntity> getObservers(NodeEntity node) {
        return getLinks(node, "#observers", ObserverEntity.class);
    }

    public List<AuditorEntity> getAuditors(NodeEntity node) {
        return getLinks(node, "#auditors", AuditorEntity.class);
    }

    private <T> List<T> getLinks(NodeEntity node, String anchor, Class<T> modelClass) {

        List<T> inlineLinks = stateService.getInlineLinks(node, anchor, modelClass);

        List<NodeEntity> linkedNodes = nodeRepository.readAllLinkedToNode(node.getId(), getExternalAnchorId(node, anchor));
        List<T> normalLinks = linkedNodes.stream()
                .flatMap(linkedEntry -> stateService.getState(linkedEntry, modelClass).stream())
                .collect(Collectors.toList());

        List<T> allLinks = new ArrayList<>(inlineLinks.size() + linkedNodes.size());
        allLinks.addAll(inlineLinks);
        allLinks.addAll(normalLinks);

        return allLinks;
    }

    private String getExternalAnchorId(NodeEntity node, String anchor) {
        if (isNodeCamouflaged(node)) {
            return extractCamouflageData(stateService.getStateMap(node))
                .map(camouflageData -> camouflageData.getCamouflagedAnchor(anchor))
                .orElse(anchor);
        }
        return anchor;
    }
}
