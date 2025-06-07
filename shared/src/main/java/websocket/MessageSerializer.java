package chess.websocket;

import chess.websocket.commands.*;
import com.google.gson.*;

public class MessageSerializer {

    private static final Gson gson = new Gson();

    public static UserGameCommand deserialize(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String typeStr = obj.get("commandType").getAsString();
        UserGameCommand.CommandType type = UserGameCommand.CommandType.valueOf(typeStr);

        return switch (type) {
            case CONNECT -> gson.fromJson(json, ConnectCommand.class);
            case MAKE_MOVE -> gson.fromJson(json, MakeMoveCommand.class);
            case LEAVE -> gson.fromJson(json, LeaveCommand.class);
            case RESIGN -> gson.fromJson(json, ResignCommand.class);
        };
    }
}