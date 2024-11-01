package com.satya.projectanalysis;

import com.google.gson.Gson;
import spark.Spark;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WebServer {
    public static void main(String[] args) throws Exception {

        Spark.port(8093);

        String path = "";
        Spark.get("/static/*", (req, res) -> {
            String responseBody = Result.attempt(() -> htmlResponseFromFile(req.pathInfo()))
                    .fold(ex -> {
                        ex.printStackTrace();
                        return "{}";
                    }, val -> val);
            res.type(computeContentType(req.pathInfo()));
            return responseBody;
        });

        Gson gson = new Gson();


        Spark.get("/api/classData", (req,res) -> {
            var annotations = Arrays.asList(req.queryParams("annotations").split(","));

//            Set<ClassData> response = new HashSet<>();
//            if(!annotations.isEmpty()) {
//                response.addAll(Global.INSTANCE.withAnnotationsLike(annotations));
//            }

            res.type("application/json");
            return Global.INSTANCE.all();
        }, gson::toJson);

        Spark.get("/api/graphData", (req,res) -> {
            String searchParam = req.params("search");

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

        Spark.get("/api/eventGraphData", (req,res) -> {
            String searchParam = req.params("search");

            Map<String, Object> response = new HashMap<>();
            response.put("events", EventFlowNetwork.getEvents());

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
        URL resource = WebServer.class.getResource(relativePath);
        if(resource == null) throw new RuntimeException(relativePath + " not found on classpath.");

        URI uri = resource.toURI();
        String path = uri.toString();
        path = path.replace("/out/production/", "/src/main/");
        path = path.replace("/build/resources/main/", "/src/main/resources/");

        System.out.println(path);

        return String.join("\n", Files.readAllLines(
                Paths.get(new URI(path)), Charset.defaultCharset()));
    }


}
