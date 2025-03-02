package com.yongzh.mybatisDemo;


import com.yongzh.mybatis.Param;
import com.yongzh.mybatis.Select;

/**
 * @author yongzh
 * @version 1.0
 * @description: TODO
 * @date 2023/3/22 21:44
 */
public interface UserMapper
{
    @Select("select * from user1 where name = #{name} and age =#{age} ")
    public User getUser(@Param("name") String name, @Param("age") Integer age);
    public User getUserById(Integer id);
}
