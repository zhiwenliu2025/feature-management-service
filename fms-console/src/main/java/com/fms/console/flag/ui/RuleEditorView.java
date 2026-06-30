package com.fms.console.flag.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.FlagDtos.FlagDetailDto;
import com.fms.console.client.dto.FlagDtos.RuleDto;
import com.fms.console.client.dto.RuleDtos.ReplaceRulesDto;
import com.fms.console.client.dto.RuleDtos.RuleInputDto;
import com.fms.console.flag.service.FlagUiService;
import com.fms.console.flag.service.RuleUiService;
import com.fms.console.flag.ui.components.RuleConditionBuilder;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.GlobalContextBar;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.EmptyState;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.FmsConfirmDialog;
import com.fms.console.shared.ui.components.FmsNotification;
import com.fms.console.shared.ui.components.UnsavedChangesGuard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.fms.console.shared.ui.RouteLinks;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Route(value = "flags/:flagKey/rules", layout = MainLayout.class)
@PermitAll
public class RuleEditorView extends VerticalLayout implements BeforeEnterObserver {

  private final FlagUiService flagUiService;
  private final RuleUiService ruleUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  private String flagKey;
  private String environment;
  private List<RuleDto> rules = new ArrayList<>();
  private final Grid<RuleDto> grid = new Grid<>(RuleDto.class, false);
  private final VerticalLayout gridContainer = new VerticalLayout();

  public RuleEditorView(
      FlagUiService flagUiService,
      RuleUiService ruleUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.flagUiService = flagUiService;
    this.ruleUiService = ruleUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;

    setPadding(true);
    setSpacing(true);
    setSizeFull();

    H2 title = new H2("Rule editor");
    title.addClassName("fms-page-title");
    configureGrid();
    gridContainer.setPadding(false);
    gridContainer.setSpacing(true);
    gridContainer.setSizeFull();
    gridContainer.addClassName("fms-grid-scroll");
    gridContainer.add(grid);
    add(title, buildEnvBar(), gridContainer, buildActions());
    setFlexGrow(1, gridContainer);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canWriteFlags()) {
      event.rerouteTo(ForbiddenView.class);
      return;
    }
    flagKey = event.getRouteParameters().get("flagKey").orElse("");
    environment = GlobalContextBar.resolveEnvironment();
    layoutUi.setBreadcrumb(new FmsBreadcrumb()
        .segment("Flags", FlagListView.class)
        .segment(flagKey, FlagDetailView.class, RouteLinks.flagParams(flagKey))
        .current("Rules"));
    loadRules();
  }

  private HorizontalLayout buildEnvBar() {
    ComboBox<String> env = new ComboBox<>("Environment");
    env.setItems("dev", "staging", "prod");
    env.setValue(environment);
    env.addValueChangeListener(e -> {
      if (e.getValue() != null) {
        UnsavedChangesGuard.confirmIfDirty(() -> {
          environment = e.getValue();
          loadRules();
        });
        if (!e.getValue().equals(environment)) {
          env.setValue(environment);
        }
      }
    });
    Button add = new Button("Add rule", e -> openRuleDialog(null));
  add.setEnabled(accessControl.canWriteFlags());
    return new HorizontalLayout(env, add);
  }

  private HorizontalLayout buildActions() {
    Button saveOrder = new Button("Save order", e -> saveAll());
    Button publish = new Button("Go to publish", e ->
        getUI().ifPresent(ui -> ui.navigate(FlagDetailView.class, RouteLinks.flagParams(flagKey))));
    publish.setEnabled(accessControl.canPublish());
    return new HorizontalLayout(saveOrder, publish);
  }

  private void configureGrid() {
    grid.addColumn(RuleDto::priority).setHeader("#").setWidth("80px");
    grid.addColumn(RuleDto::name).setHeader("Name").setFlexGrow(1);
    grid.addColumn(r -> summarize(r.conditions())).setHeader("Conditions").setFlexGrow(2);
    grid.addColumn(r -> String.valueOf(r.value())).setHeader("Value");
    grid.addColumn(RuleDto::isEnabled).setHeader("On");
    grid.addComponentColumn(rule -> {
      Button edit = new Button("Edit", e -> openRuleDialog(rule));
      Button up = new Button("↑", e -> move(rule, -1));
      Button down = new Button("↓", e -> move(rule, 1));
      return new HorizontalLayout(edit, up, down);
    }).setHeader("Actions");
    grid.setSizeFull();
  }

  private void loadRules() {
    try {
      FlagDetailDto flag = flagUiService.getFlag(GlobalContextBar.resolveAppId(), flagKey);
      rules = new ArrayList<>(flag.rules().getOrDefault(environment, List.of()));
      rules.sort(Comparator.comparingInt(RuleDto::priority));
      grid.setItems(rules);
      updateEmptyState();
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void updateEmptyState() {
    gridContainer.removeAll();
    if (rules.isEmpty()) {
      gridContainer.add(new EmptyState(
          "No rules",
          "Add targeting rules to control when this flag is enabled.",
          accessControl.canWriteFlags() ? "Add rule" : null,
          accessControl.canWriteFlags() ? () -> openRuleDialog(null) : null));
    } else {
      gridContainer.add(grid);
    }
  }

  private void move(RuleDto rule, int delta) {
    int idx = rules.indexOf(rule);
    int newIdx = idx + delta;
    if (newIdx < 0 || newIdx >= rules.size()) {
      return;
    }
    rules.remove(idx);
    rules.add(newIdx, rule);
    renumber();
    grid.getDataProvider().refreshAll();
    UnsavedChangesGuard.markDirty(true);
  }

  private void renumber() {
    List<RuleDto> renumbered = new ArrayList<>();
    int p = 10;
    for (RuleDto rule : rules) {
      renumbered.add(new RuleDto(rule.id(), p, rule.name(), rule.conditions(), rule.value(), rule.isEnabled()));
      p += 10;
    }
    rules = renumbered;
  }

  private void saveAll() {
    if (rules.size() > 50) {
      FmsNotification.error("Maximum 50 rules per environment.");
      return;
    }
    try {
      List<RuleInputDto> inputs = rules.stream()
          .map(r -> new RuleInputDto(r.priority(), r.name(), r.conditions(), r.value(), r.isEnabled()))
          .toList();
      ruleUiService.replaceRules(GlobalContextBar.resolveAppId(), flagKey,
          new ReplaceRulesDto(environment, inputs));
      UnsavedChangesGuard.markDirty(false);
      FmsNotification.success("Rules saved.");
      loadRules();
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private void openRuleDialog(RuleDto existing) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(existing == null ? "Add rule" : "Edit rule");
    TextField name = new TextField("Rule name");
    IntegerField priority = new IntegerField("Priority");
    RuleConditionBuilder conditions = new RuleConditionBuilder();
    TextField value = new TextField("Return value");
    Checkbox enabled = new Checkbox("Enabled");
    enabled.setValue(true);

    if (existing != null) {
      name.setValue(existing.name());
      priority.setValue(existing.priority());
      if (existing.conditions() instanceof Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> cond = (Map<String, Object>) map;
        conditions.setValue(cond);
      }
      value.setValue(String.valueOf(existing.value()));
      enabled.setValue(existing.isEnabled());
    } else {
      priority.setValue((rules.size() + 1) * 10);
    }

    Button save = new Button("Save", e -> {
      RuleDto updated = new RuleDto(
          existing == null ? null : existing.id(),
          priority.getValue() == null ? 0 : priority.getValue(),
          name.getValue(),
          conditions.getValue(),
          "true".equals(value.getValue()) ? Boolean.TRUE :
              "false".equals(value.getValue()) ? Boolean.FALSE : value.getValue(),
          enabled.getValue());
      if (existing == null) {
        rules.add(updated);
      } else {
        int idx = rules.indexOf(existing);
        rules.set(idx, updated);
      }
      rules.sort(Comparator.comparingInt(RuleDto::priority));
      grid.setItems(rules);
      updateEmptyState();
      UnsavedChangesGuard.markDirty(true);
      dialog.close();
    });
    dialog.add(name, priority, conditions, value, enabled, save);
    dialog.open();
  }

  @SuppressWarnings("unchecked")
  private String summarize(Object conditions) {
    if (!(conditions instanceof Map<?, ?> map)) {
      return String.valueOf(conditions);
    }
    StringBuilder sb = new StringBuilder();
    map.forEach((k, v) -> {
      if (!sb.isEmpty()) {
        sb.append(" · ");
      }
      sb.append(k).append(" ").append(v);
    });
    return sb.toString();
  }
}
