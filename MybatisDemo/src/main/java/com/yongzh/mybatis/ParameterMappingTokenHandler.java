package com.yongzh.mybatis;

import java.util.ArrayList;
import java.util.List;

public class ParameterMappingTokenHandler implements TokenHandler{

    //把变量的名字按照顺序放到parameterMappings
    private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();

    @Override
    public String handleToken(String content) {
        parameterMappings.add(new ParameterMapping(content));
        return "?";
    }

    public List<ParameterMapping> getParameterMappings() {
        return parameterMappings;
    }
}
