package model.data;

public record AuthData(
        String authToken,
        String username
) {}