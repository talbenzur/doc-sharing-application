package docSharing.entities.file;

import docSharing.entities.permission.Permission;

public enum DocOperation {
    CREATE(null),
    UPDATE(Permission.EDITOR),
    DELETE(Permission.EDITOR),
    EXPORT(Permission.EDITOR),
    IMPORT(null),
    SHARE(Permission.OWNER),
    SET_PARENT(Permission.EDITOR),
    SET_TITLE(Permission.EDITOR),
    JOIN(Permission.VIEWER);


    private final Permission permission;

    DocOperation(Permission permission) {
        this.permission = permission;
    }

    public Permission getPermission() {
        return permission;
    }
}
