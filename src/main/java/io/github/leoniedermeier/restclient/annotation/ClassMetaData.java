package io.github.leoniedermeier.restclient.annotation;

public class ClassMetaData {

    private Class<?> clazz;
    
    private String url;
    
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
    
    public Class<?> getClazz() {
        return clazz;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    public String getUrl() {
        return url;
    }
}
