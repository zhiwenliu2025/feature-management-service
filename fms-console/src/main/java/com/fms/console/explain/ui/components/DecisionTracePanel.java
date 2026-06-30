package com.fms.console.explain.ui.components;

import com.fms.console.client.dto.ExplainDtos.DecisionStepDto;
import com.fms.console.client.dto.ExplainDtos.ExplainResponseDto;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class DecisionTracePanel extends VerticalLayout {

  public DecisionTracePanel(ExplainResponseDto response) {
    addClassName("fms-decision-trace");
    add(buildSummary(response));

    if (response.release() != null) {
      Span release = new Span("Release: " + response.release().releaseId()
          + " (v" + response.release().version() + ")");
      release.addClassName("fms-decision-trace-release");
      add(release);
    }

    add(new H3("Decision trace"));
    if (response.decisionTrace() == null || response.decisionTrace().isEmpty()) {
      add(new Paragraph("No trace steps returned."));
      return;
    }
    for (DecisionStepDto step : response.decisionTrace()) {
      add(buildStep(step));
    }
  }

  private Div buildSummary(ExplainResponseDto response) {
    Div summary = new Div();
    summary.addClassName("fms-decision-summary");

    summary.add(summaryItem("Enabled", String.valueOf(response.enabled()),
        response.enabled() ? "fms-summary-value-success" : "fms-summary-value-muted"));
    summary.add(summaryItem("Value", String.valueOf(response.value()), null));
    summary.add(summaryItem("Reason", humanReason(response.reasonCode()), null));
    summary.add(summaryItem("Config version", String.valueOf(response.configVersion()), null));
    summary.add(summaryItem("Bucket",
        response.bucket() == null ? "—" : String.valueOf(response.bucket()), null));
    return summary;
  }

  private Div summaryItem(String label, String value, String valueClass) {
    Div item = new Div();
    item.addClassName("fms-decision-summary-item");
    Span lbl = new Span(label);
    lbl.addClassName("fms-decision-summary-label");
    Span val = new Span(value);
    val.addClassName("fms-decision-summary-value");
    if (valueClass != null) {
      val.addClassName(valueClass);
    }
    item.add(lbl, val);
    return item;
  }

  private Div buildStep(DecisionStepDto step) {
    Div row = new Div();
    row.addClassName("fms-decision-trace-step");
    boolean pass = "match".equalsIgnoreCase(step.result()) || "pass".equalsIgnoreCase(step.result());
    boolean fail = "fail".equalsIgnoreCase(step.result()) || "no_match".equalsIgnoreCase(step.result());
    if (pass) {
      row.addClassName("match");
    } else if (fail) {
      row.addClassName("fail");
    }

    String label = step.step();
    if (step.ruleName() != null) {
      label += " — " + step.ruleName();
    }
    String icon = pass ? "✓" : (fail ? "✗" : "→");
    Span heading = new Span(icon + " " + label);
    heading.addClassName("fms-decision-trace-step-title");
    row.add(heading);
    if (step.detail() != null) {
      Paragraph detail = new Paragraph(step.detail());
      detail.addClassName("fms-decision-trace-step-detail");
      row.add(detail);
    }
    return row;
  }

  private static String humanReason(String code) {
    if (code == null || code.isBlank()) {
      return "—";
    }
    return switch (code) {
      case "NOT_PUBLISHED" -> "Not published";
      case "DEFAULT" -> "Default value";
      case "RULE_MATCH" -> "Rule match";
      case "KILL_SWITCH" -> "Kill switch";
      default -> code.replace('_', ' ').toLowerCase();
    };
  }
}
