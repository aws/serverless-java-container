package ${groupId};


import java.util.HashMap;
import java.util.Map;

import static spark.Spark.before;
import static spark.Spark.get;

import ${groupId}.util.JsonTransformer;


public class SparkResources {

    public static void defineResources() {
        before((request, response) -> response.type("application/json"));

        get("/ping", (req, res) -> {
            Map<String, String> pong = new HashMap<>();
            pong.put("pong", "Hello, World!");
            res.status(200);
            return pong;
        }, new JsonTransformer());
    }
}