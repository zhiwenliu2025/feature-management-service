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
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.PromoteDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
    H2 title = new H2("Environments");
    title.addClassName("fms-page-title");
    Button promote = new Button("Promote flags", e ->
        new PromoteDialog(environmentUiService, com.fms.console.shared.ui.GlobalContextBar.resolveAppId(),
            List.of(), null).open());
    promote.setEnabled(accessControl.canPublish());
    add(title, promote);

    try {
      List<EnvironmentDto> envs = environmentUiService.list();
      Grid<EnvironmentDto> grid = new Grid<>(EnvironmentDto.class, false);
      grid.addColumn(EnvironmentDto::name).setHeader("Environment");
      grid.addColumn(EnvironmentDto::displayName).setHeader("Display name");
      grid.addColumn(e -> e.production() ? "Yes" : "No").setHeader("Production");
      grid.setItems(envs);
      grid.setHeight("200px");
      add(grid);

      for (EnvironmentDto env : envs) {
        add(buildEnvSection(env.name()));
      }
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private VerticalLayout buildEnvSection(String env) {
    VerticalLayout section = new VerticalLayout();
    section.add(new H3(env));
    try {
      EnvironmentConfigDto config = environmentUiService.getConfig(env);
      section.add(new com.vaadin.flow.component.html.Span(
          "Config version: " + config.currentConfigVersion()));
      section.add(new com.vaadin.flow.component.html.Span(
          "Last updated: " + config.updatedAt()));

      var audit = auditUiService.query(null, null, null, "publish", env, null, null, 5, null);
      Grid<AuditEventDto> timeline = new Grid<>(AuditEventDto.class, false);
      timeline.addColumn(AuditEventDto::createdAt).setHeader("Time");
      timeline.addColumn(AuditEventDto::actor).setHeader("Actor");
      timeline.addColumn(AuditEventDto::resourceId).setHeader("Resource");
      timeline.setItems(audit.data());
      timeline.setHeight("150px");
      section.add(timeline);
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
    return section;
  }
}
