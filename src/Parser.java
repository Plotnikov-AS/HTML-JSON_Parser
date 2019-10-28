import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Parser {
    private static final String mscMetroFile = "data\\MscMetro.json";
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
    private static JSONObject linesObject = new JSONObject();

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

    private static JSONArray allConnectionsArray = new JSONArray();
    private static JSONObject connectedStationObject = new JSONObject();
    private static JSONArray connectedStationsArray = new JSONArray();

    public static void parse(){
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

                        //Переходы
                        if (tr.select("span[title~=Переход]").attr("title").matches("Переход на станцию.*")) {
                            String lineNumber = tr.child(0).select("span.sortkey").first().text();
                            parseConnections(tr, lineNumber);
                        }
                    }
                });
            });

            FileWriter writer = new FileWriter(mscMetroFile);
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
            linesObject.put("name", tr.select("span").attr("title").replaceAll("\\sлиния", ""));
            linesObject.put("color", tr.select("td[style]").attr("style").replaceAll("background:", ""));
            if (!linesArray.contains(linesObject) && !linesObject.get("name").toString().equals("")) {
                linesArray.add(new JSONObject(linesObject));
            }
        }
        mainObject.put("lines", linesArray);
    }

    private static void parseStations (Element tr, String lineNumber){
        List<String> stationsList = new ArrayList<>();
        if (lineNum2statName.containsKey(lineNumber)) {
            stationsList = lineNum2statName.get(lineNumber);
        }
        if (!tr.select("a[title~=\\(станция]").text().equals("")) {
            stationsList.add(tr.select("a[title~=\\(станция]").text());
        }
        lineNum2statName.put(lineNumber, stationsList);
        mainObject.put("stations", lineNum2statName);

    }

    private static void parseConnections (Element tr, String originLine) {
        try {
            String lineTo = tr.child(3).text();
            String stationTo;

            if (!tr.select("a[title~=\\(станция]").text().equals("")) {
                connectedStationObject.put("line", originLine);
                connectedStationObject.put("station", tr.select("a[title~=\\(станция]").text());

                if (!isConnectionAlreadyExist(connectedStationObject)) {
                    connectedStationsArray.add(new JSONObject(connectedStationObject));

                    //Переход на несколько линий
                    if (lineTo.contains(" ")) {
                        String[] linesTo = lineTo.split("\\s");

                        List<String> stationsTo = new ArrayList<>();
                        Elements stationTo_links = tr.select("span[title~=Переход]").select("a[href]");

                        //Получить все станции на пересадку
                        for (Element link : stationTo_links) {
                            String stationTo_link = link.absUrl("href");
                            stationsTo.add(Jsoup.connect(stationTo_link).get().getElementById("firstHeading").text().replaceAll("\\s\\(.*\\)", ""));
                        }

                        //Заполнить массив пересадочными станциями
                        for (int i = 0; i < linesTo.length; i++) {
                            connectedStationObject.put("line", linesTo[i]);
                            connectedStationObject.put("station", stationsTo.get(i));
                            connectedStationsArray.add(new JSONObject(connectedStationObject));
                        }
                    }

                    //Переход на одну линию
                    else {
                        String stationTo_link = tr.select("span[title~=Переход]").select("a[href]").first().absUrl("href");
                        stationTo = Jsoup.connect(stationTo_link).get().getElementById("firstHeading").text().replaceAll("\\s\\(.*\\)", "");
                        connectedStationObject.put("line", lineTo);
                        connectedStationObject.put("station", stationTo);
                        connectedStationsArray.add(new JSONObject(connectedStationObject));
                    }
                    allConnectionsArray.add(connectedStationsArray.clone());
                    mainObject.put("connections", allConnectionsArray);
                    connectedStationsArray.clear();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private static Boolean isConnectionAlreadyExist(JSONObject stationToAdd){
        for (Object connectedStations : allConnectionsArray){
            if (((JSONArray) connectedStations).contains(stationToAdd)){
                return true;
            }
        }
        return false;
    }
}

