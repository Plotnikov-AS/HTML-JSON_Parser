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
    private static TreeSet<JSONObject> linesArray = new TreeSet<>(new Comparator<JSONObject>() {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            if (o1.get("number").toString().matches(".*\\D+") || o2.get("number").toString().matches(".*\\D+")) {
                Float f1 = Float.parseFloat(o1.get("number").toString().replaceAll("\\D+", "\\.5"));
                Float f2 = Float.parseFloat(o2.get("number").toString().replaceAll("\\D+", "\\.5"));
                return f1.compareTo(f2);
            }
            else {
                Integer i1 = Integer.parseInt(o1.get("number").toString());
                Integer i2 = Integer.parseInt(o2.get("number").toString());
                return i1.compareTo(i2);
            }
        }
    });

    private static JSONObject connectionsObject = new JSONObject();
    private static JSONArray connectionsArray = new JSONArray();

    private static JSONObject linesObject = new JSONObject();

    private static String prevLineNumber;
    private static TreeMap<String, List> lineNum2statName = new TreeMap<>(new Comparator<String>() {
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
    private static List<String> stInLine = new ArrayList<>();

    public static void main(String[] args) {
        try {
            Document doc = Jsoup.connect("https://ru.wikipedia.org/wiki/Список_станций_Московского_метрополитена").maxBodySize(0).get();
            Elements tables = doc.select("table[class~=standard sortable]").select("tbody");

            tables.forEach(t -> {
                Elements tableRows = t.getElementsByTag("tr");
                tableRows.forEach(tr -> {
                    if (tr.child(0).select("span[class]").hasClass("sortkey")){
                        //Сонцевская+Большая колевая линия
                        if (tr.child(0).select("td[data-sort-value]").attr("data-sort-value").equals("8.9")){
                            String lineNumber = tr.child(0).select("span.sortkey").first().text();
                            parseLines(tr, lineNumber);
                            parseStations(tr, lineNumber);
                            lineNumber = tr.child(0).child(3).text();
                            parseLines(tr, lineNumber);
                            parseStations(tr, lineNumber);
                        }
                        //все остальные линии
                        else {
                            String lineNumber = tr.child(0).select("span.sortkey").first().text();
                            parseLines(tr, lineNumber);
                            parseStations(tr, lineNumber);
                        }
                    }

                });
            });
            fillMainObject();
            FileWriter writer = new FileWriter("data\\MscMetro.json");
            writer.write(mainObject.toJSONString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseLines (Element tr, String lineNumber){
        if (!linesObject.containsValue(lineNumber)) {
            linesObject.put("number", lineNumber);
            linesObject.put("name", tr.child(0).select("span").attr("title").replaceAll("\\sлиния", ""));
            linesObject.put("color", tr.child(0).select("td[style]").attr("style").replaceAll("background:", ""));
            if (!linesArray.contains(linesObject) && !linesObject.get("name").toString().equals("")) {
                linesArray.add(new JSONObject(linesObject));
            }
        }
    }

    private static void parseStations (Element tr, String lineNumber){
        try {
            List<String> stationsList = new ArrayList<>();
            if (lineNum2statName.containsKey(lineNumber)) {
                stationsList = lineNum2statName.get(lineNumber);
            }
            stationsList.add(tr.child(1).select("a[title~=станция]").text());
            lineNum2statName.put(lineNumber, stationsList);

        } catch (IndexOutOfBoundsException e){
        }
    }

    private static void parseConnections (Element tr){

    }

    private static void fillMainObject (){
        mainObject.put("lines", linesArray);
        mainObject.put("stations", lineNum2statName);
    }
}
