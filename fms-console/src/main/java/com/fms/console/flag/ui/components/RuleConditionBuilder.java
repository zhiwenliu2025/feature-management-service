package com.fms.console.flag.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleConditionBuilder extends CustomField<Map<String, Object>> {

  private final VerticalLayout container = new VerticalLayout();
  private final List<ConditionRow> rows = new ArrayList<>();

  public RuleConditionBuilder() {
    setWidthFull();
    container.setPadding(false);
    container.setSpacing(true);
    Button add = new Button("Add condition", e -> addRow("", "equals", ""));
    addRow("region", "in", "US");
    add(add, container);
    add(container);
  }

  @Override
  protected Map<String, Object> generateModelValue() {
    Map<String, Object> conditions = new LinkedHashMap<>();
    for (ConditionRow row : rows) {
      String key = row.keyField.getValue();
      if (key == null || key.isBlank()) {
        continue;
      }
      conditions.put(key.trim(), parseValue(row));
    }
    return conditions;
  }

  @Override
  protected void setPresentationValue(Map<String, Object> value) {
    rows.clear();
    container.removeAll();
    if (value == null || value.isEmpty()) {
      addRow("", "equals", "");
      return;
    }
    value.forEach((k, v) -> addRow(k, inferOp(v), formatValue(v)));
  }

  private void addRow(String key, String op, String raw) {
    ConditionRow row = new ConditionRow(key, op, raw);
    rows.add(row);
    container.add(row.layout());
  }

  private Object parseValue(ConditionRow row) {
    String key = row.keyField.getValue();
    String op = row.opField.getValue();
    String raw = row.valueField.getValue();
    if ("rolloutPercent".equals(key)) {
      return Integer.parseInt(raw == null || raw.isBlank() ? "0" : raw);
    }
    if ("in".equals(op) || "not_in".equals(op)) {
      String[] parts = raw == null ? new String[0] : raw.split(",");
      List<String> values = new ArrayList<>();
      for (String part : parts) {
        if (!part.isBlank()) {
          values.add(part.trim());
        }
      }
      return values;
    }
    if (raw != null && (raw.equals("true") || raw.equals("false"))) {
      return Boolean.parseBoolean(raw);
    }
    try {
      return Integer.parseInt(raw);
    } catch (Exception e) {
      return raw;
    }
  }

  private String inferOp(Object value) {
    if (value instanceof List<?>) {
      return "in";
    }
    return "equals";
  }

  private String formatValue(Object value) {
    if (value instanceof List<?> list) {
      return String.join(",", list.stream().map(String::valueOf).toList());
    }
    return value == null ? "" : String.valueOf(value);
  }

  private class ConditionRow {
    final TextField keyField = new TextField("Attribute");
    final TextField opField = new TextField("Operator");
    final TextField valueField = new TextField("Value");

    ConditionRow(String key, String op, String val) {
      keyField.setValue(key);
      opField.setValue(op);
      valueField.setValue(val);
      keyField.setWidth("160px");
      opField.setWidth("120px");
      valueField.setWidthFull();
    }

    Div layout() {
      Div row = new Div(keyField, opField, valueField);
      row.getStyle().set("display", "flex").set("gap", "0.5rem").set("align-items", "end");
      return row;
    }
  }
}
