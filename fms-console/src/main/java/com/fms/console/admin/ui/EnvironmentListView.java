package com.fms.console.admin.ui;

import jakarta.annotation.security.PermitAll;

import com.fms.console.admin.service.EnvironmentUiService;
import com.fms.console.audit.service.AuditUiService;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.AuditDtos.AuditEventDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentConfigDto;
import com.fms.console.client.dto.EnvironmentDtos.EnvironmentDto;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.MainLayout;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.UiFormat;
import com.fms.console.shared.ui.components.EmptyState;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.PageHeader;
import com.fms.console.shared.ui.components.PromoteDialog;
import com.fms.console.shared.ui.components.SectionCard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "admin/environments", layout = MainLayout.class)
@PermitAll
public class EnvironmentListView extends VerticalLayout implements BeforeEnterObserver {

  private final EnvironmentUiService environmentUiService;
  private final AuditUiService auditUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  public EnvironmentListView(
      EnvironmentUiService environmentUiService,
      AuditUiService auditUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.environmentUiService = environmentUiService;
    this.auditUiService = auditUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.isAdmin()) {
      event.rerouteTo(ForbiddenView.class);
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Environments"));
    render();
  }

  private void render() {
    removeAll();
    Button promote = new Button("Promote flags", VaadinIcon.UPLOAD.create(), e ->
        new PromoteDialog(environmentUiService, com.fms.console.shared.ui.GlobalContextBar.resolveAppId(),
            List.of(), null).open());
    promote.setEnabled(accessControl.canPublish());
    add(new PageHeader("Environments", promote));

    try {
      List<EnvironmentDto> envs = environmentUiService.list();
      Grid<EnvironmentDto> grid = new Grid<>(EnvironmentDto.class, false);
      grid.addClassName("fms-grid-compact");
      grid.addColumn(EnvironmentDto::name).setHeader("Environment");
      grid.addColumn(EnvironmentDto::displayName).setHeader("Display name");
      grid.addColumn(e -> e.production() ? "Yes" : "No").setHeader("Production");
      grid.setItems(envs);
      grid.setHeight("200px");
      add(new SectionCard("All environments", grid));

      if (envs.isEmpty()) {
        add(new EmptyState("No environments", "Environment configuration is not available."));
        return;
      }

      Tabs tabs = new Tabs();
      Div tabContent = new Div();
      tabContent.setWidthFull();
      for (EnvironmentDto env : envs) {
        String label = env.displayName() != null && !env.displayName().isBlank()
            ? env.displayName() : env.name();
        tabs.add(new Tab(label));
      }
      tabs.addSelectedChangeListener(e -> {
        int index = tabs.getSelectedIndex();
        if (index >= 0 && index < envs.size()) {
          tabContent.removeAll();
          tabContent.add(buildEnvSection(envs.get(index).name()));
        }
      });
      tabs.setSelectedIndex(0);
      tabContent.add(buildEnvSection(envs.get(0).name()));
      add(tabs, tabContent);
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private VerticalLayout buildEnvSection(String env) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    try {
      EnvironmentConfigDto config = environmentUiService.getConfig(env);
      section.add(new Span("Config version: " + config.currentConfigVersion()));
      section.add(new Span("Last updated: " + UiFormat.formatInstant(config.updatedAt())));

      var audit = auditUiService.query(null, null, null, "publish", env, null, null, 5, null);
      if (audit.data().isEmpty()) {
        section.add(new EmptyState("No recent publishes", "Publish events will appear in this timeline."));
      } else {
        Grid<AuditEventDto> timeline = new Grid<>(AuditEventDto.class, false);
        timeline.addClassName("fms-grid-compact");
        timeline.addColumn(e -> UiFormat.formatInstant(e.createdAt())).setHeader("Time");
        timeline.addColumn(AuditEventDto::actor).setHeader("Actor");
        timeline.addColumn(AuditEventDto::resourceId).setHeader("Resource");
        timeline.setItems(audit.data());
        timeline.setHeight("150px");
        section.add(timeline);
      }
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
    return section;
  }
}
