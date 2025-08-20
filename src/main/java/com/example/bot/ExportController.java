package com.example.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
public class ExportController {
  private final ShiftStorage storage;

  // 環境変数 ADMIN_TOKEN で設定（未設定なら認証なし）
  @Value("${admin.token:}")
  private String adminToken;

  public ExportController(ShiftStorage storage) { this.storage = storage; }

  @GetMapping(value = "/export.csv", produces = "text/csv; charset=UTF-8")
  public ResponseEntity<String> exportCsv(
      @RequestHeader(value = "X-Admin-Token", required = false) String headerToken) {

    if (adminToken != null && !adminToken.isBlank()) {
      if (headerToken == null || !adminToken.equals(headerToken)) {
        return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN)
            .body("forbidden");
      }
    }

    var fmt = DateTimeFormatter.ISO_LOCAL_DATE;
    var sb = new StringBuilder();
    sb.append("userId,date\n");
    storage.snapshot().forEach((userId, dates) -> {
      for (var d : dates) {
        sb.append(userId).append(",").append(fmt.format(d)).append("\n");
      }
    });
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shifts.csv\"")
        .contentType(new MediaType("text","csv"))
        .body(sb.toString());
  }
}
