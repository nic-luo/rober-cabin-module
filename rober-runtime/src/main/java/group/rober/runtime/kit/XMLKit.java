package group.rober.runtime.kit;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import group.rober.runtime.lang.PairBond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XML操作工具类
 * Created by tisir<yangsong158@qq.com> on 2017-05-20
 */
public abstract class XMLKit {
    static final Logger logger = LoggerFactory.getLogger(XMLKit.class);

    public static  <T>T readFormFile(File file,Map<String,Class<?>> alias,Class<T> classType,List<PairBond<Class<?>, String>> useAttributes) throws IOException{
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw e;
        }
        return readFormInputStream(inputStream,alias,classType,useAttributes);
    }

    public static  <T>T readFormFile(File file,Map<String,Class<?>> alias,Class<T> classType) throws IOException{
        return readFormFile(file,alias,classType,null);
    }

    public static <T> T readFormInputStream(InputStream inputStream, Map<String, Class<?>> alias, Class<T> classType, List<PairBond<Class<?>, String>> useAttributes) throws IOException {
//        Object object = xstream.fromXML(inputStream);
//        Object object = xstream.fromXML(inputStream,"object-stream");
//        return (T)object;
        XStream xstream = getXStream(alias,useAttributes);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = xstream.createObjectInputStream(inputStream);
            Object object = objectInputStream.readObject();
            return (T)object;
        } catch (IOException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch(StreamException e){
            return null;    //读取不到流,可能是文件是空的,就忽略
        }
    }

    public static  <T>T readFormInputStream(InputStream inputStream,Map<String,Class<?>> alias,Class<T> classType) throws IOException{
        return readFormInputStream(inputStream,alias,classType,null);
    }

    public static void writeToFile(Object object, File file, Map<String,Class<?>> alias,List<PairBond<Class<?>, String>> useAttributes) throws IOException {
        if(!file.exists())file.createNewFile();
        FileWriter writer = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            writer = new FileWriter(file);
            XStream xstream = getXStream(alias,useAttributes);
            objectOutputStream = xstream.createObjectOutputStream(writer);
            objectOutputStream.writeObject(object);
            IOKit.close(objectOutputStream);//不要放到final块中close,否则错误的数据也会被写到文件中
        } catch (IOException e) {
            throw e;
        }
    }

    public static void writeToFile(Object object, File file, Map<String,Class<?>> alias) throws IOException {
        writeToFile(object,file,alias,null);
    }
    public static void writeToOutputStream(Object object, OutputStream outputStream, Map<String,Class<?>> alias,List<PairBond<Class<?>, String>> useAttributes) throws IOException {
        ObjectOutputStream objectOutputStream = null;
        try {
            XStream xstream = getXStream(alias,useAttributes);
            objectOutputStream = xstream.createObjectOutputStream(outputStream);
            objectOutputStream.writeObject(object);
        } catch (IOException e) {
            throw e;
        } finally{
            IOKit.close(objectOutputStream);
        }
    }
    /**
     * 把对象写出到一个输出流中
     * @param object
     * @param outputStream
     * @param alias
     * @throws IOException
     */
    public static void writeToOutputStream(Object object, OutputStream outputStream, Map<String,Class<?>> alias) throws IOException {
        writeToOutputStream(object,outputStream,alias,null);
    }

    /**
     * 把对象转为XML字串
     * @param object
     * @return
     */
    public static String toXMLString(Object object){
        return toXMLString(object,null);
    }
    /**
     * 把对象转为XML字串
     * @param object
     * @param alias
     * @param useAttributes
     * @return
     */
    public static String toXMLString(Object object,Map<String,Class<?>> alias,List<PairBond<Class<?>,String>> useAttributes){
        XStream xstream = getXStream(alias,useAttributes);
        return xstream.toXML(object);
    }

    /**
     * 把对象转为XML字串
     * @param object
     * @param alias
     * @return
     */
    public static String toXMLString(Object object,Map<String,Class<?>> alias){
        XStream xstream = getXStream(alias,null);
        return xstream.toXML(object);
    }

//    static XStream getXStream(Map<String,Class<?>> alias){
//        return getXStream(alias,null);
//    }

    static XStream getXStream(Map<String,Class<?>> alias,List<PairBond<Class<?>,String>> useAttributes){
        XStream xstream = new XStream(new DomDriver());
        xstream.setMode(XStream.SINGLE_NODE_XPATH_RELATIVE_REFERENCES);
        alias(xstream,alias);
        if(useAttributes!=null){
            for(PairBond<Class<?>,String> pairItem  : useAttributes){
                xstream.useAttributeFor(pairItem.getLeft(),pairItem.getRight());
            }
        }
        return xstream;
    }

    /**
     * 别名
     * @param xstream
     * @param alias
     */
    static void alias(XStream xstream, Map<String,Class<?>> alias){
        if(alias==null||alias.size()==0)return;
        Iterator<Map.Entry<String, Class<?>>> iterator = alias.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Class<?>> entry = iterator.next();
            xstream.alias(entry.getKey(), entry.getValue());
        }
    }
}
