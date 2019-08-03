package com.satya.projectanalysis;

import com.google.gson.Gson;
import spark.Spark;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WebServer {
    public static void main(String[] args) {

        Spark.port(8080);

        String path = "";
        Spark.get("/static/*", (req, res) -> {
            String responseBody = Result.attempt(() -> htmlResponseFromFile(req.pathInfo()))
                    .fold(ex -> {
                        ex.printStackTrace();
                        return "{}";
                    }, val -> {
                        return val;
                    });
            res.type(computeContentType(req.pathInfo()));
            return responseBody;
        });

        Gson gson = new Gson();

        Spark.get("/api/graphData", (req,res) -> {
            String searchParam = req.params("search");

            if(Global.INSTANCE.getNodes().isEmpty()) Analyzer.main(null);

            Map<String, Object> response = new HashMap<>();
            response.put("nodes", filter(Global.INSTANCE.getNodes(), searchParam));

            //Edges will be list of key-value
            response.put("edges", Global.INSTANCE.getRelationships().entrySet().stream().map(e -> {
                HashMap<Object, Object> map = new HashMap<>();
                map.put("key", e.getKey());
                map.put("value", e.getValue());
                return map;
            }).collect(Collectors.toList()));

            res.type("application/json");
            return response;
        }, gson::toJson);

    }

    private static Map<String, Node> filter(Map<String, Node> nodes, String searchParam) {
        //Give nodes which are similar to searchParam
        // && nodes which are 1st, 2nd, or 3rd neighbor to these nodes.

        return nodes;
    }

    private static String computeContentType(String pathInfo) {
        String[] split = pathInfo.split("\\.");
        String extension = split[split.length - 1];

        switch (extension) {
            case "html":
                return "text/html";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "css":
                return "text/css";
            default:
                return "application/text";
        }
    }

    public static String htmlResponseFromFile(String relativePath) throws URISyntaxException, IOException {
        URI uri = WebServer.class.getResource(relativePath).toURI();
        String path = uri.toString();
        path = path.replace("/out/production/", "/src/main/");

        System.out.println(path);

        return String.join("\n", Files.readAllLines(
                Paths.get(new URI(path)), Charset.defaultCharset()));
    }


}
