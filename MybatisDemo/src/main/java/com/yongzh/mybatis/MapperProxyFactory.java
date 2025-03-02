package com.yongzh.mybatis;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * @author yongzh
 * @version 1.0
 * @program: JavaTest
 * @description:
 * @date 2023/3/22 21:51
 */
public class MapperProxyFactory {
    private static Map<Class, TypeHandler> typeHandlerMap = new HashMap<>();

    static {
        typeHandlerMap.put(String.class, new StringTypeHandler());
        typeHandlerMap.put(Integer.class, new IntegerTypeHandler());

        try {
            //反射
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static<T> T getMapper(Class<T> mapper){

        Object proxyInstance = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{mapper}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
               //解析sql----->执行sql------>结果处理
                // 结果支持List或单个User对象
                Object result = null;

                // 参数名：参数值
                // paramValueMapping存储着{参数名：参数值}这样的键值对。相当于 变量名：变量值，替换到sql中
                Map<String, Object> paramValueMapping = new HashMap<>();
                Parameter[] parameters = method.getParameters();//获取方法传来的参数列表
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    //Java动态代理无法拿到parameter.getName()对应的值name,只能拿到arg0，所以需要@Param注解。下面是两种赋值方法
                    paramValueMapping.put(parameter.getName(), args[i]);//arg0:wuhu
                    paramValueMapping.put(parameter.getAnnotation(Param.class).value(), args[i]);//name:wuhu
                }

                //获取Select注解上的sql
                String sql = method.getAnnotation(Select.class).value();

                //sql解析器
                ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler();
                //把sql中的 #{}--->?，把#{name}中的name放到tokenHandler
                GenericTokenParser parser = new GenericTokenParser("#{", "}", tokenHandler);
                String parseSql = parser.parse(sql);

                //1.创建数据库连接
                Connection connection = getConnection();

                //2.构造PreparedStatement
                PreparedStatement statement = connection.prepareStatement(parseSql);

                //sql中变量的list,存着参数名称的list列表。重要的是list中参数的顺序，分别对应着 ？
                List<ParameterMapping> parameterMappingList = tokenHandler.getParameterMappings();
                for (int i = 0; i < parameterMappingList.size(); i++) {
                    // sql中的#{}变量名
                    String property = parameterMappingList.get(i).getProperty();
                    // 变量值
                    Object value = paramValueMapping.get(property);
                    // 根据值类型找TypeHandler，执行对应类型的set方法。
                    TypeHandler typeHandler = typeHandlerMap.get(value.getClass());
                    typeHandler.setParameter(statement, i + 1, value);
                }


                //3.执行PreparedStatement
                statement.execute();

                //4.根据当前执行方法的返回类型封装结果
                List<Object> list = new ArrayList<>();
                ResultSet resultSet = statement.getResultSet();

                //方法的返回值类型
                Class resultType = null;

                Type genericReturnType = method.getGenericReturnType();

                if (genericReturnType instanceof Class) {
                    // 不是泛型
                    resultType = (Class) genericReturnType;
                } else if (genericReturnType instanceof ParameterizedType) {
                    // 是泛型
                    Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                    resultType = (Class) actualTypeArguments[0];
                }

                // 根据字段名找到对应的set方法。 属性名：Method对象。例如：id: setId()
                Map<String, Method> setterMethodMapping = new HashMap<>();
                for (Method declaredMethod : resultType.getDeclaredMethods()) {//获取resultType中所有的方法，主要是为了拿set方法
                    if (declaredMethod.getName().startsWith("set")) {
                        String propertyName = declaredMethod.getName().substring(3);
                        propertyName = propertyName.substring(0, 1).toLowerCase(Locale.ROOT) + propertyName.substring(1);
                        setterMethodMapping.put(propertyName, declaredMethod);
                    }
                }

                // 记录sql返回的所有字段名，放到columnList里
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columnList = new ArrayList<>();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    columnList.add(metaData.getColumnName(i + 1));
                }

                while (resultSet.next()){
                    Object instance = resultType.newInstance();

                    for (String column : columnList) {
                        //根据字段名column找到该字段对应的set方法
                        Method setterMethod = setterMethodMapping.get(column);
                        // 根据setter方法参数类型找到TypeHandler
                        Class clazz = setterMethod.getParameterTypes()[0];
                        TypeHandler typeHandler = typeHandlerMap.get(clazz);
                        //
                        setterMethod.invoke(instance, typeHandler.getResult(resultSet, column));
                    }
                    list.add(instance);
                }


                //5.关闭数据库连接
                connection.close();

                if (method.getReturnType().equals(List.class)) {
                    result = list;
                } else {
                    result = list.get(0);
                }
                return result;
            }
        });
        return (T) proxyInstance;
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/mysql?useUnicode=true&characterEncoding=utf8&useSSL=TRUE&serverTimezone=UTC", "root", "root");
    }

}
