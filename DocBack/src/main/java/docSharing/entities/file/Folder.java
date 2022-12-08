package docSharing.entities.file;

import docSharing.entities.User;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folder")
public class Folder extends File {
    @ElementCollection
    @Column(name = "sub_files")
    private List<File> subFiles;

    public Folder() {
    }

    public Folder(User owner, int parentId, String title) {
        super(owner, parentId, title);
        this.subFiles = new ArrayList<>();
    }

    public List<File> getSubFiles() {
        return subFiles;
    }

    public void addSubFile(File file) {
        this.subFiles.add(file);
    }

    public void removeSubFile(File file) {
        this.subFiles.remove(file);
    }
}
