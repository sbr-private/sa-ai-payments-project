package com.payments.ledger.domain.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DemoUserRegistry {

  private final Map<String, DemoUser> usersByEmail;

  public DemoUserRegistry(ObjectMapper objectMapper) throws IOException {
    this.usersByEmail = loadUsers(objectMapper);
  }

  public Optional<DemoUser> findByEmail(String email) {
    return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
  }

  private static Map<String, DemoUser> loadUsers(ObjectMapper objectMapper) throws IOException {
    ClassPathResource resource = new ClassPathResource("demo-users.json");
    try (InputStream input = resource.getInputStream()) {
      JsonNode root = objectMapper.readTree(input);
      if (!root.isArray()) {
        throw new IllegalStateException("demo-users.json must be a JSON array");
      }

      Map<String, DemoUser> users = new HashMap<>();
      for (JsonNode node : root) {
        DemoUser user = parseUser(node);
        users.put(user.getEmail().toLowerCase(), user);
      }
      return Map.copyOf(users);
    }
  }

  private static DemoUser parseUser(JsonNode node) {
    String email = requiredText(node, "email");
    String password = requiredText(node, "password");
    String displayName = requiredText(node, "displayName");
    UserRole role = UserRole.valueOf(requiredText(node, "role"));

    List<UUID> accountIds =
        node.path("accountIds").isArray()
            ? java.util.stream.StreamSupport.stream(node.get("accountIds").spliterator(), false)
                .map(idNode -> UUID.fromString(idNode.asText()))
                .toList()
            : List.of();

    return new DemoUser(email, password, displayName, role, accountIds);
  }

  private static String requiredText(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.asText().isBlank()) {
      throw new IllegalStateException("demo-users.json entry missing field: " + field);
    }
    return value.asText();
  }
}
