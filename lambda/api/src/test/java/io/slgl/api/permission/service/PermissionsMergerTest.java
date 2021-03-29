package io.slgl.api.permission.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.model.PermissionEntity.Allow;
import io.slgl.api.model.PermissionEntity.Requirement;
import io.slgl.api.model.PermissionEntity.Requirements;
import io.slgl.client.jsonlogic.And;
import io.slgl.client.jsonlogic.Equal;
import io.slgl.client.jsonlogic.Var;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.slgl.api.model.PermissionEntity.AllowAction.ALL;
import static org.assertj.core.api.Assertions.assertThat;

class PermissionsMergerTest {

    private PermissionsMerger permissionsMerger = new PermissionsMerger();

    @Test
    public void shouldReturnInputPermissionWhenThereIsNoExtendingPermissions() {
        // given
        PermissionEntity permission = new PermissionEntity()
                .setId("example-id");

        List<PermissionEntity> permissions = ImmutableList.of(permission);

        // when
        List<PermissionEntity> result = permissionsMerger.mergeExtendingPermissions(permissions);

        // then
        assertThat(result).isEqualTo(permissions);
    }

    @Test
    public void shouldMergeRequirementsFromExtendingPermissions() {
        // given
        Map<String, Requirement> baseRequirements = ImmutableMap.of("value", new Requirement().setValue("base-requirement"));
        Map<String, Requirement> extendingRequirements = ImmutableMap.of("value", new Requirement().setValue("extending-requirement"));

        PermissionEntity basePermission = new PermissionEntity()
                .setId("example-id")
                .setAllow(ImmutableList.of(new Allow().setAction(ALL)))
                .setEvaluateStateAccessAsUser("example-user")
                .setRequire(Requirements.simple(baseRequirements));

        PermissionEntity extendingPermission = new PermissionEntity()
                .setExtendsId("example-id")
                .setRequire(Requirements.simple(extendingRequirements));

        List<PermissionEntity> permissions = ImmutableList.of(basePermission, extendingPermission);

        // when
        List<PermissionEntity> result = permissionsMerger.mergeExtendingPermissions(permissions);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(basePermission.getId());
        assertThat(result.get(0).getAllow()).isEqualTo(basePermission.getAllow());
        assertThat(result.get(0).getEvaluateStateAccessAsUser()).isEqualTo(basePermission.getEvaluateStateAccessAsUser());

        assertThat(result.get(0).getRequire().getRequirements())
                .isEqualTo(ImmutableList.of(baseRequirements, extendingRequirements));
    }

    @Test
    public void shouldMergeRequireLogicFromExtendingPermissions() {
        // given
        Object baseRequireLogic = new Equal(new Var("value"), "base-requirement");
        Object extendingRequireLogic = new Equal(new Var("value"), "extending-requirement");

        PermissionEntity basePermission = new PermissionEntity()
                .setId("example-id")
                .setAllow(ImmutableList.of(new Allow().setAction(ALL)))
                .setEvaluateStateAccessAsUser("example-user")
                .setRequireLogic(baseRequireLogic);

        PermissionEntity extendingPermission = new PermissionEntity()
                .setExtendsId("example-id")
                .setRequireLogic(extendingRequireLogic);

        List<PermissionEntity> permissions = ImmutableList.of(basePermission, extendingPermission);

        // when
        List<PermissionEntity> result = permissionsMerger.mergeExtendingPermissions(permissions);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(basePermission.getId());
        assertThat(result.get(0).getAllow()).isEqualTo(basePermission.getAllow());
        assertThat(result.get(0).getEvaluateStateAccessAsUser()).isEqualTo(basePermission.getEvaluateStateAccessAsUser());

        assertThat(result.get(0).getRequireLogic())
                .isEqualTo(new And(baseRequireLogic, extendingRequireLogic));
    }
}