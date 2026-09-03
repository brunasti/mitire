package it.brunasti.mitire.backend.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void effectiveForReturnsAdminWhenUserIsGloballyAdminRegardlessOfGroups() {
        Project project = new Project();
        project.setId(1L);

        User admin = new User();
        admin.setRole(Role.ADMIN);

        assertThat(Role.effectiveFor(admin, project)).isEqualTo(Role.ADMIN);
    }

    @Test
    void effectiveForReturnsNullWhenUserHasNoQualifyingGroup() {
        Project project = new Project();
        project.setId(1L);

        User user = new User();
        user.setRole(Role.MEMBER);

        assertThat(Role.effectiveFor(user, project)).isNull();
    }

    @Test
    void effectiveForElevatesViewerToGroupRole() {
        Project project = new Project();
        project.setId(1L);

        Group group = new Group();
        group.setRole(Role.MEMBER);
        group.setProjects(Set.of(project));

        User viewer = new User();
        viewer.setRole(Role.VIEWER);
        viewer.setGroups(Set.of(group));

        assertThat(Role.effectiveFor(viewer, project)).isEqualTo(Role.MEMBER);
    }

    @Test
    void effectiveForElevatesViewerToAdminViaAdminRoleGroup() {
        Project project = new Project();
        project.setId(1L);

        Group group = new Group();
        group.setRole(Role.ADMIN);
        group.setProjects(Set.of(project));

        User viewer = new User();
        viewer.setRole(Role.VIEWER);
        viewer.setGroups(Set.of(group));

        assertThat(Role.effectiveFor(viewer, project)).isEqualTo(Role.ADMIN);
    }

    @Test
    void effectiveForKeepsIntrinsicRoleWhenMoreExtensiveThanGroupRole() {
        Project project = new Project();
        project.setId(1L);

        Group group = new Group();
        group.setRole(Role.VIEWER);
        group.setProjects(Set.of(project));

        User member = new User();
        member.setRole(Role.MEMBER);
        member.setGroups(Set.of(group));

        assertThat(Role.effectiveFor(member, project)).isEqualTo(Role.MEMBER);
    }

    @Test
    void effectiveForOnlyConsidersGroupsLinkedToTheProject() {
        Project project = new Project();
        project.setId(1L);
        Project otherProject = new Project();
        otherProject.setId(2L);

        Group group = new Group();
        group.setRole(Role.ADMIN);
        group.setProjects(Set.of(otherProject));

        User viewer = new User();
        viewer.setRole(Role.VIEWER);
        viewer.setGroups(Set.of(group));

        assertThat(Role.effectiveFor(viewer, project)).isNull();
    }
}
