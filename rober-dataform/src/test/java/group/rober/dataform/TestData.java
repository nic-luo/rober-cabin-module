package group.rober.dataform;

import group.rober.dataform.entity.Person;
import group.rober.runtime.kit.DateKit;
import group.rober.runtime.lang.MapData;

import java.util.ArrayList;
import java.util.List;

public class TestData {
    public static List<MapData> dataList = new ArrayList<MapData>();
    public static List<MapData> keyList = new ArrayList<MapData>();
    public static Person[] persons = null;
    public static List<Person> personList = new ArrayList<Person>();

    static {
        persons = new Person[]{new Person(), new Person(), new Person()};

        persons[0].setId("701");
        persons[0].setCode("P701");
        persons[0].setName("测试1");
        persons[0].setChnName("测试1");
        persons[0].setEngName("test1");
//        persons[0].setRevision(1);
        persons[0].setHeight(182.3);
        persons[0].setViewTimes(3L);
        persons[0].setBirth(DateKit.parse("1999-8-8"));

        persons[1].setId("702");
        persons[1].setCode("P702");
        persons[1].setName("测试2");
        persons[1].setChnName("测试2");
        persons[1].setEngName("test2");
//        persons[1].setRevision(1);
        persons[1].setHeight(182.3);
        persons[1].setViewTimes(3L);
        persons[1].setBirth(DateKit.parse("1999-8-8"));

        persons[2].setId("703");
        persons[2].setCode("P703");
        persons[2].setName("测试3");
        persons[2].setChnName("测试3");
        persons[2].setEngName("test3");
//        persons[2].setRevision(1);
        persons[2].setHeight(182.3);
        persons[2].setViewTimes(3L);
        persons[2].setBirth(DateKit.parse("1999-8-8"));

        for (int i = 0; i < persons.length; i++) {
            Person p = persons[i];
            MapData dataItem = new MapData();
            dataItem.putFromBean(p);
            dataItem.remove("addresses");
            dataItem.remove("viewTimes");
            dataList.add(dataItem);

            MapData keys = new MapData();
            keys.put("id", p.getId());
            keyList.add(keys);
            personList.add(p);
        }

    }
}
