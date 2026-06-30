package com.fms.console.shared.ui.components;

import com.vaadin.flow.component.html.Span;

public class StatusBadge extends Span {

  public enum Variant {
    DRAFT("Draft", "fms-badge-neutral"),
    PUBLISHED("Published", "fms-badge-success"),
    ARCHIVED("Archived", "fms-badge-muted"),
    DRAFT_DIRTY("Unpublished changes", "fms-badge-warning"),
    KILL_SWITCH("Kill switch active", "fms-badge-error"),
    JOB_PENDING("Publishing…", "fms-badge-primary"),
    JOB_COMPLETED("Live", "fms-badge-success"),
    JOB_FAILED("Publish failed", "fms-badge-error");

    private final String label;
    private final String className;

    Variant(String label, String className) {
      this.label = label;
      this.className = className;
    }
  }

  public StatusBadge(Variant variant) {
    setText(variant.label);
    addClassName("fms-badge");
    addClassName(variant.className);
  }

  public static StatusBadge forFlagStatus(String status, boolean draftDirty) {
    if (draftDirty) {
      return new StatusBadge(Variant.DRAFT_DIRTY);
    }
    return switch (status == null ? "" : status.toLowerCase()) {
      case "published" -> new StatusBadge(Variant.PUBLISHED);
      case "archived" -> new StatusBadge(Variant.ARCHIVED);
      default -> new StatusBadge(Variant.DRAFT);
    };
  }
}
