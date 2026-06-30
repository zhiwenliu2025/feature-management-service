package com.fms.console.shared.ui.components;

import tools.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class DiffPanel extends HorizontalLayout {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public DiffPanel(Object left, Object right) {
    setWidthFull();
    add(toPane("Current / Left", left), toPane("Selected / Right", right));
  }

  private Div toPane(String title, Object value) {
    Div pane = new Div();
    pane.addClassName("fms-diff-panel");
    pane.setWidth("50%");
    try {
      String json = value == null ? "(empty)" : MAPPER.writeValueAsString(value);
      pane.setText(title + "\n\n" + json);
    } catch (Exception e) {
      pane.setText(title + "\n\n" + String.valueOf(value));
    }
    return pane;
  }
}
