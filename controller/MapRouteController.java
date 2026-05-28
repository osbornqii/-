package com.campusdelivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MapRouteController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ConcurrentHashMap<String, List<Map<String, Double>>> CACHE = new ConcurrentHashMap<>();

    @GetMapping("/api/map/route")
    public Map<String, Object> getRoute(
            @RequestParam String mode,
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destinationLat,
            @RequestParam double destinationLng
    ) {
        String key = mode + ":" + originLat + "," + originLng + "->" + destinationLat + "," + destinationLng;
        List<Map<String, Double>> cached = CACHE.get(key);
        if (cached != null) {
            return Map.of("points", cached);
        }

        String ak = System.getenv("BAIDU_MAP_AK");
        if (ak == null || ak.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAIDU_MAP_AK missing");
        }

        String api = switch (mode) {
            case "walking" -> "directionlite/v1/walking";
            case "riding" -> "directionlite/v1/riding";
            case "driving" -> "directionlite/v1/driving";
            default -> "directionlite/v1/walking";
        };

        String origin = urlEncode(originLat + "," + originLng);
        String dest = urlEncode(destinationLat + "," + destinationLng);
        String url = "https://api.map.baidu.com/" + api + "?origin=" + origin + "&destination=" + dest + "&ak=" + urlEncode(ak);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(6)).GET().build();
        try {
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "route service " + resp.statusCode());
            }
            JsonNode root = OBJECT_MAPPER.readTree(resp.body());
            JsonNode routes = root.path("result").path("routes");
            JsonNode steps = routes.isArray() && routes.size() > 0 ? routes.get(0).path("steps") : OBJECT_MAPPER.createArrayNode();
            List<Map<String, Double>> points = new ArrayList<>();
            if (steps.isArray()) {
                for (JsonNode s : steps) {
                    String path = s.path("path").asText("");
                    if (path.isEmpty()) continue;
                    String[] pairs = path.split(";");
                    for (String pair : pairs) {
                        String[] arr = pair.trim().split(",");
                        if (arr.length != 2) continue;
                        double lng = parseDouble(arr[0]);
                        double lat = parseDouble(arr[1]);
                        points.add(Map.of("lng", lng, "lat", lat));
                    }
                }
            }
            if (points.size() < 2) {
                points = List.of(
                        Map.of("lng", originLng, "lat", originLat),
                        Map.of("lng", destinationLng, "lat", destinationLat)
                );
            }
            CACHE.put(key, points);
            return Map.of("points", points);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            List<Map<String, Double>> points = List.of(
                    Map.of("lng", originLng, "lat", originLat),
                    Map.of("lng", destinationLng, "lat", destinationLat)
            );
            CACHE.put(key, points);
            return Map.of("points", points);
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}

