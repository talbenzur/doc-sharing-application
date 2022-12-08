package docSharing.entities.file;

import javax.persistence.*;

import static java.lang.Math.max;

@Entity
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String content;

    @OneToOne(mappedBy = "content")
    private Document document;

    public Content() {
        content = "";
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String append(String content, int start) {
        this.content = this.content.substring(0, start) + content;
        if (this.content.length() >= start + content.length()) {
            this.content += this.content.substring(start + content.length());
        }

        return this.content;
    }

    public String delete(int start, int end) {
        int count = start - end;
        this.content = this.content.substring(0, max(0, start - count)) + this.content.substring(start);

        return this.content;
    }

    public String appendRange(String content, int start, int end) {
        this.content = this.content.substring(0, start) + content
                + this.content.substring(end);

        return this.content;
    }

    public String deleteRange(int start, int end) {
        this.content = this.content.substring(0, start) + this.content.substring(end);

        return this.content;
    }

    @Override
    public String toString() {
        return "Content{" +
                "content='" + content + '\'' +
                '}';
    }
}