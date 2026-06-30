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
import com.fms.console.shared.ui.components.EmptyState;
import com.fms.console.shared.ui.components.FmsBreadcrumb;
import com.fms.console.shared.ui.components.PageHeader;
import com.fms.console.shared.ui.components.SectionCard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
    add(new PageHeader("Dashboard", GlobalContextBar.resolveAppId() + " · " + GlobalContextBar.resolveEnvironment()));

    try {
      DashboardSnapshot snap = dashboardUiService.load(
          GlobalContextBar.resolveAppId(), GlobalContextBar.resolveEnvironment());
      add(new SectionCard(buildKpis(snap)));
      add(new SectionCard("Quick actions", buildQuickActions()));
      add(new SectionCard("Recent changes", buildRecentAudit(snap)));
      add(new SectionCard("Unpublished changes", buildDirtyFlags(snap)));
      add(new SectionCard("Environment", buildEnvInfo(snap)));
    } catch (FmsUiException ex) {
      ApiClientExceptionHandler.handle(ex);
    }
  }

  private Div buildKpis(DashboardSnapshot snap) {
    Div row = new Div();
    row.addClassName("fms-kpi-row");
    row.add(
        kpiCard("Total flags", snap.totalFlags(), false),
        kpiCard("Published", snap.publishedFlags(), false),
        kpiCard("Draft", snap.draftFlags(), false),
        kpiCard("Kill switches", snap.activeKillSwitches(), snap.activeKillSwitches() > 0));
    return row;
  }

  private Div kpiCard(String label, long value, boolean error) {
    Div card = new Div();
    card.addClassName("fms-kpi-card");
    if (error) {
      card.addClassName("fms-kpi-card-error");
    }
    card.getElement().getThemeList().add("surface");
    Span val = new Span(String.valueOf(value));
    val.addClassName("fms-kpi-value");
    Span lbl = new Span(label);
    lbl.addClassName("fms-kpi-label");
    card.add(val, lbl);
    return card;
  }

  private HorizontalLayout buildQuickActions() {
    HorizontalLayout actions = new HorizontalLayout();
    actions.setSpacing(true);
    if (accessControl.canWriteFlags()) {
      Button newFlag = new Button("New flag", VaadinIcon.PLUS.create(),
          e -> getUI().ifPresent(ui -> ui.navigate(FlagListView.class)));
      actions.add(newFlag);
    }
    if (accessControl.canExplain()) {
      Button explain = new Button("Explain query", VaadinIcon.SEARCH.create(),
          e -> getUI().ifPresent(ui -> ui.navigate(ExplainView.class)));
      actions.add(explain);
    }
    if (accessControl.canReadAudit()) {
      Button audit = new Button("View audit", VaadinIcon.LIST.create(),
          e -> getUI().ifPresent(ui -> ui.navigate(AuditLogView.class)));
      actions.add(audit);
    }
    return actions;
  }

  private VerticalLayout buildRecentAudit(DashboardSnapshot snap) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    if (snap.recentAudit().isEmpty()) {
      section.add(new EmptyState("No recent changes", "Audit events will appear here as changes are made."));
      return section;
    }
    Grid<AuditEventDto> grid = new Grid<>(AuditEventDto.class, false);
    grid.addClassName("fms-grid-compact");
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
    section.setPadding(false);
    section.setSpacing(true);
    if (snap.draftDirtyFlags().isEmpty()) {
      section.add(new EmptyState("All changes published", "No flags have unpublished draft changes."));
      return section;
    }
    Grid<FlagSummaryDto> grid = new Grid<>(FlagSummaryDto.class, false);
    grid.addClassName("fms-grid-compact");
    grid.addColumn(FlagSummaryDto::key).setHeader("Key")
        .setRenderer(new ComponentRenderer<>(f -> {
          var link = RouteLinks.flag(f.key(), f.key());
          link.addClassName("fms-monospace");
          return link;
        }));
    grid.addColumn(FlagSummaryDto::name).setHeader("Name");
    grid.setItems(snap.draftDirtyFlags());
    grid.setHeight("200px");
    section.add(grid);
    return section;
  }

  private VerticalLayout buildEnvInfo(DashboardSnapshot snap) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    if (snap.environmentConfig() != null) {
      section.add(new Span("Config version: " + snap.environmentConfig().currentConfigVersion()));
      section.add(new Span("Last updated: " + snap.environmentConfig().updatedAt()));
    } else {
      section.add(new EmptyState("No environment config", "Configuration details are not available."));
    }
    return section;
  }
}
