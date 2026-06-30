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
    add(new H3("Result Summary"));
    Div summary = new Div();
    summary.setText(String.format(
        "enabled: %s  value: %s  reason: %s  configVersion: %d  bucket: %s",
        response.enabled(),
        response.value(),
        response.reasonCode(),
        response.configVersion(),
        response.bucket() == null ? "—" : response.bucket()));
    add(summary);

    if (response.release() != null) {
      add(new Paragraph("release: " + response.release().releaseId()
          + " (" + response.release().version() + ")"));
    }

    add(new H3("Decision trace"));
    if (response.decisionTrace() == null || response.decisionTrace().isEmpty()) {
      add(new Paragraph("No trace steps returned."));
      return;
    }
    for (DecisionStepDto step : response.decisionTrace()) {
      Div row = new Div();
      row.addClassName("fms-decision-trace-step");
      if ("match".equalsIgnoreCase(step.result()) || "pass".equalsIgnoreCase(step.result())) {
        row.addClassName("match");
      } else if ("fail".equalsIgnoreCase(step.result()) || "no_match".equalsIgnoreCase(step.result())) {
        row.addClassName("fail");
      }
      String label = step.step();
      if (step.ruleName() != null) {
        label += " — " + step.ruleName();
      }
      row.add(new Span("✓ " + label));
      if (step.detail() != null) {
        row.add(new Paragraph(step.detail()));
      }
      add(row);
    }
  }
}
