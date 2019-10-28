import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Reader {
    private static String jsonFile = "data\\MscMetro.json";
    public static void read(){
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonData = (JSONObject) jsonParser.parse(getJsonFile());
            JSONObject stationsObject = (JSONObject) jsonData.get("stations");
            readStations(stationsObject);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private static void readStations(JSONObject stationsObject){
        TreeSet<String> lines = new TreeSet<String>(new Comparator<String>() {
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
        lines.addAll(stationsObject.keySet());
        lines.forEach(e -> {
            System.out.println("Количество станций на линии №" + e + " " + ((JSONArray)stationsObject.get(e)).size());
        });
    }

    private static String getJsonFile(){
        StringBuilder builder = new StringBuilder();
        try {
            List<String> lines = Files.readAllLines(Paths.get(jsonFile));
            lines.forEach(line -> builder.append(line));
        }catch (Exception e){
            e.printStackTrace();
        }
        return builder.toString();
    }
}

