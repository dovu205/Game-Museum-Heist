package vn.edu.policethief.client.model;

/** Lobby information received from the server. */
public record OnlinePlayer(String name, String points, String status) {}
