package com.example.bot;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ShiftStorage {
  // userId -> 登録日（重複なし、順序保持）
  private final Map<String, LinkedHashSet<LocalDate>> map = new ConcurrentHashMap<>();

  public void add(String userId, LocalDate date) {
    map.computeIfAbsent(userId, k -> new LinkedHashSet<>()).add(date);
  }

  public Map<String, List<LocalDate>> snapshot() {
    Map<String, List<LocalDate>> out = new LinkedHashMap<>();
    for (var e : map.entrySet()) {
      out.put(e.getKey(), new ArrayList<>(e.getValue()));
    }
    return out;
  }
}
