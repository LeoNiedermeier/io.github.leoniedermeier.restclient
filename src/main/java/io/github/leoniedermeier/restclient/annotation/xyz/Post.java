package io.github.leoniedermeier.restclient.annotation.xyz;

public class Post {

    private String userId;
    private int id;
    private String title;
    private String body;
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Post [userId=");
        builder.append(userId);
        builder.append(", id=");
        builder.append(id);
        builder.append(", title=");
        builder.append(title);
        builder.append(", body=");
        builder.append(body);
        builder.append("]");
        return builder.toString();
    }
}
