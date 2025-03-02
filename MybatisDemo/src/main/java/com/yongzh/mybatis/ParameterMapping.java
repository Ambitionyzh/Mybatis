package com.yongzh.mybatis;

public class ParameterMapping {

    private String property;//取#{name}  中的name

    public ParameterMapping(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
