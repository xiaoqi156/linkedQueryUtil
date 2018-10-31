package com.xiongyx.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author xiongyx
 * @Create 2018/10/31.
 *
 * 关联查询工具类
 */

public class LinkedQueryUtil {

    /**
     * 一对一连接
     *
     * @param beanList 需要被存放数据的beanList(主体)
     * @param beanKeyName   beanList中用来匹配数据key的属性
     * @param beanModelName  beanList中用来存放匹配到的数据value的属性
     * @param dataList  需要被关联的data列表
     * @param dataKeyName 需要被关联的data中的key,和参数beanKeyName代表的数值一致
     */
    public static void oneToOneLinked(List beanList, String beanKeyName, String beanModelName, List dataList, String dataKeyName) throws Exception {
        //:::如果不需要转换,直接返回
        if(!needTrans(beanList,dataList)){
            return;
        }

        //:::将被关联的数据列表,以需要连接的字段为key,转换成map,加快查询的速度
        Map<String,Object> dataMap = beanListToMap(dataList,dataKeyName);

        //:::进行数据匹配链接
        matchedDataToBeanList(beanList,beanKeyName,beanModelName,dataMap);
    }

    /**
     * 将javaBean组成的list去重 转为map, key为bean中指定的一个属性
     *
     * @param beanList list 本身
     * @param keyName 生成的map中的key
     * @return
     * @throws Exception
     */
    public static Map<String,Object> beanListToMap(List beanList,String keyName) throws Exception{
        //:::创建一个map
        Map<String,Object> map = new HashMap<>();

        //:::由keyName获得对应的get方法字符串
        String getMethodName = makeGetMethodName(keyName);

        try {
            //:::遍历beanList
            for(Object obj : beanList){
                //:::如果当前数据是hashMap类型
                if(obj.getClass() == HashMap.class){
                    Map currentMap = (Map)obj;

                    //:::使用keyName从map中获得对应的key
                    String result = (String)currentMap.get(keyName);

                    //:::放入map中(如果key一样,则会被覆盖去重)
                    map.put(result,currentMap);
                }else{
                    //:::否则默认是pojo对象
                    //:::获得get方法
                    Method getMethod = obj.getClass().getMethod(getMethodName);

                    //:::通过get方法从bean对象中得到数据key
                    String result = (String)getMethod.invoke(obj);

                    //:::放入map中(如果key一样,则会被覆盖去重)
                    map.put(result,obj);
                }
            }
        }catch(Exception e){
            throw new Exception(e);
        }

        //:::返回结果
        return map;
    }

    //=================================================================辅助函数===========================================================
    /***
     * 将通过keyName获得对应的bean对象的get方法名称的字符串
     * @param keyName 属性名
     * @return  返回get方法名称的字符串
     */
    private static String makeGetMethodName(String keyName){
        //:::将第一个字母转为大写
        String newKeyName = transFirstCharUpperCase(keyName);

        return "get" + newKeyName;
    }

    /***
     * 将通过keyName获得对应的bean对象的set方法名称的字符串
     * @param keyName 属性名
     * @return  返回set方法名称的字符串
     */
    private static String makeSetMethodName(String keyName){
        //:::将第一个字母转为大写
        String newKeyName = transFirstCharUpperCase(keyName);

        return "set" + newKeyName;
    }

    /**
     * 将字符串的第一个字母转为大写
     * @param str 需要被转变的字符串
     * @return 返回转变之后的字符串
     */
    private static String transFirstCharUpperCase(String str){
        return str.replaceFirst(str.substring(0, 1), str.substring(0, 1).toUpperCase());
    }

    /**
     * 判断当前的数据是否需要被转换
     *
     * 两个列表存在一个为空,则不需要转换
     * @return 不需要转换返回 false,需要返回 true
     * */
    private static boolean needTrans(List beanList,List dataList){
        if(listIsEmpty(beanList) || listIsEmpty(dataList)){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 列表是否为空
     * */
    private static boolean listIsEmpty(List list){
        if(list == null || list.isEmpty()){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 将批量查询出来的数据集合,组装到对应的beanList之中
     * @param beanList 需要被存放数据的beanList(主体)
     * @param beanKeyName   beanList中用来匹配数据的属性
     * @param beanModelName  beanList中用来存放匹配到的数据的属性
     * @param dataMap  data结果集以某一字段作为key对应的map
     * @throws Exception
     */
    private static void matchedDataToBeanList(List beanList, String beanKeyName, String beanModelName, Map<String,Object> dataMap) throws Exception {
        //:::获得beanList中存放对象的key的get方法名
        String beanGetMethodName = makeGetMethodName(beanKeyName);
        //:::获得beanList中存放对象的model的set方法名
        String beanSetMethodName = makeSetMethodName(beanModelName);

        try{
            //:::遍历整个beanList
            for(Object bean : beanList){
                //:::获得bean中key的method对象
                Method beanGetMethod = bean.getClass().getMethod(beanGetMethodName);

                //:::调用获得当前的key
                String currentBeanKey = (String)beanGetMethod.invoke(bean);

                //:::从被关联的数据集map中找到匹配的数据
                Object matchedData = dataMap.get(currentBeanKey);

                //:::如果找到了匹配的对象
                if(matchedData != null){
                    //:::获得bean中对应model的set方法
                    Class clazz = matchedData.getClass();

                    //:::如果匹配到的数据是hashMap
                    if(clazz == HashMap.class){
                        //:::转为父类map class用来调用set方法
                        clazz = Map.class;
                    }

                    //:::获得主体bean用于存放被关联对象的set方法
                    Method beanSetMethod = bean.getClass().getMethod(beanSetMethodName,clazz);

                    //:::执行set方法,将匹配到的数据放入主体数据对应的model属性中
                    beanSetMethod.invoke(bean,matchedData);
                }
            }
        }catch(Exception e){
            throw new Exception(e);
        }
    }
}