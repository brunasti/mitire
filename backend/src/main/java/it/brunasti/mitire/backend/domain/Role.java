package it.brunasti.mitire.backend.domain;

public enum Role {
    ADMIN(3),
    MEMBER(2),
    VIEWER(1);

    private final int weight;

    Role(int weight) {
        this.weight = weight;
    }

    public boolean isAtLeast(Role other) {
        return this.weight >= other.weight;
    }

    /**
     * The role a user effectively holds when operating on a project: the most privileged of
     * their own intrinsic role and the roles of any of their groups that grant access to the
     * project. Null means the user has no access to the project at all (not ADMIN, and not a
     * member of any group linked to it).
     */
    public static Role effectiveFor(User user, Project project) {
        if (user.getRole() == ADMIN) {
            return ADMIN;
        }
        Role bestGroupRole = null;
        for (Group group : user.getGroups()) {
            if (group.getProjects().contains(project)
                    && (bestGroupRole == null || group.getRole().weight > bestGroupRole.weight)) {
                bestGroupRole = group.getRole();
            }
        }
        if (bestGroupRole == null) {
            return null;
        }
        return user.getRole().weight > bestGroupRole.weight ? user.getRole() : bestGroupRole;
    }
}
