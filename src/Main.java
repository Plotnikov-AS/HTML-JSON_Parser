import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final String mscMetro = "D:\\SkillBox Java\\Модуль 9\\WikiParser\\data\\MscMetro.json";
    private static JSONObject mainObject = new JSONObject();
    private static JSONObject stationsObject = new JSONObject();
    private static JSONArray stationsInLineArray = new JSONArray();

    private static JSONObject connectionsObject = new JSONObject();
    private static JSONArray connectionsArray = new JSONArray();

    private static JSONArray linesArray = new JSONArray();
    private static JSONObject linesObject = new JSONObject();

    private static String prevLineNumber;

    public static void main(String[] args) {
        try {
            Document doc = Jsoup.connect("https://ru.wikipedia.org/wiki/Список_станций_Московского_метрополитена").maxBodySize(0).get();
            Elements tables = doc.select("table[class~=standard sortable]").select("tbody");

            tables.forEach(t -> {
                Elements tableRows = t.getElementsByTag("tr");
                tableRows.forEach(tr -> {
                    if (tr.child(0).select("span[class]").hasClass("sortkey")){
                        if (tr.child(0).select("td[data-sort-value]").attr("data-sort-value").equals("8.9")){
                            String lineNumber = tr.child(0).select("span.sortkey").first().text();
                            parseLines(tr, lineNumber);
                            parseStations(tr, lineNumber);
                            lineNumber = tr.child(0).child(3).text();
                            parseLines(tr, lineNumber);
                            parseStations(tr, lineNumber);
                        }
                        else {
                            String lineNumber = tr.child(0).select("span.sortkey").first().text();
                            parseLines(tr, lineNumber);
                            parseStations(tr, lineNumber);
                        }
                    }

                });
            });
            fillMainObject(linesArray, stationsObject);

            FileWriter writer = new FileWriter("data\\MscMetro.json");
            writer.write(mainObject.toJSONString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseLines (Element tr, String lineNumber){
        linesObject.put("number", lineNumber);
        linesObject.put("name", tr.child(0).select("span").attr("title").replaceAll("\\sлиния", ""));
        linesObject.put("color", tr.child(0).select("td[style]").attr("style").replaceAll("background:", ""));
        if (!linesArray.contains(linesObject) && !linesObject.containsValue("")){
            linesArray.add(new JSONObject(linesObject));
        }
    }

    private static void parseStations (Element tr, String lineNumber){
        try {
            if (stationsObject.isEmpty()){
                stationsObject.put(lineNumber, new JSONArray());
            }
            if (stationsObject.containsKey(lineNumber)) {
                stationsInLineArray.add(tr.child(1).select("a[title~=станция]").text());
                prevLineNumber = lineNumber;
            }
            else {
                //Скидываем полученные станции на линии в stationsObject
                JSONArray arrayToPutInObject = new JSONArray();
                for (Object object : stationsInLineArray){
                    arrayToPutInObject.add(object);
                }
                stationsObject.put(prevLineNumber, arrayToPutInObject);

                //Записываем первую стацию новой линии
                stationsInLineArray.clear();
                stationsInLineArray.add(tr.child(1).select("a[title~=станция]").text());
                stationsObject.put(lineNumber, new JSONArray());
            }
        } catch (IndexOutOfBoundsException e){
            System.out.println(lineNumber);
            e.printStackTrace();
        }
    }

    private static void parseConnections (Element tr){

    }

    private static void fillMainObject (JSONArray linesArray, JSONObject stationsObject){
        Collections.sort(linesArray, (Comparator<JSONObject>) (o1, o2) -> o1.get("number").toString().compareTo(o2.get("number").toString()));
        mainObject.put("lines", linesArray);

        TreeMap<String, Object> lineNum2statName = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1.matches(".*\\D+") || o2.matches(".*\\D+")) {
                    Float f1 = Float.parseFloat(o1.replaceAll("\\D+", "\\.5"));
                    Float f2 = Float.parseFloat(o2.replaceAll("\\D+", "\\.5"));
                    return f1.compareTo(f2);
                }
                else {
                    Integer i1 = Integer.parseInt(o1);
                    Integer i2 = Integer.parseInt(o2);
                    return i1.compareTo(i2);
                }
            }
        });
        stationsObject.keySet().forEach(e -> lineNum2statName.put(e.toString(), stationsObject.get(e)));
        mainObject.put("stations", lineNum2statName);
    }
}
