package com.fms.console.dashboard.ui;

import com.fms.console.audit.ui.AuditLogView;
import com.fms.console.client.ApiClientExceptionHandler;
import com.fms.console.client.FmsUiException;
import com.fms.console.client.dto.AuditDtos.AuditEventDto;
import com.fms.console.client.dto.FlagDtos.FlagSummaryDto;
import com.fms.console.dashboard.service.DashboardUiService;
import com.fms.console.dashboard.service.DashboardUiService.DashboardSnapshot;
import com.fms.console.explain.ui.ExplainView;
import com.fms.console.flag.ui.FlagListView;
import com.fms.console.shared.ui.AccessControlService;
import com.fms.console.shared.ui.ForbiddenView;
import com.fms.console.shared.ui.GlobalContextBar;
import com.fms.console.shared.ui.LayoutUiService;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.fms.console.shared.ui.RouteLinks;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = com.fms.console.shared.ui.MainLayout.class)
@PermitAll
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

  private final DashboardUiService dashboardUiService;
  private final AccessControlService accessControl;
  private final LayoutUiService layoutUi;

  public DashboardView(
      DashboardUiService dashboardUiService,
      AccessControlService accessControl,
      LayoutUiService layoutUi) {
    this.dashboardUiService = dashboardUiService;
    this.accessControl = accessControl;
    this.layoutUi = layoutUi;
    setPadding(true);
    setSpacing(true);
    setSizeFull();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (!accessControl.canReadFlags()) {
      event.rerouteTo(ForbiddenView.class);
      return;
    }
    layoutUi.setBreadcrumb(new FmsBreadcrumb().current("Dashboard"));
    render();
  }

  private void render() {
    removeAll();
    H2 title = new H2("Dashboard");
    title.addClassName("fms-page-title");
    add(title);

    try {
      DashboardSnapshot snap = dashboardUiService.load(
          GlobalContextBar.resolveAppId(), GlobalContextBar.resolveEnvironment());
      add(buildKpis(snap));
      add(buildQuickActions());
      add(buildRecentAudit(snap));
      add(buildDirtyFlags(snap));
      add(buildEnvInfo(snap));
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private HorizontalLayout buildKpis(DashboardSnapshot snap) {
    return new HorizontalLayout(
        kpiCard("Total flags", snap.totalFlags()),
        kpiCard("Published", snap.publishedFlags()),
        kpiCard("Draft", snap.draftFlags()),
        kpiCard("Kill switches", snap.activeKillSwitches()));
  }

  private Div kpiCard(String label, long value) {
    Div card = new Div();
    card.addClassName("fms-kpi-card");
    card.getElement().getThemeList().add("surface");
    Span val = new Span(String.valueOf(value));
    val.addClassName("fms-kpi-value");
    card.add(val, new Span(label));
    return card;
  }

  private HorizontalLayout buildQuickActions() {
    HorizontalLayout actions = new HorizontalLayout();
    if (accessControl.canWriteFlags()) {
      actions.add(new Button("New flag", e -> getUI().ifPresent(ui -> ui.navigate(FlagListView.class))));
    }
    if (accessControl.canExplain()) {
      actions.add(new Button("Explain query", e -> getUI().ifPresent(ui -> ui.navigate(ExplainView.class))));
    }
    if (accessControl.canReadAudit()) {
      actions.add(new Button("View audit", e -> getUI().ifPresent(ui -> ui.navigate(AuditLogView.class))));
    }
    return actions;
  }

  private VerticalLayout buildRecentAudit(DashboardSnapshot snap) {
    VerticalLayout section = new VerticalLayout();
    section.add(new H2("Recent changes"));
    Grid<AuditEventDto> grid = new Grid<>(AuditEventDto.class, false);
    grid.addColumn(AuditEventDto::createdAt).setHeader("Time");
    grid.addColumn(AuditEventDto::actor).setHeader("Actor");
    grid.addColumn(AuditEventDto::action).setHeader("Action");
    grid.addColumn(AuditEventDto::resourceId).setHeader("Resource");
    grid.setItems(snap.recentAudit());
    grid.setHeight("200px");
    section.add(grid);
    return section;
  }

  private VerticalLayout buildDirtyFlags(DashboardSnapshot snap) {
    VerticalLayout section = new VerticalLayout();
    section.add(new H2("Unpublished changes"));
    Grid<FlagSummaryDto> grid = new Grid<>(FlagSummaryDto.class, false);
    grid.addColumn(FlagSummaryDto::key).setHeader("Key")
        .setRenderer(new ComponentRenderer<>(f -> RouteLinks.flag(f.key(), f.key())));
    grid.addColumn(FlagSummaryDto::name).setHeader("Name");
    grid.setItems(snap.draftDirtyFlags());
    grid.setHeight("200px");
    section.add(grid);
    return section;
  }

  private VerticalLayout buildEnvInfo(DashboardSnapshot snap) {
    VerticalLayout section = new VerticalLayout();
    section.add(new H2("Environment"));
    if (snap.environmentConfig() != null) {
      section.add(new Span("Config version: " + snap.environmentConfig().currentConfigVersion()));
      section.add(new Span("Last updated: " + snap.environmentConfig().updatedAt()));
    }
    return section;
  }
}
