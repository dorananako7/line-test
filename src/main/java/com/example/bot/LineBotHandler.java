package com.example.bot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.MessageAction;
import com.linecorp.bot.messaging.model.QuickReply;
import com.linecorp.bot.messaging.model.QuickReplyItem;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;

@Component
@LineMessageHandler
public class LineBotHandler {

  private final MessagingApiClient messagingApiClient;
  private final ShiftStorage storage;

  private static final ZoneId JPT = ZoneId.of("Asia/Tokyo");
  private static final Pattern DATE_TEXT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  public LineBotHandler(MessagingApiClient messagingApiClient, ShiftStorage storage) {
    this.messagingApiClient = messagingApiClient;
    this.storage = storage;
  }

  @EventMapping
  public void handleTextMessageEvent(MessageEvent event) {
    if (!(event.message() instanceof TextMessageContent textMsg)) return;
    String text = textMsg.text().trim();

    // 1) トリガー
    if (text.equalsIgnoreCase("shift") || text.equals("シフト") || text.equals("日付")) {
      replyWithDateChoices(event.replyToken());
      return;
    }

    // 2) YYYY-MM-DD 形式なら登録
    if (DATE_TEXT.matcher(text).matches()) {
      try {
        var date = LocalDate.parse(text, ISO);
        var userId = (event.source() != null && event.source().userId() != null)
            ? event.source().userId()
            : "unknown";
        storage.add(userId, date);
        replyText(event.replyToken(), "登録しました: " + ISO.format(date) + "\nCSV: /export.csv");
        return;
      } catch (Exception ignore) {
        // フォーマット不正はエコーへ
      }
    }

    // 3) その他はエコー
    replyText(event.replyToken(), text);
  }

  @EventMapping
  public void handleDefault(Event event) {
    System.out.println("event: " + event);
  }

  /* ---- 送信ユーティリティ ---- */

  private void replyText(String replyToken, String text) {
    messagingApiClient.replyMessage(
        new ReplyMessageRequest.Builder(replyToken, List.of(new TextMessage(text))).build());
  }

  // 次の7日分を QuickReply で提示（YYYY-MM-DD を送ってもらう）
  private void replyWithDateChoices(String replyToken) {
    var today = LocalDate.now(JPT);
    var items = new ArrayList<QuickReplyItem>();
    for (int i = 0; i < 7; i++) {
      var d = today.plusDays(i);
      var label = d.getMonthValue() + "/" + d.getDayOfMonth(); // 例: 8/21
      var text = ISO.format(d);                                 // 例: 2025-08-21
      items.add(new QuickReplyItem(new MessageAction(label + " を登録", text)));
    }
    var qr = new QuickReply(items);

    var msg = new TextMessage.Builder("希望日を選んでください（7日分）")
        .quickReply(qr)
        .build();

    messagingApiClient.replyMessage(
        new ReplyMessageRequest.Builder(replyToken, List.of(msg)).build());
  }
}
