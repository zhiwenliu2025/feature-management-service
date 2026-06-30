package com.fms.console.shared.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class UiFormat {

  private static final DateTimeFormatter DATETIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private UiFormat() {}

  public static String formatInstant(Instant instant) {
    if (instant == null) {
      return "—";
    }
    return DATETIME.format(instant);
  }
}
