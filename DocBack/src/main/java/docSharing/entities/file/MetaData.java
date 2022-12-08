package docSharing.entities.file;

import docSharing.entities.User;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "metadata")
public class MetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "parentId")
    private int parentId;
    @Column(name = "created")
    private final LocalDate created;
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    @Column(name = "title")
    private String title;
    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;
    @OneToOne(mappedBy = "metadata")
    private File file;

    private MetaData() {
        this.created = LocalDate.now();
        this.lastUpdated =  LocalDateTime.now();
    }

    public MetaData(File file, String title, User owner, int parentId) {
        this();
        this.file = file;
        this.title = title;
        this.owner = owner;
        this.parentId = parentId;
    }

    public LocalDate getCreated() {
        return created;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getTitle() {
        return title;
    }

    public User getOwner() {
        return owner;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
}
