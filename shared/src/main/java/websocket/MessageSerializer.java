package websocket;

import websocket.commands.*;
import com.google.gson.*;

public class MessageSerializer {

    private static final Gson GSON = new Gson();

    public static UserGameCommand deserialize(String json) {
        System.out.println("Deserializing JSON: " + json);
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String typeStr = obj.get("commandType").getAsString();
            UserGameCommand.CommandType type = UserGameCommand.CommandType.valueOf(typeStr);

            return switch (type) {
                case CONNECT -> GSON.fromJson(json, ConnectCommand.class);
                case MAKE_MOVE -> GSON.fromJson(json, MakeMoveCommand.class);
                case LEAVE -> GSON.fromJson(json, LeaveCommand.class);
                case RESIGN -> GSON.fromJson(json, ResignCommand.class);
            };
        } catch (Exception e) {
            System.out.println("Deserialization failed: " + e.getMessage());
            return null;
        }
    }
}